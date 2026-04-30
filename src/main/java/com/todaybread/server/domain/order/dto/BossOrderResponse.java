package com.todaybread.server.domain.order.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사장님 주문내역 응답 DTO
 *
 * @param orderId     주문 ID
 * @param orderNumber 주문 번호
 * @param totalAmount 총 주문 금액
 * @param createdAt   주문 생성 시각
 * @param items       주문 항목 목록
 */
public record BossOrderResponse(
        Long orderId,
        String orderNumber,
        int totalAmount,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
}
