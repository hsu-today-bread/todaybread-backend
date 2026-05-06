package com.todaybread.server.domain.auth.service;

import com.todaybread.server.config.jwt.JwtTokenService;
import com.todaybread.server.domain.auth.dto.TokenResponse;
import com.todaybread.server.domain.auth.entity.RefreshTokenEntity;
import com.todaybread.server.domain.auth.repository.RefreshTokenRepository;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 인증 도메인의 서비스 계층입니다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * 새로운 유저-refresh 토큰을 저장합니다.
     * 기존 토큰을 제거합니다.
     *
     * @param userId 유저 ID
     * @param token 토큰
     */
    @Transactional
    public void saveRefreshToken(Long userId, String token) {
        String tokenHash = passwordEncoder.encode(token);
        LocalDateTime expiresAt = LocalDateTime.now(clock).plus(refreshTokenExpiration, java.time.temporal.ChronoUnit.MILLIS);
        Optional<RefreshTokenEntity> refreshTokenOptional = refreshTokenRepository.findByUserId(userId);

        if (refreshTokenOptional.isPresent()) {
            RefreshTokenEntity entity = refreshTokenOptional.get();
            entity.renew(tokenHash, expiresAt);
            return;
        }

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .userId(userId)
                .token(tokenHash)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(entity);
    }

    /**
     * refresh 토큰을 받고 검증 후, 새 토큰을 생성해서 리턴합니다.
     * DB에는 해시된 토큰이 저장되어 있으므로, JWT에서 userId를 추출한 뒤
     * 해당 유저의 저장된 해시와 비교합니다.
     *
     * @param oldRefreshToken 기존 사용자가 가지고 있던 refresh 토큰
     * @return 새 토큰 쌍 TokenResponse
     */
    @Transactional
    public TokenResponse reissue(String oldRefreshToken){
        // JWT 파싱 및 서명 검증 후 userId 추출
        Long userId;
        try {
            userId = jwtTokenService.parseRefreshToken(oldRefreshToken);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        // userId로 저장된 해시 토큰 조회
        RefreshTokenEntity refreshTokenEntity = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));

        // 해시 비교
        if (!passwordEncoder.matches(oldRefreshToken, refreshTokenEntity.getToken())) {
            throw new CustomException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        if (refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            refreshTokenRepository.delete(refreshTokenEntity);
            throw new CustomException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String userEmail = userEntity.getEmail();
        String userRole = userEntity.getIsBoss() ? "BOSS" : "USER";

        String newRefreshToken = jwtTokenService.generateRefreshToken(userId);
        String newAccessToken = jwtTokenService.generateAccessToken(userId,userEmail,userRole);

        saveRefreshToken(userId, newRefreshToken);

        return TokenResponse.ok(newAccessToken,newRefreshToken);
    }

    /**
     * 로그아웃을 처리합니다. 로그아웃 시, 리프레쉬 토큰을 지웁니다.
     *
     * @param userId 유저 ID
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
