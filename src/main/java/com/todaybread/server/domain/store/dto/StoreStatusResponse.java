package com.todaybread.server.domain.store.dto;

/**
 * 사장님 인증 후, 탭을 누르는 경우 호출하는 응답 DTO입니다.
 * 단순히 가게가 있는지 아닌지만 보냅니다.
 *
 * @param hasStore 가게 등록 여부
 */
public record StoreStatusResponse(
        boolean hasStore
) {
    public static StoreStatusResponse registered() {
        return new StoreStatusResponse(true);
    }
    public static StoreStatusResponse notRegistered() {
        return new StoreStatusResponse(false);
    }
}
