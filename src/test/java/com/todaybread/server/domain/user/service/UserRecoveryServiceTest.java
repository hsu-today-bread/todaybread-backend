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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * {@link UserRecoveryService}의 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class UserRecoveryServiceTest {

    @InjectMocks
    private UserRecoveryService userRecoveryService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private UserEntity createUser() {
        UserEntity user = UserEntity.builder()
                .email("testuser@example.com")
                .passwordHash("hashedPw")
                .name("테스트")
                .nickname("테스터")
                .phoneNumber("010-1234-5678")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    // ========== findEmailByPhone ==========

    @Nested
    @DisplayName("findEmailByPhone")
    class FindEmailByPhone {

        @Test
        @DisplayName("정상 조회 — 5글자 이상 로컬 파트 마스킹")
        void success_longLocal() {
            given(userRepository.findByPhoneNumber("010-1234-5678"))
                    .willReturn(Optional.of(createUser()));

            UserFindEmailResponse result = userRecoveryService.findEmailByPhone("010-1234-5678");

            // testuser -> t******r@example.com
            assertThat(result.maskedEmail()).isEqualTo("t******r@example.com");
        }

        @Test
        @DisplayName("정상 조회 — 1글자 로컬 파트 마스킹")
        void success_singleChar() {
            UserEntity user = UserEntity.builder()
                    .email("a@test.com").passwordHash("pw").name("이름")
                    .nickname("닉").phoneNumber("010-0000-0000").build();
            given(userRepository.findByPhoneNumber("010-0000-0000"))
                    .willReturn(Optional.of(user));

            UserFindEmailResponse result = userRecoveryService.findEmailByPhone("010-0000-0000");

            assertThat(result.maskedEmail()).isEqualTo("*@test.com");
        }

        @Test
        @DisplayName("정상 조회 — 2~4글자 로컬 파트 마스킹")
        void success_shortLocal() {
            UserEntity user = UserEntity.builder()
                    .email("ab@test.com").passwordHash("pw").name("이름")
                    .nickname("닉2").phoneNumber("010-1111-1111").build();
            given(userRepository.findByPhoneNumber("010-1111-1111"))
                    .willReturn(Optional.of(user));

            UserFindEmailResponse result = userRecoveryService.findEmailByPhone("010-1111-1111");

            // ab -> **@test.com
            assertThat(result.maskedEmail()).isEqualTo("**@test.com");
        }

        @Test
        @DisplayName("미등록 전화번호 — USER_RECOVERY_NOT_FOUND")
        void notFound() {
            given(userRepository.findByPhoneNumber("010-9999-9999"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userRecoveryService.findEmailByPhone("010-9999-9999"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_RECOVERY_NOT_FOUND));
        }
    }

    // ========== verifyIdentity ==========

    @Nested
    @DisplayName("verifyIdentity")
    class VerifyIdentity {

        @Test
        @DisplayName("정상 본인 확인")
        void success() {
            given(userRepository.findByPhoneNumberAndEmail("010-1234-5678", "testuser@example.com"))
                    .willReturn(Optional.of(createUser()));

            VerifyIdentityResponse result = userRecoveryService.verifyIdentity(
                    "010-1234-5678", "testuser@example.com");

            assertThat(result.verified()).isTrue();
            assertThat(result.email()).isEqualTo("testuser@example.com");
        }

        @Test
        @DisplayName("불일치 — USER_RECOVERY_NOT_FOUND")
        void notFound() {
            given(userRepository.findByPhoneNumberAndEmail("010-1234-5678", "wrong@test.com"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userRecoveryService.verifyIdentity(
                    "010-1234-5678", "wrong@test.com"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_RECOVERY_NOT_FOUND));
        }
    }

    // ========== resetPassword ==========

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("정상 비밀번호 재설정")
        void success() {
            UserEntity user = createUser();
            given(userRepository.findByEmail("testuser@example.com"))
                    .willReturn(Optional.of(user));
            given(passwordEncoder.encode("newPassword1234")).willReturn("newHashedPw");

            ResetPasswordResponse result = userRecoveryService.resetPassword(
                    new ResetPasswordRequest("testuser@example.com", "newPassword1234"));

            assertThat(result.success()).isTrue();
            assertThat(user.getPasswordHash()).isEqualTo("newHashedPw");
        }

        @Test
        @DisplayName("미등록 이메일 — USER_RECOVERY_NOT_FOUND")
        void notFound() {
            given(userRepository.findByEmail("notfound@test.com"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userRecoveryService.resetPassword(
                    new ResetPasswordRequest("notfound@test.com", "newPassword1234")))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_RECOVERY_NOT_FOUND));
        }
    }
}
