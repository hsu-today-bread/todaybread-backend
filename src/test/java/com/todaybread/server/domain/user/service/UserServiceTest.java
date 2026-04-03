package com.todaybread.server.domain.user.service;

import com.todaybread.server.config.jwt.JwtTokenService;
import com.todaybread.server.domain.auth.service.AuthService;
import com.todaybread.server.domain.user.dto.*;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link UserService}의 단위 테스트입니다.
 * 회원가입, 로그인, 프로필 수정, 사장님 인증 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("register_성공")
    void register_success() {
        // given
        UserRegisterRequest request = new UserRegisterRequest(
                "test@test.com", "테스터", "테스트", "password1234", "010-1234-5678");

        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("010-1234-5678")).thenReturn(false);
        when(userRepository.existsByNickname("테스터")).thenReturn(false);
        when(passwordEncoder.encode("password1234")).thenReturn("hashedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(null);

        // when
        UserRegisterResponse response = userService.register(request);

        // then
        assertThat(response.success()).isTrue();
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("register_이메일중복_에러")
    void register_emailDuplicate_error() {
        // given
        UserRegisterRequest request = new UserRegisterRequest(
                "test@test.com", "테스터", "테스트", "password1234", "010-1234-5678");

        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_REGISTER_EMAIL_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("register_전화번호중복_에러")
    void register_phoneDuplicate_error() {
        // given
        UserRegisterRequest request = new UserRegisterRequest(
                "test@test.com", "테스터", "테스트", "password1234", "010-1234-5678");

        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("010-1234-5678")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_REGISTER_PHONE_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("register_닉네임중복_에러")
    void register_nicknameDuplicate_error() {
        // given
        UserRegisterRequest request = new UserRegisterRequest(
                "test@test.com", "테스터", "테스트", "password1234", "010-1234-5678");

        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("010-1234-5678")).thenReturn(false);
        when(userRepository.existsByNickname("테스터")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_REGISTER_NICKNAME_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("login_성공")
    void login_success() {
        // given
        UserLoginRequest request = new UserLoginRequest("test@test.com", "password1234");
        UserEntity user = createUserWithId(1L);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1234", "hashedPassword")).thenReturn(true);
        when(jwtTokenService.generateAccessToken(eq(1L), eq("test@test.com"), eq("USER")))
                .thenReturn("accessToken");
        when(jwtTokenService.generateRefreshToken(1L)).thenReturn("refreshToken");

        // when
        UserLoginResponse response = userService.login(request);

        // then
        assertThat(response.success()).isTrue();
        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
        verify(authService).saveRefreshToken(1L, "refreshToken");
    }

    @Test
    @DisplayName("login_사용자없음_에러")
    void login_userNotFound_error() {
        // given
        UserLoginRequest request = new UserLoginRequest("notfound@test.com", "password1234");

        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("login_비밀번호불일치_에러")
    void login_passwordMismatch_error() {
        // given
        UserLoginRequest request = new UserLoginRequest("test@test.com", "wrongPassword");
        UserEntity user = createUserWithId(1L);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("updateProfile_성공")
    void updateProfile_success() {
        // given
        Long userId = 1L;
        UserUpdateRequest request = new UserUpdateRequest("새닉네임", "새이름", "010-9999-9999");
        UserEntity user = createUserWithId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("새닉네임")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("010-9999-9999")).thenReturn(false);

        // when
        UserUpdateResponse response = userService.updateProfile(userId, request);

        // then
        assertThat(response.nickname()).isEqualTo("새닉네임");
        assertThat(response.name()).isEqualTo("새이름");
        assertThat(response.phoneNumber()).isEqualTo("010-9999-9999");
    }

    @Test
    @DisplayName("updateProfile_닉네임중복_에러")
    void updateProfile_nicknameDuplicate_error() {
        // given
        Long userId = 1L;
        UserUpdateRequest request = new UserUpdateRequest("중복닉네임", "테스트", "010-1234-5678");
        UserEntity user = createUserWithId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("중복닉네임")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_REGISTER_NICKNAME_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("approveBoss_성공")
    void approveBoss_success() {
        // given
        Long userId = 1L;
        UserBossRequest request = new UserBossRequest("1234567890");
        UserEntity user = createUserWithId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenService.generateAccessToken(eq(userId), eq("test@test.com"), eq("BOSS")))
                .thenReturn("bossAccessToken");
        when(jwtTokenService.generateRefreshToken(userId)).thenReturn("bossRefreshToken");

        // when
        UserBossResponse response = userService.approveBoss(userId, request);

        // then
        assertThat(response.success()).isTrue();
        assertThat(response.accessToken()).isEqualTo("bossAccessToken");
        assertThat(response.refreshToken()).isEqualTo("bossRefreshToken");
        verify(authService).saveRefreshToken(userId, "bossRefreshToken");
    }

    @Test
    @DisplayName("approveBoss_이미승인_에러")
    void approveBoss_alreadyApproved_error() {
        // given
        Long userId = 1L;
        UserBossRequest request = new UserBossRequest("1234567890");
        UserEntity user = createUserWithId(userId);
        user.approveBoss(); // 이미 사장님 승인 상태

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.approveBoss(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_BOSS_ALREADY_APPROVED);
    }

    @Test
    @DisplayName("approveBoss_번호형식오류_에러")
    void approveBoss_numberFormatError() {
        // given
        Long userId = 1L;
        UserBossRequest request = new UserBossRequest("12345"); // 10자리가 아님
        UserEntity user = createUserWithId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.approveBoss(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_BOSS_NUMBER_FORMAT_ERROR);
    }

    // ===== 헬퍼 메서드 =====

    private UserEntity createUserWithId(Long id) {
        UserEntity user = UserEntity.builder()
                .email("test@test.com")
                .passwordHash("hashedPassword")
                .name("테스트")
                .nickname("테스터")
                .phoneNumber("010-1234-5678")
                .build();
        setId(user, id);
        return user;
    }

    private void setId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
