package com.todaybread.server.domain.user.service;

import com.todaybread.server.domain.auth.repository.RefreshTokenRepository;
import com.todaybread.server.domain.user.dto.ResetPasswordRequest;
import com.todaybread.server.domain.user.dto.ResetPasswordResponse;
import com.todaybread.server.domain.user.dto.UserFindEmailResponse;
import com.todaybread.server.domain.user.dto.VerifyIdentityResponse;
import com.todaybread.server.domain.user.entity.PasswordResetTokenEntity;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.PasswordResetTokenRepository;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserRecoveryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private UserRecoveryService userRecoveryService;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T03:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(clock.instant()).thenReturn(FIXED_INSTANT);
        org.mockito.Mockito.lenient().when(clock.getZone()).thenReturn(ZONE);
    }

    @Test
    void findEmailByPhone_returnsMaskedEmail() {
        UserEntity user = TestFixtures.user(1L, false);
        given(userRepository.findByPhoneNumber(user.getPhoneNumber())).willReturn(Optional.of(user));

        UserFindEmailResponse response = userRecoveryService.findEmailByPhone(user.getPhoneNumber());

        assertThat(response.maskedEmail()).contains("@example.com");
        assertThat(response.maskedEmail()).doesNotContain("user1");
    }

    @Test
    void findEmailByPhone_rejectsUnknownPhone() {
        given(userRepository.findByPhoneNumber("01000000000")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userRecoveryService.findEmailByPhone("01000000000"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_RECOVERY_NOT_FOUND);
    }

    @Test
    void verifyIdentity_returnsVerifiedResponseWithResetToken() {
        UserEntity user = TestFixtures.user(1L, false);
        given(userRepository.findByPhoneNumberAndEmail(user.getPhoneNumber(), user.getEmail()))
                .willReturn(Optional.of(user));
        given(passwordResetTokenRepository.save(any(PasswordResetTokenEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        VerifyIdentityResponse response = userRecoveryService.verifyIdentity(user.getPhoneNumber(), user.getEmail());

        assertThat(response.verified()).isTrue();
        assertThat(response.email()).isEqualTo(user.getEmail());
        assertThat(response.resetToken()).isNotBlank();

        ArgumentCaptor<PasswordResetTokenEntity> captor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        PasswordResetTokenEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getToken()).isEqualTo(response.resetToken());
        assertThat(saved.getExpiresAt()).isEqualTo(LocalDateTime.now(clock).plusMinutes(10));
    }

    @Test
    void resetPassword_updatesPasswordAndClearsTokens() {
        UserEntity user = TestFixtures.user(1L, false);
        PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
                .userId(1L)
                .token("valid-token")
                .expiresAt(LocalDateTime.now(clock).plusMinutes(5))
                .build();

        given(passwordResetTokenRepository.findByToken("valid-token")).willReturn(Optional.of(resetToken));
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.encode("new-password")).willReturn("encoded-new-password");

        ResetPasswordResponse response = userRecoveryService.resetPassword(
                new ResetPasswordRequest(user.getEmail(), "new-password", "valid-token")
        );

        assertThat(response.success()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("encoded-new-password");
        verify(refreshTokenRepository).deleteByUserId(1L);
        verify(passwordResetTokenRepository).delete(resetToken);
    }

    @Test
    void resetPassword_rejectsInvalidToken() {
        given(passwordResetTokenRepository.findByToken("invalid-token")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userRecoveryService.resetPassword(
                new ResetPasswordRequest("user1@example.com", "new-password", "invalid-token")
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_RESET_TOKEN_INVALID);
    }

    @Test
    void resetPassword_rejectsExpiredToken() {
        PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
                .userId(1L)
                .token("expired-token")
                .expiresAt(LocalDateTime.now(clock).minusMinutes(1))
                .build();

        given(passwordResetTokenRepository.findByToken("expired-token")).willReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> userRecoveryService.resetPassword(
                new ResetPasswordRequest("user1@example.com", "new-password", "expired-token")
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_RESET_TOKEN_INVALID);

        verify(passwordResetTokenRepository).delete(resetToken);
    }

    @Test
    void resetPassword_rejectsTokenUserIdMismatch() {
        UserEntity user = TestFixtures.user(1L, false);
        PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
                .userId(999L)
                .token("mismatched-token")
                .expiresAt(LocalDateTime.now(clock).plusMinutes(5))
                .build();

        given(passwordResetTokenRepository.findByToken("mismatched-token")).willReturn(Optional.of(resetToken));
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userRecoveryService.resetPassword(
                new ResetPasswordRequest(user.getEmail(), "new-password", "mismatched-token")
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_RESET_TOKEN_INVALID);
    }
}
