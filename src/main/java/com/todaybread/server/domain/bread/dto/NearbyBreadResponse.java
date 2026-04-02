package com.todaybread.server.domain.bread.dto;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;

/**
 * 근처 빵 목록 조회 응답 DTO입니다.
 * 근처 가게의 개별 빵 정보와 판매 상태, 거리를 포함합니다.
 *
 * @param id 빵 ID
 * @param name 빵 이름
 * @param originalPrice 원가
 * @param salePrice 할인가
 * @param imageUrl 이미지 URL
 * @param storeId 가게 ID
 * @param storeName 가게 이름
 * @param isSelling 판매 상태 (영업시간 내 + 재고 있음)
 * @param distance 유저~가게 거리 (km)
 */
public record NearbyBreadResponse(
        Long id,
        String name,
        int originalPrice,
        int salePrice,
        String imageUrl,
        Long storeId,
        String storeName,
        boolean isSelling,
        double distance
) {
    public static NearbyBreadResponse of(
            BreadEntity bread, StoreEntity store,
            String imageUrl, double distance, boolean isSelling) {
        return new NearbyBreadResponse(
                bread.getId(),
                bread.getName(),
                bread.getOriginalPrice(),
                bread.getSalePrice(),
                imageUrl,
                store.getId(),
                store.getName(),
                isSelling,
                distance
        );
    }
}
