package com.todaybread.server.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 에러 상황 발생 시, 이넘에 맞춰서 규격을 정하기 위한 이넘 클래스입니다.
 *
 * [컨벤션]
 *  - 도메인_http상태코드로 이름을 짓습니다.
 *  - 서버 내부 오류의 경우는 server는 뺍니다.
 */
@Getter
public enum ErrorCode {

    /**
     * 서버 내부 오류
     */
    BAD_REQUEST("COMMON-400", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("COMMON-401", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("COMMON-403", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    NOT_FOUND("COMMON-404", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CONFLICT("COMMON-409", "요청이 충돌했습니다.", HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR("COMMON-500", "서버 내부 오류입니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    /**
     * 유저 관련 오류
     */
    USER_NOT_FOUND("USER-404", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_DUPLICATED("USER-409", "이미 존재하는 사용자입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
