package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 재설정 요청 DTO입니다.
 *
 * @param email 이메일
 * @param newPassword 새 비밀번호
 */
public record ResetPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank String newPassword
) {
}
