package com.todaybread.server.domain.payment.client.dto;

/**
 * 토스 페이먼츠 결제 승인 요청 DTO입니다.
 * POST /v1/payments/confirm 요청 본문에 사용됩니다.
 *
 * @param paymentKey 토스 페이먼츠 결제 고유 키
 * @param orderId    주문 ID
 * @param amount     결제 금액
 */
public record TossConfirmRequest(
        String paymentKey,
        String orderId,
        int amount
) {
}
