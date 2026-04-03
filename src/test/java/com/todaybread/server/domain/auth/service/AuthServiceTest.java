package com.todaybread.server.domain.auth.service;

import com.todaybread.server.config.jwt.JwtTokenService;
import com.todaybread.server.domain.auth.dto.TokenResponse;
import com.todaybread.server.domain.auth.entity.RefreshTokenEntity;
import com.todaybread.server.domain.auth.repository.RefreshTokenRepository;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * {@link AuthService}의 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private static final String SECRET = "todaybread-test-jwt-secret-key-at-least-32-bytes!!";
    private JwtTokenService realJwtTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);
        realJwtTokenService = new JwtTokenService(SECRET, 1800000L, 604800000L);
    }

    // ========== saveRefreshToken ==========

    @Nested
    @DisplayName("saveRefreshToken")
    class SaveRefreshToken {

        @Test
        @DisplayName("신규 저장 — 기존 토큰 없으면 새로 생성")
        void newToken() {
            given(passwordEncoder.encode(anyString())).willReturn("hashedToken");
            given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.empty());

            authService.saveRefreshToken(1L, "rawToken");

            verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
        }

        @Test
        @DisplayName("갱신 — 기존 토큰 있으면 renew 호출")
        void renewExisting() {
            RefreshTokenEntity existing = RefreshTokenEntity.builder()
                    .userId(1L).token("oldHash").expiresAt(LocalDateTime.now().plusDays(7)).build();
            given(passwordEncoder.encode(anyString())).willReturn("newHash");
            given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(existing));

            authService.saveRefreshToken(1L, "rawToken");

            assertThat(existing.getToken()).isEqualTo("newHash");
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // ========== reissue ==========

    @Nested
    @DisplayName("reissue")
    class Reissue {

        @Test
        @DisplayName("정상 재발급")
        void success() {
            String validToken = realJwtTokenService.generateRefreshToken(1L);
            given(jwtTokenService.parseRefreshToken(validToken)).willReturn(1L);

            RefreshTokenEntity entity = RefreshTokenEntity.builder()
                    .userId(1L).token("hashedOld").expiresAt(LocalDateTime.now().plusDays(7)).build();
            given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(entity));
            given(passwordEncoder.matches(eq(validToken), eq("hashedOld"))).willReturn(true);

            UserEntity user = UserEntity.builder()
                    .email("test@test.com").passwordHash("pw").name("테스트")
                    .nickname("테스터").phoneNumber("010-1234-5678").build();
            ReflectionTestUtils.setField(user, "id", 1L);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            given(jwtTokenService.generateAccessToken(eq(1L), eq("test@test.com"), eq("USER")))
                    .willReturn("newAccess");
            given(jwtTokenService.generateRefreshToken(1L)).willReturn("newRefresh");
            given(passwordEncoder.encode("newRefresh")).willReturn("newRefreshHash");

            TokenResponse result = authService.reissue(validToken);

            assertThat(result.accessToken()).isEqualTo("newAccess");
            assertThat(result.refreshToken()).isEqualTo("newRefresh");
        }

        @Test
        @DisplayName("잘못된 JWT 형식 — AUTH_REFRESH_TOKEN_INVALID")
        void invalidJwtFormat() {
            given(jwtTokenService.parseRefreshToken("not-a-jwt"))
                    .willThrow(new IllegalArgumentException("invalid jwt"));

            assertThatThrownBy(() -> authService.reissue("not-a-jwt"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        }

        @Test
        @DisplayName("서명 불일치 — AUTH_REFRESH_TOKEN_INVALID")
        void signatureInvalid() {
            String wrongToken = "signed-by-other-secret";
            given(jwtTokenService.parseRefreshToken(wrongToken))
                    .willThrow(new IllegalArgumentException("invalid signature"));

            assertThatThrownBy(() -> authService.reissue(wrongToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        }

        @Test
        @DisplayName("DB에 토큰 없음 — AUTH_REFRESH_TOKEN_INVALID")
        void noTokenInDb() {
            String validToken = realJwtTokenService.generateRefreshToken(1L);
            given(jwtTokenService.parseRefreshToken(validToken)).willReturn(1L);
            given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.reissue(validToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        }

        @Test
        @DisplayName("해시 불일치 — AUTH_REFRESH_TOKEN_INVALID")
        void hashMismatch() {
            String validToken = realJwtTokenService.generateRefreshToken(1L);
            given(jwtTokenService.parseRefreshToken(validToken)).willReturn(1L);

            RefreshTokenEntity entity = RefreshTokenEntity.builder()
                    .userId(1L).token("differentHash").expiresAt(LocalDateTime.now().plusDays(7)).build();
            given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(entity));
            given(passwordEncoder.matches(eq(validToken), eq("differentHash"))).willReturn(false);

            assertThatThrownBy(() -> authService.reissue(validToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        }

        @Test
        @DisplayName("만료된 토큰 — AUTH_REFRESH_TOKEN_INVALID + 삭제")
        void expiredToken() {
            String validToken = realJwtTokenService.generateRefreshToken(1L);
            given(jwtTokenService.parseRefreshToken(validToken)).willReturn(1L);

            RefreshTokenEntity entity = RefreshTokenEntity.builder()
                    .userId(1L).token("hash").expiresAt(LocalDateTime.now().minusDays(1)).build();
            given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(entity));
            given(passwordEncoder.matches(eq(validToken), eq("hash"))).willReturn(true);

            assertThatThrownBy(() -> authService.reissue(validToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
            verify(refreshTokenRepository).delete(entity);
        }
    }

    // ========== logout ==========

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("로그아웃 시 토큰 삭제")
        void success() {
            authService.logout(1L);
            verify(refreshTokenRepository).deleteByUserId(1L);
        }
    }
}
