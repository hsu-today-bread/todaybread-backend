package com.todaybread.server.domain.store.dto;

import java.util.List;

/**
 * 사장님 인증 후, 탭을 누르는 경우 호출하는 응답 DTO입니다.
 * 가게가 등록된 경우에는 가게 정보와 이미지 목록을 보냅니다.
 *
 * @param hasStore 가게 등록 여부
 * @param storeCommonResponse 가게 정보
 * @param images 가게 이미지 목록 (가게 미등록 시 null)
 */
public record StoreStatusResponse(
        boolean hasStore,
        StoreCommonResponse storeCommonResponse,
        List<StoreImageResponse> images
) {
    public static StoreStatusResponse hasStore(
            StoreCommonResponse storeCommonResponse,
            List<StoreImageResponse> images) {
        return new StoreStatusResponse(true, storeCommonResponse, images);
    }
    public static StoreStatusResponse hasNoStore() {
        return new StoreStatusResponse(false, null, null);
    }
}
