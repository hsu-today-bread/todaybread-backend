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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(refreshTokenExpiration * 1_000_000);
        Optional<RefreshTokenEntity> refreshTokenOptional = refreshTokenRepository.findByUserId(userId);

        if (refreshTokenOptional.isPresent()) {
            RefreshTokenEntity entity = refreshTokenOptional.get();
            entity.renew(token, expiresAt);
            return;
        }

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(entity);
    }

    /**
     * refresh 토큰을 받고 검증 후, 조회 후 새 토큰을 생성 해서 리턴합니다.
     *
     * @param oldRefreshToken 기존 사용자가 가지고 있던 refresh 토큰
     * @return 새 토큰 쌍 TokenResponse
     */
    @Transactional
    public TokenResponse reissue(String oldRefreshToken){
        Optional<RefreshTokenEntity> refreshTokenOptional = refreshTokenRepository.findByToken(oldRefreshToken);

        if (refreshTokenOptional.isEmpty()) {
            throw new CustomException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        RefreshTokenEntity refreshTokenEntity = refreshTokenOptional.get();

        if (refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshTokenEntity);
            throw new CustomException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        Long userId = refreshTokenEntity.getUserId();

        Optional<UserEntity> userEntityOptional = userRepository.findById(userId);
        if (userEntityOptional.isEmpty()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        UserEntity userEntity = userEntityOptional.get();

        String userEmail = userEntity.getEmail();
        String userRole = userEntity.isBoss() ? "BOSS" : "USER";

        String newRefreshToken = jwtTokenService.generateRefreshToken(userId);
        String newAccessToken = jwtTokenService.generateAccessToken(userId,userEmail,userRole);

        saveRefreshToken(userId, newRefreshToken);

        return TokenResponse.ok(newAccessToken,newRefreshToken);
    }

    /**
     * 로그아웃을 처리합니다. 로그아웃 시, 리프레쉬 토큰을 지웁니다.
     * @param userId 유저 ID
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
