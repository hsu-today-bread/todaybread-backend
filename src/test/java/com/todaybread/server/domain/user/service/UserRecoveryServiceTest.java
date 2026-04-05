package com.todaybread.server.domain.user.service;

import com.todaybread.server.domain.auth.repository.RefreshTokenRepository;
import com.todaybread.server.domain.user.dto.ResetPasswordRequest;
import com.todaybread.server.domain.user.dto.ResetPasswordResponse;
import com.todaybread.server.domain.user.dto.UserFindEmailResponse;
import com.todaybread.server.domain.user.dto.VerifyIdentityResponse;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @InjectMocks
    private UserRecoveryService userRecoveryService;

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
    void verifyIdentity_returnsVerifiedResponse() {
        UserEntity user = TestFixtures.user(1L, false);
        given(userRepository.findByPhoneNumberAndEmail(user.getPhoneNumber(), user.getEmail()))
                .willReturn(Optional.of(user));

        VerifyIdentityResponse response = userRecoveryService.verifyIdentity(user.getPhoneNumber(), user.getEmail());

        assertThat(response.verified()).isTrue();
        assertThat(response.email()).isEqualTo(user.getEmail());
    }

    @Test
    void resetPassword_updatesPasswordAndClearsRefreshTokens() {
        UserEntity user = TestFixtures.user(1L, false);
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.encode("new-password")).willReturn("encoded-new-password");

        ResetPasswordResponse response = userRecoveryService.resetPassword(
                new ResetPasswordRequest(user.getEmail(), "new-password")
        );

        assertThat(response.success()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("encoded-new-password");
        verify(refreshTokenRepository).deleteByUserId(1L);
    }
}
