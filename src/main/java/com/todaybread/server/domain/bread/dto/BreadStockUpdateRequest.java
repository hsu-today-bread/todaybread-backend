package com.todaybread.server.domain.bread.dto;

import jakarta.validation.constraints.Min;

/**
 * 재고 수정 요청 DTO
 *
 * @param remainingQuantity 수정된 재고
 */
public record BreadStockUpdateRequest(
    @Min(0) int remainingQuantity
) {
}
