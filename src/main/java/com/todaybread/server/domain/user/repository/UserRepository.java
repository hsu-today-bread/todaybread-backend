package com.todaybread.server.domain.user.repository;

import com.todaybread.server.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 유저 도메인 용 리포지터리입니다.
 * 기본 메서드 + 추가 메서드를 제공합니다.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);
    boolean existsByNickname(String username);
    boolean existsByPhone(String phone);

    Optional<UserEntity> findByEmail(String email);
}
