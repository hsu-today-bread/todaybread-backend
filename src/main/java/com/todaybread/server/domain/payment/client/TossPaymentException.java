package com.todaybread.server.domain.payment.client;

import lombok.Getter;

/**
 * 토스 페이먼츠 API 에러 응답을 래핑하는 예외 클래스입니다.
 * HTTP 4xx/5xx 응답 시 토스 에러 코드와 메시지를 포함하여 발생합니다.
 */
@Getter
public class TossPaymentException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;
    private final int httpStatus;

    public TossPaymentException(String errorCode, String errorMessage, int httpStatus) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.httpStatus = httpStatus;
    }
}
