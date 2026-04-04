package com.todaybread.server.domain.payment.service;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.order.service.OrderService;
import com.todaybread.server.domain.payment.dto.PaymentRequest;
import com.todaybread.server.domain.payment.dto.PaymentResponse;
import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
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
     * @param userId  유저 ID
     * @param request 결제 요청
     * @return 결제 응답
     */
    @Transactional
    public PaymentResponse processPayment(Long userId, PaymentRequest request, String idempotencyKey) {
        // 1. 비관적 락으로 Order 조회 — 동시 결제 방지
        OrderEntity order = orderRepository.findByIdWithLock(request.orderId())
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

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
}
