package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 재설정 시, 요청 DTO입니다.
 *
 * @param phone 전화번호
 * @param email 이메일
 */
public record UserResetPasswordRequest(
        @NotBlank String phone,
        @NotBlank @Email String email
) {
}
