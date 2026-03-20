package com.todaybread.server.domain.user.dto;

/**
 * 정보 수정 후 응답 DTO
 * @param nickname 닉네임
 * @param name 이름
 * @param email 이메일
 */
public record UserUpdateResponse(
        String nickname,
        String name,
        String email
) {
    public static UserUpdateResponse ok(String nickname, String name, String email) {
        return new UserUpdateResponse(nickname, name, email);
    }
}
