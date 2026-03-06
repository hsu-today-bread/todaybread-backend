package com.todaybread.server.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 에러 및 실패 시,규격을 정하기 위한 이넘 클래스입니다.
 * 
 * 성공 (2xx) → 정상 DTO 그대로 사용
 * 실패 (4xx, 5xx) → ErrorResponse에서 에러 코드 읽고 분기
 *
 * [컨벤션]
 *  - 핸들러는 얇게 유지하고, 의미는 ErrorCode에서 구체적으로 표현합니다.
 *  - HTTP 상태코드는 큰 분류로 사용합니다.
 *  - code 필드는 도메인_번호(COMMON_001, USER_001, ...)를 사용합니다.
 *  - 영역_API형태_의미 순서로 네이밍합니다.
 *      - 공통 오류 인 경우... COMMON_ ....
 *      - 유저 도메인인 경우... USER_ ...
 *
 * [HTTP 상태코드 가이드]
 *  - 400 BAD_REQUEST: 요청값/형식/검증 오류
 *  - 401 UNAUTHORIZED: 인증 실패 또는 인증 필요
 *  - 403 FORBIDDEN: 인증은 되었지만 권한 없음
 *  - 404 NOT_FOUND: 대상 리소스 없음
 *  - 405 METHOD_NOT_ALLOWED: 허용되지 않은 HTTP 메서드
 *  - 409 CONFLICT: 중복/상태 충돌
 *  - 500 INTERNAL_SERVER_ERROR: 서버 내부 예외
 */
@Getter
public enum ErrorCode {

    /**
     * ============================
     * 공통 오류
     * ============================
     */
    COMMON_REQUEST_VALIDATION_FAILED("COMMON_001", "요청값 검증에 실패했습니다.", HttpStatus.BAD_REQUEST),
    COMMON_HTTP_METHOD_NOT_ALLOWED("COMMON_002", "허용되지 않은 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    COMMON_INTERNAL_SERVER_ERROR("COMMON_003", "서버 내부 오류입니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    /**
     * ============================
     * 유저 오류
     * ============================
     */
    USER_REGISTER_EMAIL_ALREADY_EXISTS("USER_001", "이미 가입한 이메일입니다.", HttpStatus.CONFLICT),
    USER_REGISTER_PHONE_ALREADY_EXISTS("USER_002", "이미 가입한 전화번호입니다.", HttpStatus.CONFLICT),
    USER_REGISTER_NICKNAME_ALREADY_EXISTS("USER_003", "이미 사용중인 닉네임입니다.", HttpStatus.CONFLICT),
    USER_LOGIN_USER_NOT_FOUND("USER_004", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;

    private ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
