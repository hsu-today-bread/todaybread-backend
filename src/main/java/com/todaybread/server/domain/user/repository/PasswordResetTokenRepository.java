package com.todaybread.server.domain.user.repository;

import com.todaybread.server.domain.user.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * 토큰 문자열로 비밀번호 재설정 토큰을 조회합니다.
     *
     * @param token 토큰 문자열
     * @return 토큰 엔티티 (없으면 빈 Optional)
     */
    Optional<PasswordResetTokenEntity> findByToken(String token);

    /**
     * 유저 ID로 비밀번호 재설정 토큰을 삭제합니다.
     *
     * @param userId 유저 ID
     */
    void deleteByUserId(Long userId);
}
