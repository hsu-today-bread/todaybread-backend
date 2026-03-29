package com.todaybread.server.domain.bread.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 빵 메뉴 등록 요청 DTO
 *
 * @param name 빵 이름
 * @param originalPrice 원가
 * @param salePrice 할인가
 * @param remainingQuantity 재고
 * @param description 설명
 */
public record BreadAddRequest(
        @NotBlank String name,
        @Min(0) int originalPrice,
        @Min(0) int salePrice,
        @Min(0) int remainingQuantity,
        @NotBlank String description
) {
}
