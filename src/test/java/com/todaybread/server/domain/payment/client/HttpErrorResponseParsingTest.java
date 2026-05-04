package com.todaybread.server.domain.payment.client;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

// Feature: toss-payment-integration, Property 5: HTTP 에러 응답 파싱 보존

/**
 * Property 5: HTTP 에러 응답 파싱 보존
 *
 * 임의의 HTTP 4xx/5xx 상태 코드와 토스 에러 응답(code, message)을 생성하여
 * {@link TossPaymentException}이 해당 값을 정확히 포함하는지 검증합니다.
 *
 * <b>Validates: Requirements 4.5</b>
 */
class HttpErrorResponseParsingTest {

    /**
     * **Validates: Requirements 4.5**
     *
     * 임의의 에러 코드, 에러 메시지, HTTP 4xx 상태 코드에 대해:
     * TossPaymentException이 errorCode, errorMessage, httpStatus를 정확히 보존하는지 검증
     */
    @Property(tries = 100)
    void tossPaymentExceptionPreservesFieldsFor4xx(
            @ForAll @StringLength(min = 1, max = 100) String errorCode,
            @ForAll @StringLength(min = 1, max = 200) String errorMessage,
            @ForAll @IntRange(min = 400, max = 499) int httpStatus) {

        // when: TossPaymentException 생성
        TossPaymentException exception = new TossPaymentException(errorCode, errorMessage, httpStatus);

        // then: 모든 필드가 정확히 보존되는지 검증
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(exception.getHttpStatus()).isEqualTo(httpStatus);
        assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }

    /**
     * **Validates: Requirements 4.5**
     *
     * 임의의 에러 코드, 에러 메시지, HTTP 5xx 상태 코드에 대해:
     * TossPaymentException이 errorCode, errorMessage, httpStatus를 정확히 보존하는지 검증
     */
    @Property(tries = 100)
    void tossPaymentExceptionPreservesFieldsFor5xx(
            @ForAll @StringLength(min = 1, max = 100) String errorCode,
            @ForAll @StringLength(min = 1, max = 200) String errorMessage,
            @ForAll @IntRange(min = 500, max = 599) int httpStatus) {

        // when: TossPaymentException 생성
        TossPaymentException exception = new TossPaymentException(errorCode, errorMessage, httpStatus);

        // then: 모든 필드가 정확히 보존되는지 검증
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(exception.getHttpStatus()).isEqualTo(httpStatus);
        assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
}
