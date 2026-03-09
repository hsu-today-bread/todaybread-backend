package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 유저의 로그인을 위한 요청 DTO 입니다.
 *
 * @param email 이메일
 * @param password 패스워드
 */
public record UserLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {
}
