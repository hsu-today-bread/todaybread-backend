package com.todaybread.server.domain.bread.dto;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;

/**
 * 빵 상세 조회 응답 DTO입니다.
 *
 * @param id 빵 ID
 * @param name 빵 이름
 * @param originalPrice 원가
 * @param salePrice 할인가
 * @param remainingQuantity 재고
 * @param description 설명
 * @param imageUrl 이미지 URL
 * @param storeId 가게 ID
 * @param storeName 가게 이름
 * @param isSelling 판매 상태 (영업시간 내 + 재고 있음)
 */
public record BreadDetailResponse(
        Long id,
        String name,
        int originalPrice,
        int salePrice,
        int remainingQuantity,
        String description,
        String imageUrl,
        Long storeId,
        String storeName,
        boolean isSelling
) {
    public static BreadDetailResponse of(
            BreadEntity bread, StoreEntity store, String imageUrl, boolean isSelling) {
        return new BreadDetailResponse(
                bread.getId(),
                bread.getName(),
                bread.getOriginalPrice(),
                bread.getSalePrice(),
                bread.getRemainingQuantity(),
                bread.getDescription(),
                imageUrl,
                store.getId(),
                store.getName(),
                isSelling
        );
    }
}
