package com.todaybread.server.domain.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 장바구니 수량 변경 요청 DTO
 *
 * @param quantity 변경할 수량
 */
public record CartUpdateRequest(
        @NotNull @Min(1) Integer quantity
) {
}
