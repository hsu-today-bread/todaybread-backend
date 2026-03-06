package com.todaybread.server.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 런타임 에러, 특정 에러 상황 발생 시, 해당 에러의 처리를 담당합니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 내부에서 빠르게 변환을 위한 헬퍼 메서드입니다.
     * @param errorCode 에러코드
     * @return HTTP 응답
     */
    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.errorFrom(errorCode));
    }

    /**
     * CustomException을 처리합니다.
     * @param ex 커스텀 예외
     * @return ErrorResponse HTTP 형태로 반환
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        return toResponse(ex.getErrorCode());
    }

    /**
     * 전역적 에러를 처리합니다.
     * 자바 혹은 스프링 내부에서 이미 기록된 오류, 에러를 반환합니다.
     * @param ex 예외 및 오류
     * @return ErrorResponse HTTP 형태로 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        return toResponse(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
    }
}
