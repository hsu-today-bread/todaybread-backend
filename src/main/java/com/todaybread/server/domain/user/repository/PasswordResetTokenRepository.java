package com.todaybread.server.domain.user.repository;

import com.todaybread.server.domain.user.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 비밀번호 재설정 토큰 리포지터리입니다.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    /**
     * 유저 ID로 비밀번호 재설정 토큰을 조회합니다.
     *
     * @param userId 유저 ID
     * @return 토큰 엔티티 (없으면 빈 Optional)
     */
    Optional<PasswordResetTokenEntity> findByUserId(Long userId);

    /**
     * 만료된 비밀번호 재설정 토큰을 삭제합니다.
     *
     * @param now 현재 시각
     * @return 삭제된 토큰 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") LocalDateTime now);

    /**
     * 유저 ID로 비밀번호 재설정 토큰을 삭제합니다.
     *
     * @param userId 유저 ID
     */
    void deleteByUserId(Long userId);
}
