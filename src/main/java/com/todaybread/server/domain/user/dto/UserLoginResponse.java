package com.todaybread.server.domain.user.dto;

/**
 * 로그인 완료 후 응답 DTO입니다.
 *
 * @param success 성공 여부
 */
public record UserLoginResponse(boolean success) {
    public static UserLoginResponse ok(){
        return new UserLoginResponse(true);
    }
}
