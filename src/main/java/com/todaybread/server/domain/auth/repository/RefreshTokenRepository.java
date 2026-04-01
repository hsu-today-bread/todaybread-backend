package com.todaybread.server.domain.auth.repository;

import com.todaybread.server.domain.auth.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * JWT 인증을 위한 리포지터리입니다.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    /**
     * 유저 ID로 리프레쉬 토큰 엔티티를 조회합니다.
     *
     * @param userId 유저 ID
     * @return 토큰 엔티티 (없으면 빈 Optional)
     */
    Optional<RefreshTokenEntity> findByUserId(Long userId);

    /**
     * 유저 ID로 리프레쉬 토큰을 삭제합니다.
     *
     * @param userId 유저 ID
     */
    void deleteByUserId(Long userId);
}
