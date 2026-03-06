package com.todaybread.server.domain.user.service;

import com.todaybread.server.domain.user.dto.UserLoginRequest;
import com.todaybread.server.domain.user.dto.UserLoginResponse;
import com.todaybread.server.domain.user.dto.UserRegisterRequest;
import com.todaybread.server.domain.user.dto.UserRegisterResponse;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유저 도메인의 서비스 계층입니다.
 * 각종 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 내용: 이메일 중복 여부를 체크합니다.
     *
     * @param email 검사할 이메일
     * @return 중복 여부 (존재하면 true, 존재하지 않으면 false)
     */
    @Transactional(readOnly = true)
    public boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 내용: 닉네임 중복 여부를 체크합니다.
     *
     * @param nickname 검사할 닉네임
     * @return 중복 여부 (존재하면 true, 존재하지 않으면 false)
     */
    @Transactional(readOnly = true)
    public boolean checkNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    /**
     * 내용: 회원가입을 실시합니다. 비밀번호를 해싱하여 저장합니다.
     *
     * @param request 회원가입 요청 DTO
     * @return 회원가입 응답 DTO
     */
    @Transactional
    public UserRegisterResponse register(UserRegisterRequest request) {
        if (checkEmail(request.email())){
            throw new CustomException(ErrorCode.USER_DUPLICATED);
        }
        if (checkNickname(request.nickname())){
            throw new CustomException(ErrorCode.USER_DUPLICATED);
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        
        UserEntity newUser = UserEntity.builder()
                .email(request.email())
                .passwordHash(encodedPassword)
                .name(request.name())
                .nickname(request.nickname())
                .phoneNumber(request.phoneNumber())
                .isBoss(false)
                .build();
                
        userRepository.save(newUser);

        return UserRegisterResponse.ok();
    }

    /**
     * 내용: 로그인을 실시합니다. 이메일과 비밀번호를 검증합니다.
     *
     * @param request 로그인 요청 DTO
     * @return 로그인 응답 DTO
     */
    @Transactional(readOnly = true)
    public UserLoginResponse login(UserLoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return new UserLoginResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}
