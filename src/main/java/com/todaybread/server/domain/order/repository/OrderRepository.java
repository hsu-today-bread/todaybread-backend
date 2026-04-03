package com.todaybread.server.domain.order.repository;

import com.todaybread.server.domain.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 주문 관련 리포지토리입니다.
 */
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * 유저 ID로 주문 목록을 최신순으로 조회합니다.
     *
     * @param userId 유저 ID
     * @return 주문 엔티티 목록 (최신순)
     */
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
