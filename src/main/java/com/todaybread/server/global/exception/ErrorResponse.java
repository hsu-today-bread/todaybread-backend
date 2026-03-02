package com.todaybread.server.global.exception;

/**
 * 기본적인 에러 상황 발생 시, 만들어질 객체입니다.
 *
 * @param code 에러 코드
 * @param message 에러 메시지
 */
public record ErrorResponse(String code, String message) {

    /**
     * 헬퍼 메서드, 이넘에 적용된 에러 코드를 errorResponse 형태로 쉽게 만들어줍니다.
     *
     * @param errorCode 이넘에 적용된 에러 코드
     * @return ErrorResponse 객체
     */
    public static ErrorResponse errorFrom(ErrorCode errorCode){
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }
}
