package com.todaybread.server.domain.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 토스 페이먼츠 결제 취소 응답 DTO입니다.
 * POST /v1/payments/{paymentKey}/cancel 성공 응답을 매핑합니다.
 * 토스 API는 다수의 필드를 반환하지만, 필요한 필드만 매핑합니다.
 *
 * @param paymentKey 토스 페이먼츠 결제 고유 키
 * @param orderId    주문 ID
 * @param status     결제 상태 (예: "CANCELED")
 * @param cancels    취소 상세 정보 목록
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossCancelResponse(
        String paymentKey,
        String orderId,
        String status,
        List<TossCancelDetail> cancels
) {
}
