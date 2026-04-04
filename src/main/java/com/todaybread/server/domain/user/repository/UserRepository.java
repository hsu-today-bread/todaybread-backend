package com.todaybread.server.domain.user.repository;

import com.todaybread.server.domain.user.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 유저 도메인 용 리포지터리입니다.
 * 기본 메서드 + 추가 메서드를 제공합니다.
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * 이메일 존재 여부를 확인합니다.
     *
     * @param email 이메일
     * @return 존재하면 true
     */
    boolean existsByEmail(String email);

    /**
     * 닉네임 존재 여부를 확인합니다.
     *
     * @param username 닉네임
     * @return 존재하면 true
     */
    boolean existsByNickname(String username);

    /**
     * 전화번호 존재 여부를 확인합니다.
     *
     * @param phoneNumber 전화번호
     * @return 존재하면 true
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * 이메일로 유저를 조회합니다.
     *
     * @param email 이메일
     * @return 유저 엔티티 (없으면 빈 Optional)
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * 전화번호로 유저를 조회합니다.
     *
     * @param phoneNumber 전화번호
     * @return 유저 엔티티 (없으면 빈 Optional)
     */
    Optional<UserEntity> findByPhoneNumber(String phoneNumber);

    /**
     * 전화번호와 이메일로 유저를 조회합니다.
     *
     * @param phoneNumber 전화번호
     * @param email       이메일
     * @return 유저 엔티티 (없으면 빈 Optional)
     */
    Optional<UserEntity> findByPhoneNumberAndEmail(String phoneNumber, String email);

    /**
     * 비관적 락으로 유저를 조회합니다.
     * 장바구니 최초 생성 시 동시 생성 경쟁을 방지합니다.
     *
     * @param userId 유저 ID
     * @return 유저 엔티티 (락 획득)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserEntity u WHERE u.id = :userId")
    Optional<UserEntity> findByIdWithLock(@Param("userId") Long userId);
}
