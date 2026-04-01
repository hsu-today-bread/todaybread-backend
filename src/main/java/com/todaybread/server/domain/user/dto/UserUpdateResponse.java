package com.todaybread.server.domain.user.dto;

/**
 * 정보 수정 후 응답 DTO
 *
 * @param nickname 닉네임
 * @param name 이름
 * @param phoneNumber 휴대전화번호
 */
public record UserUpdateResponse(
        String nickname,
        String name,
        String phoneNumber
) {
    public static UserUpdateResponse ok(String nickname, String name, String phoneNumber) {
        return new UserUpdateResponse(nickname, name, phoneNumber);
    }
}
