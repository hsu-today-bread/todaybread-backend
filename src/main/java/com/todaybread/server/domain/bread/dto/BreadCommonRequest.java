package com.todaybread.server.domain.bread.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 빵 메뉴 등록/수정 요청 DTO
 *
 * @param name 빵 이름
 * @param originalPrice 원가
 * @param salePrice 할인가
 * @param remainingQuantity 재고
 * @param description 설명
 */
public record BreadCommonRequest(
        @NotBlank @Size(max = 100) String name,
        @Min(0) int originalPrice,
        @Min(0) int salePrice,
        @Min(0) int remainingQuantity,
        @NotBlank @Size(max = 255) String description
) {
}
