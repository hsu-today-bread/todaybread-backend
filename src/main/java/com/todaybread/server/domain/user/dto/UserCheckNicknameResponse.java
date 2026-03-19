package com.todaybread.server.domain.user.dto;

/**
 * 닉네임 중복 체크 응답 DTO입니다.
 */
public record UserCheckNicknameResponse(boolean success, String message, boolean exists) {
    public static UserCheckNicknameResponse of(boolean exists) {
        String message = exists ? "이미 사용 중인 닉네임입니다." : "사용 가능한 닉네임입니다.";
        return new UserCheckNicknameResponse(true, message, exists);
    }
}
