package com.todaybread.server.domain.user.dto;

/**
 * 비밀번호 재설정 응답 DTO입니다.
 *
 * @param success 성공 여부
 * @param message 응답 메시지
 */
public record ResetPasswordResponse(
        boolean success,
        String message
) {
    public static ResetPasswordResponse ok() {
        return new ResetPasswordResponse(true, "비밀번호가 재설정되었습니다.");
    }
}
