package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 유저 정보 업데이트를 위한 요청 DTO
 *
 * @param nickname 닉네임
 * @param name 이름
 * @param phoneNumber 휴대전화번호
 */
public record UserUpdateRequest(
        @NotBlank String nickname,
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.") String phoneNumber
) {
}
