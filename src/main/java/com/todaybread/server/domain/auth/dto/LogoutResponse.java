package com.todaybread.server.domain.auth.dto;

/**
 * 로그아웃 응답 DTO입니다.
 */
public record LogoutResponse(boolean success, String message) {
    public static LogoutResponse ok() {
        return new LogoutResponse(true, "로그아웃되었습니다.");
    }
}
