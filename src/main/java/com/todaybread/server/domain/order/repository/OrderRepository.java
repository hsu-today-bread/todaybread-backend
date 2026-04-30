package com.todaybread.server.domain.order.repository;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
     * 주문 상태가 파라미터로 전달된 상태이고 생성 시각이 기준 시각 이전인 주문을 생성 시각 오름차순, ID 오름차순으로 반환합니다.
     * 비관적 락 없이 일반 조회로 수행합니다.
     *
     * @param status     주문 상태
     * @param cutoffTime 만료 기준 시각
     * @param pageable   페이지 정보
     * @return 만료 대상 주문 목록 (생성 시각 오름차순, ID 오름차순)
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.status = :status AND o.createdAt < :cutoffTime ORDER BY o.createdAt ASC, o.id ASC")
    List<OrderEntity> findExpiredPendingOrders(@Param("status") OrderStatus status, @Param("cutoffTime") LocalDateTime cutoffTime, Pageable pageable);

    /**
     * 가게의 특정 상태 주문을 생성 시각 내림차순으로 조회합니다.
     *
     * @param storeId 가게 ID
     * @param status  주문 상태
     * @return 주문 엔티티 목록 (최신순)
     */
    List<OrderEntity> findByStoreIdAndStatusOrderByCreatedAtDesc(Long storeId, OrderStatus status);

    /**
     * 가게의 특정 상태 주문을 생성 시각 내림차순으로 페이지네이션 조회합니다.
     *
     * @param storeId  가게 ID
     * @param status   주문 상태
     * @param pageable 페이지 정보
     * @return 주문 엔티티 페이지 (최신순)
     */
    Page<OrderEntity> findByStoreIdAndStatusOrderByCreatedAtDesc(Long storeId, OrderStatus status, Pageable pageable);

    /**
     * 가게 + 주문 날짜 + 주문 번호로 주문 존재 여부를 확인합니다.
     * 주문 번호 중복 체크에 사용합니다.
     *
     * @param storeId     가게 ID
     * @param orderDate   주문 날짜
     * @param orderNumber 주문 번호
     * @return 존재하면 true
     */
    boolean existsByStoreIdAndOrderDateAndOrderNumber(Long storeId, LocalDate orderDate, String orderNumber);
}
