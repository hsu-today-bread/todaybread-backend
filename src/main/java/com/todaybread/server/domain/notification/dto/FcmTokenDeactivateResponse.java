package com.todaybread.server.domain.notification.dto;

/**
 * FCM 토큰 비활성화 응답 DTO입니다.
 *
 * @param success 처리 성공 여부
 */
public record FcmTokenDeactivateResponse(
        boolean success
) {
    public static FcmTokenDeactivateResponse ok() {
        return new FcmTokenDeactivateResponse(true);
    }
}
