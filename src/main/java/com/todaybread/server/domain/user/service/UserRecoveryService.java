package com.todaybread.server.domain.user.service;

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
 * 유저 도메인에서 아이디, 비밀번호 찾기 계층을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class UserRecoveryService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 이메일의 일부를 마스킹하고, 이를 리턴합니다.
     * @param email 기존 이메일
     * @return 마스킹된 이메일
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (local.length() <= 1) {
            return "*" + domain;
        }
        return local.charAt(0) + "*".repeat(local.length() - 3) + domain;
    }

    /**
     * 들어온 전화번호를 검증하고, 이메일을 보냅니다.
     * @param phone 이메일
     * @return 마스킹된 이메일
     */
    @Transactional(readOnly = true)
    public UserFindEmailResponse findEmailByPhone(String phone) {
        Optional<UserEntity> userEntityOptional = userRepository.findByEmail(phone);
        if (userEntityOptional.isEmpty()) {
            throw new CustomException(ErrorCode.USER_RECOVERY_NOT_FOUND);
        }
        UserEntity userEntity = userEntityOptional.get();
        String maskedEmail = maskEmail(userEntity.getEmail());

        return new UserFindEmailResponse(maskedEmail);
    }

    /**
     * 전화 번호, 이메일을 통해 비밀번호를 바꿀 수 있는 지 여부를 보냅니다.
     *
     * @param request 요청 DTO
     * @return 여부
     */
    @Transactional(readOnly = true)
    public UserResetPasswordResponse verifyIdentity(UserResetPasswordRequest request) {
        String email = request.email();
        String phone = request.phone();
        Optional<UserEntity> userEntityOptional = userRepository.findByPhoneAndEmail(phone, email);

        if (userEntityOptional.isEmpty()) {
            throw new CustomException(ErrorCode.USER_RECOVERY_NOT_FOUND);
        }
        UserEntity userEntity = userEntityOptional.get();

        if(!request.email().equals(userEntity.getEmail()) && !request.phone().equals(userEntity.getPhone())) {
            throw new CustomException(ErrorCode.USER_RECOVERY_NOT_FOUND);
        }

        return UserResetPasswordResponse.ok();
    }

    /**
     * 새로운 비밀번호를 저장하고 결과를 리턴합니다.
     *
     * @param request 요청 DTO
     * @return 결과값
     */
    @Transactional(readOnly = false)
    public UserNewPasswordResponse resetPassword(UserNewPasswordRequest request) {
        Optional<UserEntity> userEntity

    }


}
