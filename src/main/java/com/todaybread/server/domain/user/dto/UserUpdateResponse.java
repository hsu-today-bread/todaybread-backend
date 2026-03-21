package com.todaybread.server.domain.user.dto;

/**
 * 정보 수정 후 응답 DTO
 * @param nickname 닉네임
 * @param name 이름
 * @param phone 휴대전화번호
 */
public record UserUpdateResponse(
        String nickname,
        String name,
        String phone
) {
    public static UserUpdateResponse ok(String nickname, String name, String phone) {
        return new UserUpdateResponse(nickname, name, phone);
    }
}
