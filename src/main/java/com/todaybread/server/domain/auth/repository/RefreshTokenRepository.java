package com.todaybread.server.domain.auth.repository;

import com.todaybread.server.domain.auth.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * JWT 인증을 위한 리포지터리입니다.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
    Optional<RefreshTokenEntity> findByToken(String refreshToken);
    void deleteByUserId(Long userId);
}
