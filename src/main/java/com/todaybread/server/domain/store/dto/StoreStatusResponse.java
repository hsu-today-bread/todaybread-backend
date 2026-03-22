package com.todaybread.server.domain.store.dto;

/**
 * 사장님 인증 후, 탭을 누르는 경우 호출하는 응답 DTO입니다.
 * 가게가 등록된 경우에는 가게 정보를 보냅니다.
 *
 * @param hasStore 가게 등록 여부
 * @param storeInfo 가게 정보
 */
public record StoreStatusResponse(
        boolean hasStore,
        StoreInfo storeInfo
) {
    public static StoreStatusResponse hasStore(StoreInfo storeInfo) {
        return new StoreStatusResponse(true, storeInfo);
    }
    public static StoreStatusResponse hasNoStore() {
        return new StoreStatusResponse(false, null);
    }
}
