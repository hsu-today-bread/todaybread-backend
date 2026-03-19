package com.todaybread.server.domain.user.dto;

/**
 * 비밀번호 재설정 응답 DTO입니다.
 */
public record UserResetPasswordResponse(boolean success, String message) {
    public static UserResetPasswordResponse ok() {
        return new UserResetPasswordResponse(true, "비밀번호가 성공적으로 재설정되었습니다.");
    }
}
