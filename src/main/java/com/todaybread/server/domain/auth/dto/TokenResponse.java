package com.todaybread.server.domain.auth.dto;

/**
 * 토큰 응답 DTO입니다.
 */
public record TokenResponse(boolean success, String message, String accessToken, String refreshToken) {
    public static TokenResponse ok(String accessToken, String refreshToken) {
        return new TokenResponse(true, "토큰이 성공적으로 발급되었습니다.", accessToken, refreshToken);
    }
}
