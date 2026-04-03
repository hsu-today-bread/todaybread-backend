package com.todaybread.server.domain.bread.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;

import java.time.LocalTime;

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
 * @param isSelling 판매 상태 (영업시간 내 + 라스트오더 이전 + 재고 있음)
 * @param lastOrderTime 마지막 주문 시간 (null이면 영업 종료 시간이 마감)
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
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        LocalTime lastOrderTime,
        double distance
) {
    public static NearbyBreadResponse of(
            BreadEntity bread, StoreEntity store,
            String imageUrl, double distance, boolean isSelling, LocalTime lastOrderTime) {
        return new NearbyBreadResponse(
                bread.getId(),
                bread.getName(),
                bread.getOriginalPrice(),
                bread.getSalePrice(),
                imageUrl,
                store.getId(),
                store.getName(),
                isSelling,
                lastOrderTime,
                distance
        );
    }
}
