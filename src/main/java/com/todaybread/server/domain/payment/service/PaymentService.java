package com.todaybread.server.domain.payment.service;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.order.service.OrderService;
import com.todaybread.server.domain.payment.client.TossPaymentException;
import com.todaybread.server.domain.payment.client.dto.TossPaymentResponse;
import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import com.todaybread.server.domain.payment.processor.CancelResult;
import com.todaybread.server.domain.payment.processor.PaymentProcessor;
import com.todaybread.server.domain.payment.processor.PaymentResult;
import com.todaybread.server.domain.payment.repository.PaymentRepository;
import com.todaybread.server.domain.payment.util.TossOrderIdHelper;
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
    private final PaymentCancelExecutor paymentCancelExecutor;
    private final Clock clock;

    /**
     * 토스 결제 승인을 확정합니다.
     * 프론트엔드에서 받은 paymentKey, orderId, amount를 검증하고 토스 Confirm API를 호출합니다.
     *
     * <p>멱등성: 동일 orderId + idempotencyKey로 이미 APPROVED 결제가 있으면 기존 결과를 반환합니다.
     * <p>토스 에러 처리:
     * <ul>
     *   <li>ALREADY_PROCESSED_PAYMENT → 토스 조회 API로 실제 상태 확인 후 동기화</li>
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
        // 1. 멱등성 처리: 동일 orderId + idempotencyKey로 기존 APPROVED 결제가 있으면 기존 결과 반환
        Optional<PaymentEntity> existingByKey = paymentRepository.findByOrderIdAndIdempotencyKey(orderId, idempotencyKey);
        if (existingByKey.isPresent() && existingByKey.get().getStatus() == PaymentStatus.APPROVED) {
            // 소유자 검증 — 멱등성 반환 전에 주문 소유자 확인
            OrderEntity existingOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
            if (!existingOrder.getUserId().equals(userId)) {
                throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
            }
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
        String tossOrderId = TossOrderIdHelper.toTossOrderId(orderId);
        try {
            PaymentResult result = paymentProcessor.confirm(paymentKey, tossOrderId, amount, idempotencyKey);

            // 성공 시: Payment 저장 (APPROVED, paymentKey, method), 주문 CONFIRMED 전환
            PaymentEntity payment;
            if (existingPayment.isPresent()) {
                payment = existingPayment.get();
                payment.approve(LocalDateTime.now(clock), idempotencyKey, result.paymentKey(), result.method());
                paymentRepository.save(payment);
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

            // ALREADY_PROCESSED_PAYMENT: 토스 조회 API로 실제 상태 확인 후 동기화
            if ("ALREADY_PROCESSED_PAYMENT".equals(ex.getErrorCode())) {
                return handleAlreadyProcessedPayment(paymentKey, orderId, amount, idempotencyKey, existingPayment);
            }

            // PROVIDER_ERROR: PAYMENT_004 에러 반환
            if ("PROVIDER_ERROR".equals(ex.getErrorCode())) {
                saveFailedPayment(orderId, amount, idempotencyKey, existingPayment);
                throw new CustomException(ErrorCode.PAYMENT_PROVIDER_ERROR);
            }

            // 카드 관련 에러 및 기타: Payment 저장 (FAILED), 토스 에러 메시지 그대로 전달
            saveFailedPayment(orderId, amount, idempotencyKey, existingPayment);
            throw ex;
        }
    }

    /**
     * 결제를 취소합니다. (2단계 취소 패턴)
     * 1단계: 짧은 트랜잭션으로 주문 상태를 CANCEL_PENDING으로 변경
     * 2단계: 트랜잭션 밖에서 토스 Cancel API 호출
     * 3단계: 짧은 트랜잭션으로 최종 상태 반영 (CANCELLED + 재고 복원 또는 CONFIRMED 롤백)
     *
     * <p>트랜잭션 단위 작업은 {@link PaymentCancelExecutor}에 위임하여
     * Spring AOP 프록시를 통한 @Transactional 적용을 보장합니다.
     *
     * @param userId  유저 ID
     * @param orderId 주문 ID
     */
    public void cancelPayment(Long userId, Long orderId) {
        // 1단계: 취소 준비 (짧은 트랜잭션 — 프록시를 통해 호출)
        PaymentCancelExecutor.CancelPreparation prep = paymentCancelExecutor.prepareCancelPayment(userId, orderId);

        // 2단계: 토스 Cancel API 호출 (트랜잭션 밖)
        CancelResult cancelResult;
        try {
            cancelResult = paymentProcessor.cancel(
                    prep.paymentKey(), "고객 요청", prep.amount());
        } catch (TossPaymentException ex) {
            log.error("결제 취소 실패: orderId={}, code={}, message={}",
                    orderId, ex.getErrorCode(), ex.getErrorMessage());
            // 토스 취소 실패 → CONFIRMED로 복원
            paymentCancelExecutor.rollbackCancelPayment(orderId);
            throw new CustomException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }

        // 3단계: 취소 완료 (짧은 트랜잭션 — 프록시를 통해 호출)
        paymentCancelExecutor.completeCancelPayment(orderId, prep.paymentId(), cancelResult);
    }

    /**
     * ALREADY_PROCESSED_PAYMENT 에러 시 토스 조회 API로 실제 상태를 확인하고 동기화합니다.
     */
    private PaymentEntity handleAlreadyProcessedPayment(String paymentKey, Long orderId, int amount,
                                                         String idempotencyKey,
                                                         Optional<PaymentEntity> existingPayment) {
        // 기존 결제가 APPROVED면 그대로 반환
        if (existingPayment.isPresent() && existingPayment.get().getStatus() == PaymentStatus.APPROVED) {
            return existingPayment.get();
        }

        // 기존 결제가 FAILED이거나 없으면 → 토스 조회 API로 실제 상태 확인
        try {
            TossPaymentResponse tossPayment = paymentProcessor.getPayment(paymentKey);

            if ("DONE".equals(tossPayment.status())) {
                // 토스 조회 결과의 orderId/totalAmount/paymentKey를 현재 요청과 비교
                String expectedTossOrderId = TossOrderIdHelper.toTossOrderId(orderId);
                if (!expectedTossOrderId.equals(tossPayment.orderId())
                        || tossPayment.totalAmount() != amount
                        || !paymentKey.equals(tossPayment.paymentKey())) {
                    log.error("ALREADY_PROCESSED 재조정 불일치: expected orderId={}, amount={}, paymentKey={} / actual orderId={}, amount={}, paymentKey={}",
                            expectedTossOrderId, amount, paymentKey,
                            tossPayment.orderId(), tossPayment.totalAmount(), tossPayment.paymentKey());
                    saveFailedPayment(orderId, amount, idempotencyKey, existingPayment);
                    throw new CustomException(ErrorCode.PAYMENT_PROVIDER_ERROR);
                }

                // 토스 상태가 DONE → 기존 결제를 APPROVED로 갱신 + 주문 확정
                PaymentEntity payment;
                if (existingPayment.isPresent()) {
                    payment = existingPayment.get();
                    payment.approve(LocalDateTime.now(clock), idempotencyKey,
                            tossPayment.paymentKey(), tossPayment.method());
                    paymentRepository.save(payment);
                } else {
                    payment = PaymentEntity.builder()
                            .orderId(orderId)
                            .amount(amount)
                            .status(PaymentStatus.APPROVED)
                            .paidAt(LocalDateTime.now(clock))
                            .idempotencyKey(idempotencyKey)
                            .build();
                    payment.approve(LocalDateTime.now(clock), idempotencyKey,
                            tossPayment.paymentKey(), tossPayment.method());
                    paymentRepository.save(payment);
                }

                orderService.confirmOrder(orderId);
                log.info("ALREADY_PROCESSED 재조정 완료 (DONE): orderId={}, paymentKey={}",
                        orderId, tossPayment.paymentKey());
                return payment;
            }
        } catch (Exception e) {
            log.error("토스 결제 조회 실패: paymentKey={}, orderId={}", paymentKey, orderId, e);
        }

        // 토스 상태가 DONE이 아니거나 조회 실패 → FAILED 저장 후 예외
        saveFailedPayment(orderId, amount, idempotencyKey, existingPayment);
        throw new CustomException(ErrorCode.PAYMENT_PROVIDER_ERROR);
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
