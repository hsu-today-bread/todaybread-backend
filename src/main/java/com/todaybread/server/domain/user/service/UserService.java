package com.todaybread.server.domain.user.service;

import com.todaybread.server.domain.user.dto.UserLoginRequest;
import com.todaybread.server.domain.user.dto.UserLoginResponse;
import com.todaybread.server.domain.user.dto.UserRegisterRequest;
import com.todaybread.server.domain.user.dto.UserRegisterResponse;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유저 도메인의 서비스 계층입니다.
 * 각종 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    // TODO - 리포지터리 연결
    //private final UserRepository userRepository;

    /**
     * 이메일 중복 여부를 체크합니다.
     * @param email 이메일
     * @return true/false
     */
    @Transactional(readOnly = true)
    public boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 전화번호 중복 여부를 체크합니다.
     * @param phone 전화번호
     * @return true/false
     */
    @Transactional(readOnly = true)
    public boolean checkPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    /**
     * 닉네임 중복 여부를 체크합니다.
     * @param nickname 닉네임
     * @return true/false
     */
    @Transactional(readOnly = true)
    public boolean checkNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    /**
     * 회원가입을 실시합니다.
     * 비밀번호를 해쉬화합니다.
     * 오류 발생 시, 에러를 던집니다.
     */
    // TODO - 회원 가입 로직 구현 및 오류 코드 추가 (중복된 경우)
    @Transactional
    public UserRegisterResponse register(UserRegisterRequest request) {
        if (checkEmail(request.email())){
            throw new CustomException(ErrorCode.USER_DUPLICATED);
        }
        if (checkPhone(request.phoneNumber())){
            throw new CustomException(ErrorCode.USER_DUPLICATED);
        }

        String password = request.password();

        return UserRegisterResponse.ok();
    }

    /**
     * 로그인을 실시합니다. 기본으로 응답을 던지지만, 예외시 오류 코드를 송출합니다.
     * @param request
     * @return
     */
    public UserLoginResponse login(UserLoginRequest request) {
        // TODO 로그인 붙이기 및 오류 코드 던지기
    }

}
