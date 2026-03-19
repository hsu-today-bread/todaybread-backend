package com.todaybread.server.domain.user.dto;

/**
 * 이메일 찾기 응답 DTO입니다.
 */
public record UserFindEmailResponse(boolean success, String message, String email) {
    public static UserFindEmailResponse ok(String email) {
        return new UserFindEmailResponse(true, "이메일 조회가 완료되었습니다.", email);
    }
}
