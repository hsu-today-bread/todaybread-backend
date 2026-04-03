package com.todaybread.server.domain.order.dto;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;

/**
 * 주문 목록 응답 DTO
 *
 * @param orderId 주문 ID
 * @param storeName 매장 이름
 * @param status 주문 상태
 * @param totalAmount 총 결제 금액
 * @param createdAt 주문 생성 시각
 */
public record OrderResponse(
        Long orderId,
        String storeName,
        OrderStatus status,
        int totalAmount,
        LocalDateTime createdAt
) {
    public static OrderResponse of(OrderEntity order, String storeName) {
        return new OrderResponse(
                order.getId(),
                storeName,
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }
}
