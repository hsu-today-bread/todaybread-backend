package com.todaybread.server.domain.notification.controller;

import com.todaybread.server.domain.notification.dto.FcmTokenDeactivateResponse;
import com.todaybread.server.domain.notification.dto.FcmTokenRegisterRequest;
import com.todaybread.server.domain.notification.dto.FcmTokenRegisterResponse;
import com.todaybread.server.domain.notification.entity.FcmTokenEntity;
import com.todaybread.server.domain.notification.service.FcmTokenService;
import com.todaybread.server.global.util.JwtRoleHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * FCM 토큰 등록/비활성화 API입니다.
 */
@RestController
@RequestMapping("/api/fcm-tokens")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PostMapping
    public FcmTokenRegisterResponse registerToken(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody FcmTokenRegisterRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        FcmTokenEntity token = fcmTokenService.registerToken(userId, request.token(), request.platform());
        return new FcmTokenRegisterResponse(token.getUserId(), token.getPlatform(), token.getActive());
    }

    @DeleteMapping
    public FcmTokenDeactivateResponse deactivateToken(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        fcmTokenService.deactivateToken(userId);
        return FcmTokenDeactivateResponse.ok();
    }
}
