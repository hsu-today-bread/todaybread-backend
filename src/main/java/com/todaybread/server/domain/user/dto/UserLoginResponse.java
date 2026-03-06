package com.todaybread.server.domain.user.dto;

/**
 * 로그인 응답 DTO
 *
 * @param id 유저 고유 ID
 * @param email 이메일
 * @param nickname 닉네임
 */
public record UserLoginResponse(
        Integer id,
        String email,
        String nickname) {
}