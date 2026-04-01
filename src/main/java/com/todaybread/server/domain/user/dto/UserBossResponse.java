package com.todaybread.server.domain.user.dto;

/**
 * 사업자 등록 후 응답 DTo
 *
 * @param success true
 * @param message 메세지
 * @param accessToken access token
 * @param refreshToken refresh token
 */
public record UserBossResponse (
        boolean success,
        String message,
        String accessToken,
        String refreshToken
) {
    public static UserBossResponse ok(String accessToken, String refreshToken) {
        return new UserBossResponse(true,"사업자 등록이 완료되었습니다.",
        accessToken,refreshToken);
    }
}
