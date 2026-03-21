package com.todaybread.server.domain.auth.repository;

import com.todaybread.server.domain.auth.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JWT 인증을 위한 리포지터리입니다.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
    Optional<RefreshTokenEntity> findByToken(String refreshToken);
    Optional<RefreshTokenEntity> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
