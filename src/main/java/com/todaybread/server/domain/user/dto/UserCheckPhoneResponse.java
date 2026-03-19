package com.todaybread.server.domain.user.dto;

/**
 * 전화번호 중복 체크 응답 DTO입니다.
 */
public record UserCheckPhoneResponse(boolean success, String message, boolean exists) {
    public static UserCheckPhoneResponse of(boolean exists) {
        String message = exists ? "이미 사용 중인 전화번호입니다." : "사용 가능한 전화번호입니다.";
        return new UserCheckPhoneResponse(true, message, exists);
    }
}
