package com.todaybread.server.domain.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 토스 페이먼츠 결제 승인 응답 DTO입니다.
 * POST /v1/payments/confirm 성공 응답을 매핑합니다.
 * 토스 API는 다수의 필드를 반환하지만, 필요한 필드만 매핑합니다.
 *
 * @param paymentKey  토스 페이먼츠 결제 고유 키
 * @param orderId     주문 ID
 * @param status      결제 상태 (예: "DONE")
 * @param totalAmount 총 결제 금액
 * @param method      결제 수단 (예: "카드")
 * @param approvedAt  결제 승인 시각 (ISO 8601 형식)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        String status,
        int totalAmount,
        String method,
        String approvedAt
) {
}
