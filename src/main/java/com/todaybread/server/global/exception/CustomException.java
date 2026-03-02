package com.todaybread.server.global.exception;

import lombok.Getter;

/**
 * 특정 오류 및 예외 처리 용 사용자 정의 예외입니다.
 * 기존 예외 처리 및 오류 발생 시, 에러 코드를 삽입하고 파싱합니다.
 */
@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
