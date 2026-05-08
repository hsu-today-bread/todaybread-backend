package com.todaybread.server.domain.notification.service;

import com.todaybread.server.domain.notification.entity.FcmTokenEntity;
import com.todaybread.server.domain.notification.entity.Platform;
import com.todaybread.server.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FCM 토큰 등록, 갱신, 비활성화, 조회를 담당하는 서비스입니다.
 * 유저당 최대 1개의 활성 토큰을 유지합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final Clock clock;

    /**
     * FCM 토큰을 등록하거나 갱신합니다.
     * 토큰 충돌 시 다른 유저의 row를 삭제한 뒤 현재 유저의 row에 저장합니다.
     *
     * <p>충돌 처리 흐름:
     * <ol>
     *   <li>요청 token이 다른 유저 row에 존재하면 해당 row를 삭제</li>
     *   <li>현재 유저의 row가 존재하면 기존 row를 갱신</li>
     *   <li>현재 유저의 row가 없으면 새 row를 생성</li>
     * </ol>
     *
     * @param userId   유저 ID
     * @param token    FCM 토큰 문자열
     * @param platform 플랫폼 (ANDROID, IOS)
     */
    @Transactional
    public FcmTokenEntity registerToken(Long userId, String token, Platform platform) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 1. 요청 token이 다른 유저 row에 존재하는지 확인
        Optional<FcmTokenEntity> existingTokenRow = fcmTokenRepository.findByToken(token);
        if (existingTokenRow.isPresent() && !existingTokenRow.get().getUserId().equals(userId)) {
            fcmTokenRepository.deleteByToken(token);
            fcmTokenRepository.flush();
        }

        // 2. 현재 유저의 row가 존재하는지 확인
        Optional<FcmTokenEntity> currentUserRow = fcmTokenRepository.findByUserId(userId);
        if (currentUserRow.isPresent()) {
            // 기존 row 갱신
            FcmTokenEntity entity = currentUserRow.get();
            entity.updateToken(token, platform, now);
            return entity;
        } else {
            // 새 row 생성
            FcmTokenEntity newEntity = FcmTokenEntity.builder()
                    .userId(userId)
                    .token(token)
                    .platform(platform)
                    .lastSeenAt(now)
                    .build();
            return fcmTokenRepository.save(newEntity);
        }
    }

    /**
     * 유저의 FCM 토큰을 비활성화합니다.
     * 로그아웃 또는 알림 끄기 시 호출됩니다.
     * 해당 유저의 row가 존재하지 않으면 아무 동작 없이 리턴합니다.
     *
     * @param userId 유저 ID
     */
    @Transactional
    public void deactivateToken(Long userId) {
        fcmTokenRepository.findByUserId(userId)
                .ifPresent(FcmTokenEntity::deactivate);
    }

    /**
     * 유저의 활성 FCM 토큰을 조회합니다.
     *
     * @param userId 유저 ID
     * @return 활성 FCM 토큰 엔티티 (없으면 빈 Optional)
     */
    @Transactional(readOnly = true)
    public Optional<FcmTokenEntity> findActiveToken(Long userId) {
        return fcmTokenRepository.findByUserIdAndActiveTrue(userId);
    }

    /**
     * 여러 유저의 활성 FCM 토큰을 일괄 조회합니다.
     * userId당 최대 1개의 활성 토큰이 존재한다고 가정합니다.
     *
     * @param userIds 유저 ID 목록
     * @return userId를 키로 하는 활성 FCM 토큰 맵
     */
    @Transactional(readOnly = true)
    public Map<Long, FcmTokenEntity> findActiveTokensByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return fcmTokenRepository.findByUserIdInAndActiveTrue(userIds).stream()
                .collect(Collectors.toMap(FcmTokenEntity::getUserId, Function.identity()));
    }

    /**
     * 만료되거나 무효한 토큰을 비활성화합니다.
     * 실패한 token 값 기준으로 해당 row를 active=false 처리합니다.
     * 토큰이 존재하지 않으면 아무 동작 없이 리턴합니다.
     *
     * @param token 만료/무효 FCM 토큰 문자열
     */
    @Transactional
    public void markTokenInvalid(String token) {
        fcmTokenRepository.findByToken(token)
                .ifPresent(FcmTokenEntity::deactivate);
    }
}
