package com.todaybread.server.domain.user.dto;

/**
 * 비밀번호 재설정 허가 시, 응답 DTO 입니다.
 * @param success true
 * @param message
 */
public record UserResetPasswordResponse (
        boolean success,
        String message
) {
    public static UserResetPasswordResponse ok(){
        return new UserResetPasswordResponse(true, "비밀 번호 변경 가능");
    }
}
