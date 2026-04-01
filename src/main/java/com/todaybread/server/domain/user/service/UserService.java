package com.todaybread.server.domain.user.service;

import com.todaybread.server.config.jwt.JwtTokenService;
import com.todaybread.server.domain.auth.service.AuthService;
import com.todaybread.server.domain.user.dto.*;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 유저 도메인의 서비스 계층입니다.
 * 각종 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final JwtTokenService jwtTokenService;

    /**
     * 이메일 중복 여부를 체크합니다.
     *
     * @param email 이메일
     * @return true/false
     */
    @Transactional(readOnly = true)
    public boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 전화번호 중복 여부를 체크합니다.
     *
     * @param phone 전화번호
     * @return true/false
     */
    @Transactional(readOnly = true)
    public boolean checkPhone(String phone) {
        return userRepository.existsByPhoneNumber(phone);
    }

    /**
     * 닉네임 중복 여부를 체크합니다.
     *
     * @param nickname 닉네임
     * @return true/false
     */
    @Transactional(readOnly = true)
    public boolean checkNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    /**
     * 회원가입을 실시합니다.
     * 중복 체크 -> 비밀번호 해쉬 -> 엔티티 저장으로 진행됩니다.
     */
    @Transactional
    public UserRegisterResponse register(UserRegisterRequest request) {

        // 중복 여부 검증
        if (checkEmail(request.email())){
            throw new CustomException(ErrorCode.USER_REGISTER_EMAIL_ALREADY_EXISTS);
        }
        if (checkPhone(request.phoneNumber())){
            throw new CustomException(ErrorCode.USER_REGISTER_PHONE_ALREADY_EXISTS);
        }
        if (checkNickname(request.nickname())){
            throw new CustomException(ErrorCode.USER_REGISTER_NICKNAME_ALREADY_EXISTS);
        }

        // 비밀번호 해쉬화
        String passwordHash = passwordEncoder.encode(request.password());

        // 엔티티 생성 후 저장
        UserEntity userEntity = UserEntity.builder()
                .email(request.email())
                .passwordHash(passwordHash)
                .name(request.name())
                .nickname(request.nickname())
                .phoneNumber(request.phoneNumber())
                .build();

        userRepository.save(userEntity);
        return UserRegisterResponse.ok();
    }

    /**
     * 로그인을 실시합니다. 기본으로 응답을 던지지만, 예외시 오류 코드를 송출합니다.
     * 로그인 및 각종 토큰을 보내고, 이를 DB에 다시 기록합니다.
     *
     * @param request 로그인 요청 DTO
     * @return 로그인 여부
     */
    @Transactional
    public UserLoginResponse login(UserLoginRequest request) {

        Optional<UserEntity> userEntityOptional = userRepository.findByEmail(request.email());
        if (userEntityOptional.isEmpty()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        UserEntity userEntity = userEntityOptional.get();
        if (!passwordEncoder.matches(request.password(), userEntity.getPasswordHash())) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        Long userId = userEntity.getId();
        String userEmail = userEntity.getEmail();
        String userRole = userEntity.isBoss() ? "BOSS" : "USER";

        String accessToken = jwtTokenService.generateAccessToken(userId,userEmail,userRole);
        String refreshToken = jwtTokenService.generateRefreshToken(userId);

        authService.saveRefreshToken(userId,refreshToken);

        return UserLoginResponse.ok(accessToken, refreshToken, userEntity);
    }

    /**
     * 유저 정보를 업데이트 합니다.
     * 정보를 중복 검사합니다.
     *
     * @param request 요청 DTO
     * @return 응답 DTO
     */
    @Transactional
    public UserUpdateResponse updateProfile(Long userId, UserUpdateRequest request) {
        Optional<UserEntity> userEntityOptional = userRepository.findById(userId);

        if (userEntityOptional.isEmpty()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        UserEntity userEntity = userEntityOptional.get();

        String nickname = request.nickname();
        String name = request.name();
        String phoneNumber = request.phoneNumber();

        if (!userEntity.getNickname().equals(nickname) && checkNickname(nickname)) {
            throw new CustomException(ErrorCode.USER_REGISTER_NICKNAME_ALREADY_EXISTS);
        }
        if (!userEntity.getPhoneNumber().equals(phoneNumber) && checkPhone(phoneNumber)) {
            throw new CustomException(ErrorCode.USER_REGISTER_PHONE_ALREADY_EXISTS);
        }

        userEntity.updateProfile(name, nickname, phoneNumber);

        return UserUpdateResponse.ok(nickname, name, phoneNumber);
    }

    /**
     * 사업자 등록을 처리합니다.
     * 토큰을 업데이트합니다.
     *
     * @param userId 유저 ID
     * @param request 요청 DTO
     * @return 응답 DTO
     */
    // TODO 사업자 등록 서비스 로직 수행
    @Transactional
    public UserBossResponse approveBoss(Long userId, UserBossRequest request) {
        Optional<UserEntity> userEntityOptional = userRepository.findById(userId);

        if (userEntityOptional.isEmpty()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        UserEntity userEntity = userEntityOptional.get();

        if (userEntity.isBoss()){
            throw new CustomException(ErrorCode.USER_BOSS_ALREADY_APPROVED);
        }

        String bossNumber = request.bossNumber();
        if (!bossNumber.matches("\\d{10}")) {
            throw new CustomException(ErrorCode.USER_BOSS_NUMBER_FORMAT_ERROR);
        }

        userEntity.approveBoss();

        String userEmail = userEntity.getEmail();
        String userRole = userEntity.isBoss() ? "BOSS" : "USER";

        String accessToken = jwtTokenService.generateAccessToken(userId,userEmail,userRole);
        String refreshToken = jwtTokenService.generateRefreshToken(userId);

        authService.saveRefreshToken(userId,refreshToken);

        return UserBossResponse.ok(accessToken, refreshToken);
    }


}
