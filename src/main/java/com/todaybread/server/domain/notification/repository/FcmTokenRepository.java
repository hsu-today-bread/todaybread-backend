package com.todaybread.server.domain.notification.repository;

import com.todaybread.server.domain.notification.entity.FcmTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * FCM 토큰 관리를 위한 리포지터리입니다.
 */
public interface FcmTokenRepository extends JpaRepository<FcmTokenEntity, Long> {

    /**
     * 특정 유저의 FCM 토큰 row를 조회합니다.
     *
     * @param userId 유저 ID
     * @return FCM 토큰 엔티티 (없으면 빈 Optional)
     */
    Optional<FcmTokenEntity> findByUserId(Long userId);

    /**
     * 토큰 값으로 FCM 토큰 row를 조회합니다.
     *
     * @param token FCM 토큰 문자열
     * @return FCM 토큰 엔티티 (없으면 빈 Optional)
     */
    Optional<FcmTokenEntity> findByToken(String token);

    /**
     * 특정 유저의 활성 FCM 토큰을 조회합니다.
     *
     * @param userId 유저 ID
     * @return 활성 FCM 토큰 엔티티 (없으면 빈 Optional)
     */
    Optional<FcmTokenEntity> findByUserIdAndActiveTrue(Long userId);

    /**
     * 여러 유저의 활성 FCM 토큰을 일괄 조회합니다.
     *
     * @param userIds 유저 ID 목록
     * @return 활성 FCM 토큰 엔티티 목록
     */
    List<FcmTokenEntity> findByUserIdInAndActiveTrue(Collection<Long> userIds);

    /**
     * 토큰 값으로 FCM 토큰 row를 삭제합니다.
     *
     * @param token FCM 토큰 문자열
     */
    void deleteByToken(String token);
}
