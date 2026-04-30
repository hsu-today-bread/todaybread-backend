package com.todaybread.server.domain.order.repository;

import com.todaybread.server.domain.order.dto.SalesAggregateProjection;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 항목 관련 리포지토리입니다.
 */
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    /**
     * 주문 ID로 주문 항목 목록을 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 항목 엔티티 목록
     */
    List<OrderItemEntity> findByOrderId(Long orderId);

    /**
     * 여러 주문 ID로 주문 항목 목록을 일괄 조회합니다.
     *
     * @param orderIds 주문 ID 목록
     * @return 주문 항목 엔티티 목록
     */
    List<OrderItemEntity> findByOrderIdIn(List<Long> orderIds);

    /**
     * 매출 집계 쿼리입니다.
     * 가게 + 상태 + 날짜 범위로 breadName별 판매 수량과 매출을 집계합니다.
     *
     * @param storeId       가게 ID
     * @param statuses      포함할 주문 상태 목록
     * @param startDateTime 시작 시각 (포함)
     * @param endDateTime   종료 시각 (미포함)
     * @return 매출 집계 프로젝션 목록
     */
    @Query("""
            SELECT oi.breadId AS breadId,
                   oi.breadName AS breadName,
                   SUM(oi.quantity) AS totalQuantity,
                   SUM(oi.breadPrice * oi.quantity) AS totalSales
            FROM OrderItemEntity oi
            JOIN OrderEntity o ON oi.orderId = o.id
            WHERE o.storeId = :storeId
              AND o.status IN :statuses
              AND o.createdAt >= :startDateTime
              AND o.createdAt < :endDateTime
            GROUP BY oi.breadId, oi.breadName
            """)
    List<SalesAggregateProjection> aggregateSales(
            @Param("storeId") Long storeId,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);
}
