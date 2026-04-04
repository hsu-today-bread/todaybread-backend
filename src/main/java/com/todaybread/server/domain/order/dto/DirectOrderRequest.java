package com.todaybread.server.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 바로 구매 요청 DTO
 *
 * @param breadId 빵 ID
 * @param quantity 수량
 */
public record DirectOrderRequest(
        @NotNull Long breadId,
        @NotNull @Min(1) Integer quantity
) {
}
