package com.todaybread.server.domain.user.service;

import com.todaybread.server.config.jwt.JwtTokenService;
import com.todaybread.server.domain.auth.service.AuthService;
import com.todaybread.server.domain.user.dto.UserBossRequest;
import com.todaybread.server.domain.user.dto.UserBossResponse;
import com.todaybread.server.domain.user.dto.UserLoginRequest;
import com.todaybread.server.domain.user.dto.UserLoginResponse;
import com.todaybread.server.domain.user.dto.UserRegisterRequest;
import com.todaybread.server.domain.user.dto.UserRegisterResponse;
import com.todaybread.server.domain.user.dto.UserUpdateRequest;
import com.todaybread.server.domain.user.dto.UserUpdateResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private UserService userService;

    @Test
    void register_savesEncodedUser() {
        UserRegisterRequest request = new UserRegisterRequest(
                "new@example.com", "nick", "name", "password1234", "010-1234-5678"
        );
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByPhoneNumber(request.phoneNumber())).willReturn(false);
        given(userRepository.existsByNickname(request.nickname())).willReturn(false);
        given(passwordEncoder.encode("password1234")).willReturn("encoded");

        UserRegisterResponse response = userService.register(request);

        assertThat(response.success()).isTrue();
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void register_rejectsDuplicateEmail() {
        UserRegisterRequest request = new UserRegisterRequest(
                "dup@example.com", "nick", "name", "password1234", "010-1234-5678"
        );
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_REGISTER_EMAIL_ALREADY_EXISTS);
    }

    @Test
    void login_returnsTokensAndUserProfile() {
        UserEntity user = TestFixtures.user(1L, false);
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches("plain", user.getPasswordHash())).willReturn(true);
        given(jwtTokenService.generateAccessToken(1L, user.getEmail(), "USER")).willReturn("access");
        given(jwtTokenService.generateRefreshToken(1L)).willReturn("refresh");

        UserLoginResponse response = userService.login(new UserLoginRequest(user.getEmail(), "plain"));

        assertThat(response.success()).isTrue();
        assertThat(response.accessToken()).isEqualTo("access");
        verify(authService).saveRefreshToken(1L, "refresh");
    }

    @Test
    void login_rejectsWrongPassword() {
        UserEntity user = TestFixtures.user(1L, false);
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", user.getPasswordHash())).willReturn(false);

        assertThatThrownBy(() -> userService.login(new UserLoginRequest(user.getEmail(), "wrong")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updateProfile_updatesFieldsWhenValuesAreUnique() {
        UserEntity user = TestFixtures.user(1L, false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("new-nick")).willReturn(false);
        given(userRepository.existsByPhoneNumber("010-9999-8888")).willReturn(false);

        UserUpdateResponse response = userService.updateProfile(1L, new UserUpdateRequest("new-nick", "new-name", "010-9999-8888"));

        assertThat(response.nickname()).isEqualTo("new-nick");
        assertThat(user.getName()).isEqualTo("new-name");
        assertThat(user.getPhoneNumber()).isEqualTo("010-9999-8888");
    }

    @Test
    void approveBoss_rejectsInvalidBossNumber() {
        UserEntity user = TestFixtures.user(1L, false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.approveBoss(1L, new UserBossRequest("invalid")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_BOSS_NUMBER_FORMAT_ERROR);
    }

    @Test
    void approveBoss_updatesRoleAndReturnsNewTokens() {
        UserEntity user = TestFixtures.user(1L, false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtTokenService.generateAccessToken(1L, user.getEmail(), "BOSS")).willReturn("boss-access");
        given(jwtTokenService.generateRefreshToken(1L)).willReturn("boss-refresh");

        UserBossResponse response = userService.approveBoss(1L, new UserBossRequest("1234567890"));

        assertThat(response.success()).isTrue();
        assertThat(user.getIsBoss()).isTrue();
        verify(authService).saveRefreshToken(1L, "boss-refresh");
    }
}
