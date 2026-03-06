package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO
 *
 * @param email 이메일
 * @param password 패스워드
 */
public record UserLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {
}