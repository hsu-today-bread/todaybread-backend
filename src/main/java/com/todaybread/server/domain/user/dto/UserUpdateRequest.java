package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 유저 정보 업데이트를 위한 요청 DTO
 * @param nickname 닉네임
 * @param name 이름
 * @param email 이메일
 */
public record UserUpdateRequest(
        @NotBlank String nickname,
        @NotBlank String name,
        @NotBlank @Email String email
) {
}
