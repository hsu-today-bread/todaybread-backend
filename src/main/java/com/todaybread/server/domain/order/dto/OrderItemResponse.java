package com.todaybread.server.domain.order.dto;

import com.todaybread.server.domain.order.entity.OrderItemEntity;

/**
 * 주문 항목 응답 DTO
 *
 * @param breadName 빵 이름
 * @param breadPrice 빵 가격 (스냅샷)
 * @param quantity 수량
 * @param breadImageUrl 빵 대표 이미지 URL (nullable, 이미지가 없을 수 있음)
 */
public record OrderItemResponse(
        String breadName,
        int breadPrice,
        int quantity,
        String breadImageUrl
) {
    /**
     * OrderItemEntity로부터 응답을 생성합니다.
     * breadImageUrl은 null로 설정됩니다.
     *
     * @param orderItem 주문 항목 엔티티
     * @return 주문 항목 응답
     */
    public static OrderItemResponse of(OrderItemEntity orderItem) {
        return new OrderItemResponse(
                orderItem.getBreadName(),
                orderItem.getBreadPrice(),
                orderItem.getQuantity(),
                null
        );
    }

    /**
     * OrderItemEntity와 빵 대표 이미지 URL로부터 응답을 생성합니다.
     *
     * @param orderItem 주문 항목 엔티티
     * @param breadImageUrl 빵 대표 이미지 URL (nullable)
     * @return 주문 항목 응답
     */
    public static OrderItemResponse of(OrderItemEntity orderItem, String breadImageUrl) {
        return new OrderItemResponse(
                orderItem.getBreadName(),
                orderItem.getBreadPrice(),
                orderItem.getQuantity(),
                breadImageUrl
        );
    }
}
