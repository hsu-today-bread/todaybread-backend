package com.todaybread.server.domain.cart.repository;

import com.todaybread.server.domain.cart.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 장바구니 항목 리포지토리입니다.
 */
public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {

    /**
     * 장바구니 ID로 모든 항목을 조회합니다.
     *
     * @param cartId 장바구니 ID
     * @return 장바구니 항목 목록
     */
    List<CartItemEntity> findByCartId(Long cartId);

    /**
     * 장바구니 ID와 빵 ID로 항목을 조회합니다.
     *
     * @param cartId 장바구니 ID
     * @param breadId 빵 ID
     * @return 장바구니 항목
     */
    Optional<CartItemEntity> findByCartIdAndBreadId(Long cartId, Long breadId);

    /**
     * 장바구니 ID에 해당하는 모든 항목을 삭제합니다.
     *
     * @param cartId 장바구니 ID
     */
    void deleteByCartId(Long cartId);
}
