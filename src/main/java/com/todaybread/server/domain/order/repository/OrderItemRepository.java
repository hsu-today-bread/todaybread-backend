package com.todaybread.server.domain.order.repository;

import com.todaybread.server.domain.order.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
