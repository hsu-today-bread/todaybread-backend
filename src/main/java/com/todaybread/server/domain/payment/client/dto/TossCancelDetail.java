package com.todaybread.server.domain.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 토스 페이먼츠 결제 취소 상세 정보 DTO입니다.
 * TossCancelResponse의 cancels 리스트 항목에 사용됩니다.
 *
 * @param cancelAmount 취소 금액
 * @param cancelReason 취소 사유
 * @param canceledAt   취소 시각 (ISO 8601 형식)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossCancelDetail(
        int cancelAmount,
        String cancelReason,
        String canceledAt
) {
}
