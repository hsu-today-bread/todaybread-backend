package com.todaybread.server.domain.user.dto;

/**
 * 로그인 후 성공 여부, 2개의 토큰을 보냅니다.
 *
 * @param success   성공 여부
 * @param message   응답 메시지
 * @param accessToken   access token
 * @param refreshToken  refresh token
 */
public record UserLoginResponse(boolean success, String message, String accessToken, String refreshToken) {

    /**
     * 헬퍼 메서드입니다.
     * @param accessToken 발급된 access token
     * @param refreshToken 발급된 refresh token
     * @return 응답 객체
     */
    public static UserLoginResponse ok(String accessToken, String refreshToken) {
        return new UserLoginResponse(true, "로그인 성공", accessToken, refreshToken);
    }
}
