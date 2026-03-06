package com.todaybread.server.global.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 런타임 에러, 특정 에러 상황 발생 시, 해당 에러의 처리를 담당합니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 내용: CustomException을 처리합니다.
     * @param ex 커스텀 예외
     * @return ErrorResponse HTTP 형태로 반환
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.errorFrom(errorCode));
    }

    /**
     * 내용: DTO 유효성 검증 실패(@Valid) 시 발생하는 예외를 처리합니다.
     * @param ex MethodArgumentNotValidException 예외
     * @return ErrorResponse HTTP 형태로 반환 (400 BAD_REQUEST)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        ErrorResponse response = new ErrorResponse(ErrorCode.BAD_REQUEST.getCode(), errorMessage);
        
        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getStatus())
                .body(response);
    }

    /**
     * 내용: DB Unique 제약 조건 위반 (회원가입 동시성 문제 등) 시 발생하는 예외를 처리합니다.
     * @param ex DataIntegrityViolationException 예외
     * @return ErrorResponse HTTP 형태로 반환 (409 CONFLICT)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(ErrorCode.USER_DUPLICATED.getStatus())
                .body(ErrorResponse.errorFrom(ErrorCode.USER_DUPLICATED));
    }

    /**
     * 내용: 전역적 에러를 처리합니다.
     * @param ex 예외 및 오류
     * @return ErrorResponse HTTP 형태로 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.errorFrom(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}