package com.todaybread.server.domain.user.service;

import com.todaybread.server.domain.auth.repository.RefreshTokenRepository;
import com.todaybread.server.domain.user.dto.*;
import com.todaybread.server.domain.user.entity.PasswordResetTokenEntity;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.PasswordResetTokenRepository;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 유저 도메인에서 아이디, 비밀번호 찾기 계층을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class UserRecoveryService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final Clock clock;

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
     * 성공 시 일회용 비밀번호 재설정 토큰을 발급합니다 (10분 유효).
     *
     * @param phone 전화번호
     * @param email 이메일
     * @return 본인 확인 결과 및 재설정 토큰
     */
    @Transactional
    public VerifyIdentityResponse verifyIdentity(String phone, String email) {
        UserEntity userEntity = userRepository.findByPhoneNumberAndEmail(phone, email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_RECOVERY_NOT_FOUND));

        // 기존 토큰이 있으면 삭제
        passwordResetTokenRepository.deleteByUserId(userEntity.getId());

        // 새 토큰 발급 (10분 유효)
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusMinutes(10);

        PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
                .userId(userEntity.getId())
                .token(token)
                .expiresAt(expiresAt)
                .build();
        passwordResetTokenRepository.save(resetToken);

        return new VerifyIdentityResponse(true, email, token);
    }

    /**
     * 새로운 비밀번호를 저장하고 결과를 리턴합니다.
     * 재설정 토큰을 검증하여 본인 확인을 거친 요청만 허용합니다.
     *
     * @param request 요청 DTO
     * @return 결과값
     */
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        // 토큰 검증
        PasswordResetTokenEntity resetToken = passwordResetTokenRepository.findByToken(request.resetToken())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_RESET_TOKEN_INVALID));

        // 만료 확인
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            passwordResetTokenRepository.delete(resetToken);
            throw new CustomException(ErrorCode.USER_RESET_TOKEN_INVALID);
        }

        // 이메일로 유저 조회
        UserEntity userEntity = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_RECOVERY_NOT_FOUND));

        // 토큰의 userId와 이메일의 userId 일치 확인
        if (!resetToken.getUserId().equals(userEntity.getId())) {
            throw new CustomException(ErrorCode.USER_RESET_TOKEN_INVALID);
        }

        userEntity.changePassword(passwordEncoder.encode(request.newPassword()));

        // 기존 Refresh Token 무효화 (비밀번호 변경 시 기존 세션 강제 종료)
        refreshTokenRepository.deleteByUserId(userEntity.getId());

        // 사용된 재설정 토큰 삭제
        passwordResetTokenRepository.delete(resetToken);

        return ResetPasswordResponse.ok();
    }
}
