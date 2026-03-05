package com.todaybread.server.domain.user.service;

import com.todaybread.server.domain.user.dto.UserRegisterRequest;
import com.todaybread.server.domain.user.dto.UserRegisterResponse;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
     */
    @Transactional(readOnly = true)
    public boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 전화번호 중복 여부를 체크합니다.
     */
    @Transactional(readOnly = true)
    public boolean checkPhone(String phone) {
        return userRepository.existsByPhone(phone);
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
    }

}
