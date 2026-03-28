package com.todaybread.server.domain.store.dto;

import java.util.List;

/**
 * 매장 정보 + 이미지를 한번에 내려주는 응답 DTO입니다.
 *
 * @param store  매장 정보
 * @param images 이미지 목록 (displayOrder 오름차순)
 */
public record StoreInfoResponse(
        StoreCommonResponse store,
        List<StoreImageResponse> images
) {
    public static StoreInfoResponse of(StoreCommonResponse store, List<StoreImageResponse> images) {
        return new StoreInfoResponse(store, images);
    }
}
