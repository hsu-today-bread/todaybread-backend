package com.todaybread.server.domain.notification.dto;

import com.todaybread.server.domain.notification.entity.Platform;

/**
 * FCM 토큰 등록 응답 DTO입니다.
 * 보안상 FCM token 전체를 응답으로 반환하지 않습니다.
 *
 * @param userId 유저 ID
 * @param platform 디바이스 플랫폼
 * @param active 토큰 활성 상태
 */
public record FcmTokenRegisterResponse(
        Long userId,
        Platform platform,
        boolean active
) {
}
