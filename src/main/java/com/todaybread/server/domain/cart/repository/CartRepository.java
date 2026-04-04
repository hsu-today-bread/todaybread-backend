package com.todaybread.server.domain.cart.repository;

import com.todaybread.server.domain.cart.entity.CartEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 장바구니 리포지토리입니다.
 */
public interface CartRepository extends JpaRepository<CartEntity, Long> {

    /**
     * 유저 ID로 장바구니를 조회합니다.
     *
     * @param userId 유저 ID
     * @return 장바구니 엔티티
     */
    Optional<CartEntity> findByUserId(Long userId);

    /**
     * 비관적 락으로 유저의 장바구니를 조회합니다.
     * 주문 생성 시 장바구니 중복 checkout을 방지합니다.
     *
     * @param userId 유저 ID
     * @return 장바구니 엔티티 (락 획득)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CartEntity c WHERE c.userId = :userId")
    Optional<CartEntity> findByUserIdWithLock(@Param("userId") Long userId);
}
