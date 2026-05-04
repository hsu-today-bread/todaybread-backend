package com.todaybread.server.domain.payment.service;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.order.service.OrderService;
import com.todaybread.server.domain.payment.client.TossPaymentException;
import com.todaybread.server.domain.payment.dto.PaymentRequest;
import com.todaybread.server.domain.payment.dto.PaymentResponse;
import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import com.todaybread.server.domain.payment.processor.CancelResult;
import com.todaybread.server.domain.payment.processor.PaymentProcessor;
import com.todaybread.server.domain.payment.processor.PaymentResult;
import com.todaybread.server.domain.payment.repository.PaymentRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 결제 서비스 계층입니다.
 * 결제 요청 검증, PaymentProcessor 호출, Payment 저장, Order 확정을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProcessor paymentProcessor;
    private final OrderService orderService;
    private final Clock clock;

    /**
     * 결제를 처리합니다.
     * 비관적 락으로 주문을 조회하여 동시 결제를 방지합니다.
     * 기존 FAILED 결제가 있으면 상태를 갱신하여 재시도를 지원합니다.
     *
     * @param userId         유저 ID
     * @param request        결제 요청
     * @param idempotencyKey 멱등성 키
     * @return 결제 응답
     */
    @Transactional
    public PaymentResponse processPayment(Long userId, PaymentRequest request, String idempotencyKey) {
        // 1. 비관적 락으로 Order 조회 — 동시 결제 방지
        Optional<OrderEntity> orderOpt = orderRepository.findByIdWithLock(request.orderId());
        if (orderOpt.isEmpty()) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }
        OrderEntity order = orderOpt.get();

        // 2. 소유자 확인
        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 3. 동일 idempotency key 재시도면 기존 응답을 그대로 반환
        Optional<PaymentEntity> sameRequestPayment = paymentRepository
                .findByOrderIdAndIdempotencyKey(order.getId(), idempotencyKey);
        if (sameRequestPayment.isPresent()) {
            return PaymentResponse.of(sameRequestPayment.get());
        }

        // 4. 기존 결제 확인 — 이미 APPROVED면 거부, FAILED면 새 key로 재시도 허용
        Optional<PaymentEntity> existingPayment = paymentRepository.findByOrderId(order.getId());

        // 5. PENDING 상태 확인
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new CustomException(ErrorCode.PAYMENT_ORDER_STATUS_INVALID);
        }

        if (existingPayment.isPresent() && existingPayment.get().getStatus() == PaymentStatus.APPROVED) {
            throw new CustomException(ErrorCode.PAYMENT_ORDER_STATUS_INVALID);
        }

        // 6. 금액 일치 확인
        if (request.amount() != order.getTotalAmount()) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 7. PaymentProcessor 호출 (orderId를 외부 idempotency key로 전달)
        PaymentResult result = paymentProcessor.pay(order.getId(), request.amount());

        // 8. Payment 저장 또는 갱신
        PaymentEntity payment;
        if (existingPayment.isPresent()) {
            payment = existingPayment.get();
            if (result.status() == PaymentStatus.APPROVED) {
                payment.approve(LocalDateTime.now(clock), idempotencyKey);
            } else {
                payment.updateStatus(result.status(), idempotencyKey);
            }
        } else {
            payment = PaymentEntity.builder()
                    .orderId(order.getId())
                    .amount(request.amount())
                    .status(result.status())
                    .paidAt(result.status() == PaymentStatus.APPROVED ? LocalDateTime.now(clock) : null)
                    .idempotencyKey(idempotencyKey)
                    .build();
            paymentRepository.save(payment);
        }

        // 9. APPROVED → Order 확정
        if (result.status() == PaymentStatus.APPROVED) {
            orderService.confirmOrder(order.getId());
            log.info("결제 승인 완료: paymentId={}, orderId={}, amount={}", payment.getId(), order.getId(), request.amount());
        } else {
            log.warn("결제 실패: orderId={}, status={}, message={}", order.getId(), result.status(), result.message());
        }

        return PaymentResponse.of(payment);
    }

    /**
     * 토스 결제 승인을 확정합니다.
     * 프론트엔드에서 받은 paymentKey, orderId, amount를 검증하고 토스 Confirm API를 호출합니다.
     *
     * <p>멱등성: 동일 idempotencyKey로 이미 APPROVED 결제가 있으면 기존 결과를 반환합니다.
     * <p>토스 에러 처리:
     * <ul>
     *   <li>ALREADY_PROCESSED_PAYMENT → 기존 결제 결과 조회 후 반환</li>
     *   <li>PROVIDER_ERROR → PAYMENT_004 에러 반환</li>
     *   <li>카드 관련 에러 → 토스 에러 메시지 그대로 전달</li>
     * </ul>
     *
     * @param userId         유저 ID
     * @param paymentKey     토스 페이먼츠 결제 고유 키
     * @param orderId        주문 ID
     * @param amount         결제 금액
     * @param idempotencyKey 멱등성 키
     * @return 결제 엔티티
     */
    @Transactional
    public PaymentEntity confirmPayment(Long userId, String paymentKey, Long orderId, int amount,
                                        String idempotencyKey) {
        // 1. 멱등성 처리: 동일 idempotencyKey로 기존 APPROVED 결제가 있으면 기존 결과 반환
        Optional<PaymentEntity> existingByKey = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingByKey.isPresent() && existingByKey.get().getStatus() == PaymentStatus.APPROVED) {
            return existingByKey.get();
        }

        // 2. 비관적 락으로 주문 조회
        OrderEntity order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 3. 소유자 확인
        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 4. PENDING 상태 확인
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new CustomException(ErrorCode.PAYMENT_ORDER_STATUS_INVALID);
        }

        // 5. 금액 일치 확인
        if (amount != order.getTotalAmount()) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 6. 기존 결제 확인
        Optional<PaymentEntity> existingPayment = paymentRepository.findByOrderId(orderId);

        // 7. PaymentProcessor.confirm() 호출
        // 토스 orderId 규격: 6~64자 영문/숫자/-/_ 문자열
        String tossOrderId = "order_" + orderId;
        try {
            PaymentResult result = paymentProcessor.confirm(paymentKey, tossOrderId, amount);

            // 성공 시: Payment 저장 (APPROVED, paymentKey, method), 주문 CONFIRMED 전환
            PaymentEntity payment;
            if (existingPayment.isPresent()) {
                payment = existingPayment.get();
                payment.approve(LocalDateTime.now(clock), idempotencyKey, result.paymentKey(), result.method());
            } else {
                payment = PaymentEntity.builder()
                        .orderId(orderId)
                        .amount(amount)
                        .status(PaymentStatus.APPROVED)
                        .paidAt(LocalDateTime.now(clock))
                        .idempotencyKey(idempotencyKey)
                        .build();
                payment.approve(LocalDateTime.now(clock), idempotencyKey, result.paymentKey(), result.method());
                paymentRepository.save(payment);
            }

            orderService.confirmOrder(orderId);
            log.info("토스 결제 승인 완료: paymentId={}, orderId={}, amount={}, paymentKey={}",
                    payment.getId(), orderId, amount, result.paymentKey());

            return payment;

        } catch (TossPaymentException ex) {
            log.error("토스 결제 에러: code={}, message={}", ex.getErrorCode(), ex.getErrorMessage());

            // ALREADY_PROCESSED_PAYMENT: 기존 결제 결과 조회 후 반환
            if ("ALREADY_PROCESSED_PAYMENT".equals(ex.getErrorCode())) {
                Optional<PaymentEntity> alreadyProcessed = paymentRepository.findByOrderId(orderId);
                if (alreadyProcessed.isPresent()) {
                    return alreadyProcessed.get();
                }
            }

            // PROVIDER_ERROR: PAYMENT_004 에러 반환
            if ("PROVIDER_ERROR".equals(ex.getErrorCode())) {
                // 실패 시: Payment 저장 (FAILED), 주문 PENDING 유지
                saveFailedPayment(orderId, amount, idempotencyKey, existingPayment);
                throw new CustomException(ErrorCode.PAYMENT_PROVIDER_ERROR);
            }

            // 카드 관련 에러 및 기타: Payment 저장 (FAILED), 토스 에러 메시지 그대로 전달
            saveFailedPayment(orderId, amount, idempotencyKey, existingPayment);
            throw ex;
        }
    }

    /**
     * 결제를 취소합니다.
     * orderId로 APPROVED 상태의 결제를 조회하고, 토스 Cancel API를 호출하여 결제를 취소합니다.
     *
     * <p>취소 성공 시: Payment 상태를 CANCELLED로 변경하고, 취소 사유와 취소 시각을 저장합니다.
     * <p>취소 실패 시: PAYMENT_007 에러를 반환합니다.
     * <p>취소 성공 후 주문 취소 처리를 {@link OrderService#cancelOrder(Long, Long)}에 위임합니다.
     *
     * @param userId  유저 ID
     * @param orderId 주문 ID
     */
    @Transactional
    public void cancelPayment(Long userId, Long orderId) {
        // 1. orderId로 APPROVED 상태 결제 조회
        PaymentEntity payment = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.APPROVED)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 소유자 검증: 주문 조회하여 userId 확인
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 3. PaymentProcessor.cancel() 호출
        String cancelReason = "고객 요청";
        try {
            CancelResult cancelResult = paymentProcessor.cancel(
                    payment.getPaymentKey(), cancelReason, payment.getAmount());

            // 4. 성공 시: Payment 상태 CANCELLED, cancelReason, cancelledAt 저장
            LocalDateTime cancelledAt = LocalDateTime.parse(cancelResult.cancelledAt(),
                    java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            payment.cancel(cancelReason, cancelledAt);

            log.info("결제 취소 완료: orderId={}, paymentKey={}", orderId, payment.getPaymentKey());

        } catch (TossPaymentException ex) {
            // 5. 실패 시: PAYMENT_007 에러 반환
            log.error("결제 취소 실패: orderId={}, code={}, message={}",
                    orderId, ex.getErrorCode(), ex.getErrorMessage());
            throw new CustomException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }

        // 6. 주문 취소 처리 위임
        orderService.cancelOrder(userId, orderId);
    }

    /**
     * 결제 실패 시 Payment를 FAILED 상태로 저장합니다.
     */
    private void saveFailedPayment(Long orderId, int amount, String idempotencyKey,
                                   Optional<PaymentEntity> existingPayment) {
        if (existingPayment.isPresent()) {
            existingPayment.get().updateStatus(PaymentStatus.FAILED, idempotencyKey);
        } else {
            PaymentEntity failedPayment = PaymentEntity.builder()
                    .orderId(orderId)
                    .amount(amount)
                    .status(PaymentStatus.FAILED)
                    .idempotencyKey(idempotencyKey)
                    .build();
            paymentRepository.save(failedPayment);
        }
    }
}
