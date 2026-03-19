package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserFindEmailRequest(
        @NotBlank String name,
        @NotBlank String phone
) {}
