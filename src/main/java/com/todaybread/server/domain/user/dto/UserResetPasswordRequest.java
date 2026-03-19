package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserResetPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotBlank String name,
        @NotBlank String newPassword
) {}
