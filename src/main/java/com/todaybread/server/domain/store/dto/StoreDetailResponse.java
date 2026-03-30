package com.todaybread.server.domain.store.dto;

import com.todaybread.server.domain.bread.dto.BreadCommonResponse;

import java.util.List;

/**
 * 가게 상세 조회 응답 DTO입니다.
 * 가게 정보 + 이미지 목록 + 빵 목록을 한번에 내려줍니다.
 *
 * @param store  가게 정보
 * @param images 이미지 목록
 * @param breads 빵 목록
 */
public record StoreDetailResponse(
        StoreCommonResponse store,
        List<StoreImageResponse> images,
        List<BreadCommonResponse> breads
) {
    public static StoreDetailResponse of(
            StoreCommonResponse store,
            List<StoreImageResponse> images,
            List<BreadCommonResponse> breads) {
        return new StoreDetailResponse(store, images, breads);
    }
}
