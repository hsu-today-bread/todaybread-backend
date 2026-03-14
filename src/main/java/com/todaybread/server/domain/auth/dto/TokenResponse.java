package com.todaybread.server.domain.auth.dto;

/**
 * 토큰의 정보를 담은 DTO입니다.
 * 회원 가입 및 토큰 재발급 시, 사용됩니다.
 *
 * @param accessToken 엑세스 토큰
 * @param refreshToken 리프래쉬 토큰
 */
public record TokenResponse(
    String accessToken,
    String refreshToken
) {
    public static TokenResponse ok(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken);
    }
}