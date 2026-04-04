package com.todaybread.server.domain.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 장바구니 빵 추가 요청 DTO
 *
 * @param breadId 빵 ID
 * @param quantity 수량
 */
public record CartAddRequest(
        @NotNull Long breadId,
        @NotNull @Min(1) Integer quantity
) {
}
