package com.todaybread.server.domain.store.dto;

import com.todaybread.server.domain.bread.dto.BreadCommonResponse;
import com.todaybread.server.domain.store.util.SellingStatus;

import java.util.List;

/**
 * 가게 상세 조회 응답 DTO입니다.
 * 가게 정보 + 이미지 목록 + 빵 목록 + 판매 상태를 한번에 내려줍니다.
 *
 * @param store         가게 정보
 * @param images        이미지 목록
 * @param breads        빵 목록
 * @param isSelling     현재 판매중 여부
 * @param sellingStatus 매장 판매 상태 enum
 * @param averageRating 가게 평균 평점
 * @param reviewCount   가게 리뷰 수
 */
public record StoreDetailResponse(
        StoreCommonResponse store,
        List<StoreImageResponse> images,
        List<BreadCommonResponse> breads,
        boolean isSelling,
        SellingStatus sellingStatus,
        double averageRating,
        int reviewCount
) {
    public static StoreDetailResponse of(
            StoreCommonResponse store,
            List<StoreImageResponse> images,
            List<BreadCommonResponse> breads,
            SellingStatus sellingStatus,
            double averageRating,
            int reviewCount) {
        boolean isSelling = sellingStatus == SellingStatus.SELLING;
        return new StoreDetailResponse(store, images, breads, isSelling, sellingStatus,
                averageRating, reviewCount);
    }
}
