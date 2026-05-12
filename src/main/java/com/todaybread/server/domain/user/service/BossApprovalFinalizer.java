package com.todaybread.server.domain.user.service;

import com.todaybread.server.config.jwt.JwtTokenService;
import com.todaybread.server.domain.auth.service.AuthService;
import com.todaybread.server.domain.user.client.dto.NtsBusinessValidationResult;
import com.todaybread.server.domain.user.dto.UserBossResponse;
import com.todaybread.server.domain.user.entity.BusinessApprovalEntity;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.BusinessApprovalRepository;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 국세청 검증 성공 후 사장님 승인을 확정합니다.
 */
@Service
@RequiredArgsConstructor
public class BossApprovalFinalizer {

    private final UserRepository userRepository;
    private final BusinessApprovalRepository businessApprovalRepository;
    private final BusinessNumberHasher businessNumberHasher;
    private final AuthService authService;
    private final JwtTokenService jwtTokenService;
    private final Clock clock;

    @Transactional
    public UserBossResponse finalizeApproval(Long userId, String normalizedBossNumber,
                                             String businessStartDate,
                                             NtsBusinessValidationResult validationResult) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (userEntity.getIsBoss()) {
            throw new CustomException(ErrorCode.USER_BOSS_ALREADY_APPROVED);
        }

        String businessNumberHash = businessNumberHasher.hash(normalizedBossNumber);
        if (businessApprovalRepository.existsByBusinessNumberHash(businessNumberHash)) {
            throw new CustomException(ErrorCode.USER_BOSS_NUMBER_ALREADY_REGISTERED);
        }

        BusinessApprovalEntity approval = BusinessApprovalEntity.builder()
                .userId(userId)
                .businessNumberHash(businessNumberHash)
                .businessNumberLast4(businessNumberHasher.last4(normalizedBossNumber))
                .businessStartDate(businessStartDate)
                .businessStatusCode(validationResult.businessStatusCode())
                .businessStatusName(validationResult.businessStatusName())
                .verifiedAt(LocalDateTime.now(clock))
                .build();

        try {
            businessApprovalRepository.saveAndFlush(approval);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.USER_BOSS_NUMBER_ALREADY_REGISTERED);
        }

        userEntity.approveBoss();

        String accessToken = jwtTokenService.generateAccessToken(userId, userEntity.getEmail(), "BOSS");
        String refreshToken = jwtTokenService.generateRefreshToken(userId);

        authService.saveRefreshToken(userId, refreshToken);

        return UserBossResponse.ok(accessToken, refreshToken);
    }
}
