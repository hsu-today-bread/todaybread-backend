package com.todaybread.server.domain.auth.controller;

import com.todaybread.server.global.util.JwtRoleHelper;
import com.todaybread.server.domain.auth.dto.LogoutResponse;
import com.todaybread.server.domain.auth.dto.TokenReissueRequest;
import com.todaybread.server.domain.auth.dto.TokenResponse;
import com.todaybread.server.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * auth 도메인을 위한 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    /**
     * 토큰 재발급을 위한 엔드 포인트입니다.
     * @param request 기존 refresh 토큰
     * @return token response
     */
    @Operation(summary = "토큰 재발급")
    @PostMapping("/reissue")
    public TokenResponse reissue(@RequestBody @Valid TokenReissueRequest request) {
        String oldRefreshToken = request.refreshToken();
        return authService.reissue(oldRefreshToken);
    }


    /**
     * 로그아웃 엔드포인트
     *
     * @param jwt 프론트에서 유지 중이던 JWT 토큰
     * @return 로그아웃 응답
     */
    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public LogoutResponse logout(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        authService.logout(userId);
        return LogoutResponse.ok();
    }
}
