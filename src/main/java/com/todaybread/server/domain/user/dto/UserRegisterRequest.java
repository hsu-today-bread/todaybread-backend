package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO
 *
 * @param email 이메일
 * @param nickname 닉네임
 * @param name 이름
 * @param password 패스워드 (최소 10자)
 * @param phoneNumber 전화번호
 */
public record UserRegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String nickname,
        @NotBlank String name,
        @NotBlank @Size(min = 10, message = "비밀번호는 최소 10자 이상이어야 합니다.") String password,
        @NotBlank @Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.") String phoneNumber) {
}
