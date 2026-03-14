package com.todaybread.server.domain.auth.dto;

/**
 * 로그아웃 응답을 위한 DTO입니다.
 * 헬퍼 메서드를 지원합니다.
 *
 * @param success 성공 유무
 */
public record LogoutResponse(
        boolean success
) {
    public static LogoutResponse ok(){
        return new LogoutResponse(true);
    }
}
