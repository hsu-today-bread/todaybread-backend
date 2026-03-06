package com.todaybread.server.domain.user.repository;

import com.todaybread.server.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 유저 도메인에서 사용될 리포지터리입니다.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    /**
     * 내용: 이메일 존재 여부를 확인합니다.
     *
     * @param email 검사할 이메일
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByEmail(String email);

    /**
     * 내용: 닉네임 존재 여부를 확인합니다.
     *
     * @param nickname 검사할 닉네임
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByNickname(String nickname);

    /**
     * 내용: 이메일로 유저 정보를 조회합니다.
     *
     * @param email 조회할 이메일
     * @return 유저 엔티티 (Optional)
     */
    Optional<UserEntity> findByEmail(String email);
}