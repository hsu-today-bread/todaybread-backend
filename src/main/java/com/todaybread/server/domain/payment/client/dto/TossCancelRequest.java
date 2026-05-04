package com.todaybread.server.domain.payment.client.dto;

/**
 * 토스 페이먼츠 결제 취소 요청 DTO입니다.
 * POST /v1/payments/{paymentKey}/cancel 요청 본문에 사용됩니다.
 *
 * @param cancelReason 취소 사유
 * @param cancelAmount 취소 금액
 */
public record TossCancelRequest(
        String cancelReason,
        int cancelAmount
) {
}
