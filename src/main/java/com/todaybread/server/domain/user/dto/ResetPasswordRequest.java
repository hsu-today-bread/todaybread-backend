package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 요청 DTO입니다.
 *
 * @param email 이메일
 * @param newPassword 새 비밀번호 (최소 10자)
 */
public record ResetPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 10, message = "비밀번호는 최소 10자 이상이어야 합니다.") String newPassword
) {
}
