package com.todaybread.server.domain.auth.controller;

import com.todaybread.server.domain.auth.dto.LogoutResponse;
import com.todaybread.server.domain.auth.dto.TokenReissueRequest;
import com.todaybread.server.domain.auth.dto.TokenResponse;
import com.todaybread.server.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
     * @return token response 응답
     */
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody @Valid TokenReissueRequest request) {
        String oldRefreshToken = request.refreshToken();
        TokenResponse response = authService.reissue(oldRefreshToken);
        return ResponseEntity.ok(response);
    }


    /**
     * 로그아웃 엔드포인트
     * @param jwt 프론트에서 유지 중이던 JWT 토큰
     * @return 로그아웃 성공 응답
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        authService.logout(userId);
        return ResponseEntity.ok(LogoutResponse.ok());
    }
}
