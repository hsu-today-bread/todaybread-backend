package com.todaybread.server.domain.order.dto;

import com.todaybread.server.domain.order.entity.OrderItemEntity;

/**
 * 주문 항목 응답 DTO
 *
 * @param breadName 빵 이름
 * @param breadPrice 빵 가격 (스냅샷)
 * @param quantity 수량
 */
public record OrderItemResponse(
        String breadName,
        int breadPrice,
        int quantity
) {
    public static OrderItemResponse of(OrderItemEntity orderItem) {
        return new OrderItemResponse(
                orderItem.getBreadName(),
                orderItem.getBreadPrice(),
                orderItem.getQuantity()
        );
    }
}
