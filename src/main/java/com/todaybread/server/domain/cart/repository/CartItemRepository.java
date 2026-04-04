package com.todaybread.server.domain.cart.repository;

import com.todaybread.server.domain.cart.entity.CartItemEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * 비관적 락으로 장바구니 항목을 조회합니다.
     * checkout 중 수량 변경/삭제와의 경합을 방지합니다.
     *
     * @param cartId 장바구니 ID
     * @return 장바구니 항목 목록 (락 획득)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CartItemEntity c WHERE c.cartId = :cartId")
    List<CartItemEntity> findByCartIdWithLock(@Param("cartId") Long cartId);

    /**
     * 장바구니 ID와 빵 ID로 항목을 조회합니다.
     *
     * @param cartId 장바구니 ID
     * @param breadId 빵 ID
     * @return 장바구니 항목
     */
    Optional<CartItemEntity> findByCartIdAndBreadId(Long cartId, Long breadId);

    /**
     * 장바구니 ID에 해당하는 모든 항목을 벌크 삭제합니다.
     *
     * @param cartId 장바구니 ID
     */
    @Modifying
    @Query("DELETE FROM CartItemEntity c WHERE c.cartId = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);
}
