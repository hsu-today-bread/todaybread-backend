package com.todaybread.server.domain.user.dto;

/**
 * 회원가입 응답 DTO입니다.
 */
public record UserRegisterResponse(boolean success, String message) {
    public static UserRegisterResponse ok() {
        return new UserRegisterResponse(true, "회원가입이 완료되었습니다.");
    }
}
