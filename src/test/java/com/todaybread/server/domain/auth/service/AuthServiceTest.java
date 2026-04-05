package com.todaybread.server.domain.auth.service;

import com.todaybread.server.config.jwt.JwtTokenService;
import com.todaybread.server.domain.auth.dto.TokenResponse;
import com.todaybread.server.domain.auth.entity.RefreshTokenEntity;
import com.todaybread.server.domain.auth.repository.RefreshTokenRepository;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 60_000L);
    }

    @Test
    void saveRefreshToken_savesNewEntityWhenMissing() {
        given(passwordEncoder.encode("refresh-token")).willReturn("encoded-token");
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.empty());

        authService.saveRefreshToken(1L, "refresh-token");

        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    @Test
    void saveRefreshToken_renewsExistingEntity() {
        RefreshTokenEntity refreshToken = TestFixtures.refreshToken(1L, 1L, "old", LocalDateTime.now().minusDays(1));
        given(passwordEncoder.encode("refresh-token")).willReturn("encoded-token");
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(refreshToken));

        authService.saveRefreshToken(1L, "refresh-token");

        assertThat(refreshToken.getToken()).isEqualTo("encoded-token");
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void reissue_rejectsUnparseableToken() {
        given(jwtTokenService.parseRefreshToken("bad-token")).willThrow(new IllegalArgumentException("bad token"));

        assertThatThrownBy(() -> authService.reissue("bad-token"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
    }

    @Test
    void reissue_deletesExpiredRefreshToken() {
        RefreshTokenEntity refreshToken = TestFixtures.refreshToken(
                1L, 1L, "encoded-old", LocalDateTime.now().minusMinutes(1)
        );
        given(jwtTokenService.parseRefreshToken("refresh-token")).willReturn(1L);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(refreshToken));
        given(passwordEncoder.matches("refresh-token", "encoded-old")).willReturn(true);

        assertThatThrownBy(() -> authService.reissue("refresh-token"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);

        verify(refreshTokenRepository).delete(refreshToken);
    }

    @Test
    void reissue_returnsNewTokensWhenRefreshTokenIsValid() {
        RefreshTokenEntity refreshToken = TestFixtures.refreshToken(
                1L, 1L, "encoded-old", LocalDateTime.now().plusMinutes(10)
        );
        UserEntity user = TestFixtures.user(1L, true);

        given(jwtTokenService.parseRefreshToken("refresh-token")).willReturn(1L);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(refreshToken));
        given(passwordEncoder.matches("refresh-token", "encoded-old")).willReturn(true);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtTokenService.generateRefreshToken(1L)).willReturn("new-refresh");
        given(jwtTokenService.generateAccessToken(1L, user.getEmail(), "BOSS")).willReturn("new-access");
        given(passwordEncoder.encode("new-refresh")).willReturn("new-encoded-refresh");

        TokenResponse response = authService.reissue("refresh-token");

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(refreshToken.getToken()).isEqualTo("new-encoded-refresh");
    }

    @Test
    void logout_deletesRefreshTokenByUserId() {
        authService.logout(1L);

        verify(refreshTokenRepository).deleteByUserId(1L);
    }
}
