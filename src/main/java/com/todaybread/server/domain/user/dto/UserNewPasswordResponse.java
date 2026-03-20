package com.todaybread.server.domain.user.dto;

/**
 * 비밀번호가 재설정되었을 떄, 응답 DTO입니다.
 * @param success
 * @param message
 */
public record UserNewPasswordResponse (
        boolean success,
        String message
) {
    public static UserNewPasswordResponse ok(){
        return new UserNewPasswordResponse(true, "비밀번호가 재설정되었습니다.");
    }
}
