package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 새로운 비밀번호를 작성합니다.
 */
public record UserNewPasswordRequest(
        @NotBlank String password
) {
}
