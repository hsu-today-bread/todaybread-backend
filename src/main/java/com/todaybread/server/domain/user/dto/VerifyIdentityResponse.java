package com.todaybread.server.domain.user.dto;

/**
 * 본인 확인 응답 DTO입니다.
 *
 * @param verified 본인 확인 성공 여부
 * @param email 이메일
 */
public record VerifyIdentityResponse(
        boolean verified,
        String email
) {
}
