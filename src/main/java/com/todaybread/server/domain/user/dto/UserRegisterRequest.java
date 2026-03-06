package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원가입 요청 DTO
 *
 * @param email 이메일
 * @param name 실명
 * @param nickname 닉네임
 * @param password 패스워드
 * @param phoneNumber 전화번호
 */
public record UserRegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotBlank String nickname,
        @NotBlank String password,
        String phoneNumber) {
}