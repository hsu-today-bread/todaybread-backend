package com.todaybread.server.domain.payment.client.dto;

/**
 * 토스 페이먼츠 에러 응답 DTO입니다.
 * 토스 API가 HTTP 4xx/5xx 응답을 반환할 때의 본문을 매핑합니다.
 *
 * @param code    토스 에러 코드 (예: "ALREADY_PROCESSED_PAYMENT")
 * @param message 토스 에러 메시지 (예: "이미 처리된 결제입니다.")
 */
public record TossErrorResponse(
        String code,
        String message
) {
}
