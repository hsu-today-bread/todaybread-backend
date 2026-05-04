package com.todaybread.server.domain.payment.processor;

/**
 * 결제 취소 결과를 나타내는 레코드입니다.
 *
 * @param paymentKey  토스 페이먼츠 결제 고유 키
 * @param orderId     주문 ID
 * @param status      취소 상태 (예: "CANCELED")
 * @param cancelledAt 취소 시각 ISO 8601
 */
public record CancelResult(
        String paymentKey,
        String orderId,
        String status,
        String cancelledAt
) {
}
