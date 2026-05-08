package com.todaybread.server.domain.notification.dto;

import com.todaybread.server.domain.notification.entity.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * FCM 토큰 등록 요청 DTO입니다.
 *
 * @param token FCM 디바이스 토큰
 * @param platform 디바이스 플랫폼 (ANDROID, IOS)
 */
public record FcmTokenRegisterRequest(
        @NotBlank String token,
        @NotNull Platform platform
) {
}
