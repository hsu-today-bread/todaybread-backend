package com.todaybread.server.domain.store.dto;

/**
 * 사장님 인증 후, 탭을 누르는 경우 호출하는 응답 DTO입니다.
 * 가게가 등록된 경우에는 가게 정보를 보냅니다.
 *
 * @param hasStore 가게 등록 여부
 * @param storeCommonResponse 가게 정보
 */
public record StoreStatusResponse(
        boolean hasStore,
        StoreCommonResponse storeCommonResponse
) {
    public static StoreStatusResponse hasStore(StoreCommonResponse storeCommonResponse) {
        return new StoreStatusResponse(true, storeCommonResponse);
    }
    public static StoreStatusResponse hasNoStore() {
        return new StoreStatusResponse(false, null);
    }
}
