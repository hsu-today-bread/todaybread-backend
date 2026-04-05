package com.todaybread.server.domain.order.dto;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 상세 응답 DTO
 *
 * @param orderId 주문 ID
 * @param storeName 매장 이름
 * @param status 주문 상태
 * @param totalAmount 총 결제 금액
 * @param createdAt 주문 생성 시각
 * @param items 주문 항목 목록
 */
public record OrderDetailResponse(
        Long orderId,
        String storeName,
        OrderStatus status,
        int totalAmount,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
    /**
     * OrderEntity로부터 상세 응답을 생성합니다.
     *
     * @param order     주문 엔티티
     * @param storeName 매장 이름
     * @param items     주문 항목 응답 목록
     * @return 주문 상세 응답
     */
    public static OrderDetailResponse of(OrderEntity order, String storeName, List<OrderItemResponse> items) {
        return new OrderDetailResponse(
                order.getId(),
                storeName,
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                items
        );
    }
}
