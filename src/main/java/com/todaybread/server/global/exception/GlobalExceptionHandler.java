package com.todaybread.server.global.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 런타임 에러, 특정 에러 상황 발생 시, 해당 에러의 처리를 담당합니다.
 */
@Slf4j
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
     * @Valid 검증 실패 시 처리합니다. (@RequestBody DTO 검증)
     * 예: 이메일 빈칸, 비밀번호 누락 등
     * @param ex 검증 예외
     * @return 400 BAD_REQUEST + COMMON_001
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return toResponse(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED);
    }

    /**
     * @Validated + @RequestParam 검증 실패 시 처리합니다.
     * 예: /exist/email?value= 빈칸으로 요청
     * @param ex 제약 조건 위반 예외
     * @return 400 BAD_REQUEST + COMMON_001
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return toResponse(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED);
    }

    /**
     * 허용되지 않은 HTTP 메서드 요청 시 처리합니다.
     * 예: POST 전용 API에 GET으로 요청
     * @param ex HTTP 메서드 불일치 예외
     * @return 405 METHOD_NOT_ALLOWED + COMMON_002
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return toResponse(ErrorCode.COMMON_HTTP_METHOD_NOT_ALLOWED);
    }

    /**
     * 위에서 처리되지 않은 모든 예외를 처리합니다. 자바 내부에서 생길 수 있는 모든 에러입니다.
     * 디버깅을 위해 로그를 남깁니다.
     * @param ex 예외 및 오류
     * @return 500 INTERNAL_SERVER_ERROR + COMMON_003
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("처리되지 않은 예외 발생", ex);
        return toResponse(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
    }
}
