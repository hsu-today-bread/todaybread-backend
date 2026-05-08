package com.todaybread.server.domain.notification.controller;

import com.todaybread.server.domain.notification.dto.FcmSendResult;
import com.todaybread.server.domain.notification.service.NotificationService;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.global.util.JwtRoleHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * 개발 환경 FCM 테스트 API입니다.
 */
@RestController
@RequestMapping("/api/dev/fcm")
@RequiredArgsConstructor
public class DevFcmController {

    private final NotificationService notificationService;
    private final Environment environment;

    @PostMapping("/test")
    public FcmSendResult sendTestNotification(@AuthenticationPrincipal Jwt jwt) {
        boolean isDevProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (!isDevProfile) {
            throw new CustomException(ErrorCode.COMMON_ACCESS_DENIED);
        }

        Long userId = JwtRoleHelper.getUserId(jwt);
        return notificationService.sendTestNotification(userId);
    }
}
