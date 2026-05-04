package com.todaybread.server.domain.payment.dto;

import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

/**
 * 토스 결제 승인 확정 응답 DTO
 *
 * @param paymentId 결제 ID
 * @param orderId   주문 ID
 * @param amount    결제 금액
 * @param status    결제 상태
 * @param paidAt    결제 처리 시각
 * @param method    결제 수단 (카드, 간편결제 등)
 */
public record PaymentConfirmResponse(
        Long paymentId,
        Long orderId,
        int amount,
        PaymentStatus status,
        LocalDateTime paidAt,
        String method
) {
    /**
     * PaymentEntity로부터 응답을 생성합니다.
     *
     * @param payment 결제 엔티티
     * @return 결제 승인 확정 응답
     */
    public static PaymentConfirmResponse of(PaymentEntity payment) {
        return new PaymentConfirmResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getMethod()
        );
    }
}
