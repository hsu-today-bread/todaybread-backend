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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 결제 서비스 계층입니다.
 * 결제 요청 검증, PaymentProcessor 호출, Payment 저장, Order 확정을 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProcessor paymentProcessor;
    private final OrderService orderService;

    /**
     * 결제를 처리합니다.
     *
     * 검증 순서:
     * 1. Order 존재 확인 (ORDER_001)
     * 2. 소유자 확인 (ORDER_002)
     * 3. PENDING 상태 확인 (PAYMENT_003)
     * 4. 금액 > 0 확인 (PAYMENT_002)
     * 5. 금액 일치 확인 (PAYMENT_001)
     *
     * @param userId  유저 ID
     * @param request 결제 요청
     * @return 결제 응답
     */
    @Transactional
    public PaymentResponse processPayment(Long userId, PaymentRequest request) {
        // 1. Order 존재 확인
        OrderEntity order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 소유자 확인
        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 3. PENDING 상태 확인
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new CustomException(ErrorCode.PAYMENT_ORDER_STATUS_INVALID);
        }

        // 4. 금액 > 0 확인
        if (request.amount() <= 0) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MUST_BE_POSITIVE);
        }

        // 5. 금액 일치 확인
        if (request.amount() != order.getTotalAmount()) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 6. PaymentProcessor 호출
        PaymentResult result = paymentProcessor.pay(request.amount());

        // 7. Payment 엔티티 생성 및 저장
        PaymentEntity payment = PaymentEntity.builder()
                .orderId(order.getId())
                .amount(request.amount())
                .status(result.status())
                .paidAt(result.status() == PaymentStatus.APPROVED ? LocalDateTime.now() : null)
                .build();
        paymentRepository.save(payment);

        // 8. APPROVED → Order 확정
        if (result.status() == PaymentStatus.APPROVED) {
            orderService.confirmOrder(order.getId());
        }

        // 9. 응답 반환
        return PaymentResponse.of(payment);
    }
}
