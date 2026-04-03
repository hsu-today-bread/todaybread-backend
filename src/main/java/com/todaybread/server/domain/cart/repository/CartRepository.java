package com.todaybread.server.domain.cart.repository;

import com.todaybread.server.domain.cart.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
