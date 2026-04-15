package com.todaybread.server.domain.order.repository;

import com.todaybread.server.domain.order.entity.OrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주문 관련 리포지토리입니다.
 */
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * 유저 ID로 주문 목록을 최신순으로 페이지네이션 조회합니다.
     *
     * @param userId   유저 ID
     * @param pageable 페이지 정보
     * @return 주문 엔티티 페이지 (최신순)
     */
    Page<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 유저 ID로 주문 목록을 최신순으로 전체 조회합니다.
     *
     * @param userId 유저 ID
     * @return 주문 엔티티 목록 (최신순)
     */
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 비관적 락으로 주문을 조회합니다.
     * 주문 취소/결제 시 동시성 제어를 위해 사용합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 엔티티 (락 획득)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :orderId")
    Optional<OrderEntity> findByIdWithLock(@Param("orderId") Long orderId);

    /**
     * 유저와 idempotency key로 주문을 조회합니다.
     *
     * @param userId 유저 ID
     * @param idempotencyKey idempotency key
     * @return 주문 엔티티
     */
    Optional<OrderEntity> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    /**
     * 만료 대상 PENDING 주문을 조회합니다.
     * 주문 상태가 PENDING이고 생성 시각이 기준 시각 이전인 주문을 ID 오름차순으로 반환합니다.
     * 비관적 락 없이 일반 조회로 수행합니다.
     *
     * @param cutoffTime 만료 기준 시각
     * @return 만료 대상 주문 목록 (ID 오름차순)
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.status = 'PENDING' AND o.createdAt < :cutoffTime ORDER BY o.id ASC")
    List<OrderEntity> findExpiredPendingOrders(@Param("cutoffTime") LocalDateTime cutoffTime);
}
