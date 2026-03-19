package com.todaybread.server.domain.user.dto;

/**
 * 이메일 중복 체크 응답 DTO입니다.
 */
public record UserCheckEmailResponse(boolean success, String message, boolean exists) {
    public static UserCheckEmailResponse of(boolean exists) {
        String message = exists ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.";
        return new UserCheckEmailResponse(true, message, exists);
    }
}
