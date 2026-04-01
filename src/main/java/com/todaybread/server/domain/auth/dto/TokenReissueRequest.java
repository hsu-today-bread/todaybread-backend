package com.todaybread.server.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * access token 재 발행을 위한 DTO 입니다
 *
 * @param refreshToken refresh 토큰
 */
public record TokenReissueRequest(
        @NotBlank String refreshToken
) {
}
