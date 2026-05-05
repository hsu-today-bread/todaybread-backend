package com.todaybread.server.domain.user.service;

import com.todaybread.server.domain.auth.repository.RefreshTokenRepository;
import com.todaybread.server.domain.user.dto.*;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유저 도메인에서 아이디, 비밀번호 찾기 계층을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class UserRecoveryService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 이메일의 일부를 마스킹하고, 이를 리턴합니다.
     * 로컬 파트 1글자: 전체 '*' 치환, 2~4글자: 첫 글자 + '*' 반복, 5글자 이상: 앞 3글자 + '*' 반복.
     * 도메인 파트는 원본 그대로 유지하며, 결과 문자열 길이는 원본과 동일합니다.
     *
     * @param email 기존 이메일
     * @return 마스킹된 이메일
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (local.length() <= 2) {
            return "*".repeat(local.length()) + domain;
        } else {
            return local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1) + domain;
        }
    }

    /**
     * 들어온 전화번호를 검증하고, 마스킹된 이메일을 반환합니다.
     *
     * @param phone 전화번호
     * @return 마스킹된 이메일
     */
    @Transactional(readOnly = true)
    public UserFindEmailResponse findEmailByPhone(String phone) {
        UserEntity userEntity = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_RECOVERY_NOT_FOUND));

        String email = userEntity.getEmail();
        String maskedEmail = maskEmail(email);

        return new UserFindEmailResponse(maskedEmail);
    }

    /**
     * 전화번호와 이메일을 통해 본인 확인을 수행합니다.
     *
     * @param phone 전화번호
     * @param email 이메일
     * @return 본인 확인 결과
     */
    @Transactional(readOnly = true)
    public VerifyIdentityResponse verifyIdentity(String phone, String email) {
        userRepository.findByPhoneNumberAndEmail(phone, email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_RECOVERY_NOT_FOUND));

        return new VerifyIdentityResponse(true, email);
    }

    /**
     * 새로운 비밀번호를 저장하고 결과를 리턴합니다.
     *
     * @param request 요청 DTO
     * @return 결과값
     */
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        UserEntity userEntity = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_RECOVERY_NOT_FOUND));

        userEntity.changePassword(passwordEncoder.encode(request.newPassword()));

        // 기존 Refresh Token 무효화 (비밀번호 변경 시 기존 세션 강제 종료)
        refreshTokenRepository.deleteByUserId(userEntity.getId());

        return ResetPasswordResponse.ok();
    }
}
