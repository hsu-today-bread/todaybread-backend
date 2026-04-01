package com.todaybread.server.domain.user.dto;

import com.todaybread.server.domain.user.entity.UserEntity;

/**
 * 로그인 후 성공 여부, 토큰, 유저 개인정보를 보냅니다.
 *
 * @param success      성공 여부
 * @param accessToken  access token
 * @param refreshToken refresh token
 * @param nickname     닉네임
 * @param name         이름
 * @param phoneNumber  전화번호
 */
public record UserLoginResponse(
        boolean success,
        String accessToken,
        String refreshToken,
        String nickname,
        String name,
        String phoneNumber
) {

    /**
     * 헬퍼 메서드입니다.
     *
     * @param accessToken  발급된 access token
     * @param refreshToken 발급된 refresh token
     * @param user         유저 엔티티
     * @return 응답 객체
     */
    public static UserLoginResponse ok(String accessToken, String refreshToken, UserEntity user) {
        return new UserLoginResponse(
                true,
                accessToken,
                refreshToken,
                user.getNickname(),
                user.getName(),
                user.getPhoneNumber()
        );
    }
}
