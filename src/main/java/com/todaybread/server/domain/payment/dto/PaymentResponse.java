package com.todaybread.server.domain.payment.dto;

import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

/**
 * 결제 응답 DTO
 *
 * @param paymentId 결제 ID
 * @param orderId 주문 ID
 * @param amount 결제 금액
 * @param status 결제 상태
 * @param paidAt 결제 처리 시각
 */
public record PaymentResponse(
        Long paymentId,
        Long orderId,
        int amount,
        PaymentStatus status,
        LocalDateTime paidAt
) {
    /**
     * PaymentEntity로부터 응답을 생성합니다.
     *
     * @param payment 결제 엔티티
     * @return 결제 응답
     */
    public static PaymentResponse of(PaymentEntity payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt()
        );
    }
}
