package com.todaybread.server.domain.bread.dto;

import com.todaybread.server.domain.bread.entity.BreadEntity;

/**
 * 빵 공통 응답 포맷입니다.
 *
 * @param id 빵 ID
 * @param storeId 가게 ID
 * @param name 이름
 * @param originalPrice 원가
 * @param salePrice 할인가
 * @param remainingQuantity 재고
 * @param description 설명
 * @param imageUrl 이미지 링크 - 1장 or null
 */
public record BreadCommonResponse(
        Long id,
        Long storeId,
        String name,
        int originalPrice,
        int salePrice,
        int remainingQuantity,
        String description,
        String imageUrl
){
    public static BreadCommonResponse fromEntity(BreadEntity entity, String imageUrl) {
        return new BreadCommonResponse(
                entity.getId(),
                entity.getStoreId(),
                entity.getName(),
                entity.getOriginalPrice(),
                entity.getSalePrice(),
                entity.getRemainingQuantity(),
                entity.getDescription(),
                imageUrl
        );
    }

    public static BreadCommonResponse fromEntity(BreadEntity entity) {
        return fromEntity(entity, null);
    }

    public static BreadCommonResponse deletedOrSoldout(){
        return new BreadCommonResponse(null, null, null, 0, 0, 0, null, null);
    }
}
