package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 유저 정보 업데이트를 위한 요청 DTO
 * @param nickname 닉네임
 * @param name 이름
 * @param phone 휴대전화번호
 */
public record UserUpdateRequest(
        @NotBlank String nickname,
        @NotBlank String name,
        @NotBlank String phone
) {
}
