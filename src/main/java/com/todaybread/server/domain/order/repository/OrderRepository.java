package com.todaybread.server.domain.order.repository;

import com.todaybread.server.domain.order.dto.DailySalesProjection;
import com.todaybread.server.domain.order.dto.PurchaseCountProjection;
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

    /**
     * 일별 매출 합산 쿼리입니다.
     * 가게 + 상태 + 날짜 범위로 orderDate별 매출 합계를 집계합니다.
     *
     * @param storeId       가게 ID
     * @param statuses      포함할 주문 상태 목록
     * @param startDateTime 시작 시각 (포함)
     * @param endDateTime   종료 시각 (미포함)
     * @return 일별 매출 프로젝션 목록 (날짜 오름차순)
     */
    @Query("""
            SELECT o.orderDate AS salesDate,
                   SUM(o.totalAmount) AS totalSales
            FROM OrderEntity o
            WHERE o.storeId = :storeId
              AND o.status IN :statuses
              AND o.createdAt >= :startDateTime
              AND o.createdAt < :endDateTime
            GROUP BY o.orderDate
            ORDER BY o.orderDate
            """)
    List<DailySalesProjection> aggregateDailySales(
            @Param("storeId") Long storeId,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * 특정 가게에서 특정 상태의 주문 수를 유저별로 일괄 집계합니다.
     * 사장님 리뷰 관리 목록에서 각 리뷰 작성자의 구매 횟수를 효율적으로 조회하기 위해 사용합니다.
     *
     * @param storeId 가게 ID
     * @param status  주문 상태
     * @param userIds 유저 ID 목록
     * @return PurchaseCountProjection 목록
     */
    @Query("SELECT o.userId AS userId, COUNT(o) AS purchaseCount FROM OrderEntity o WHERE o.storeId = :storeId AND o.status = :status AND o.userId IN :userIds GROUP BY o.userId")
    List<PurchaseCountProjection> countByStoreIdAndStatusAndUserIdIn(
            @Param("storeId") Long storeId,
            @Param("status") OrderStatus status,
            @Param("userIds") List<Long> userIds);
}
