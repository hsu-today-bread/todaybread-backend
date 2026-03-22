package com.todaybread.server.domain.store.dto;

/**
 * 사장님 인증 후, 탭을 누르는 경우 호출하는 응답 DTO입니다.
 *
 * @param isBoss 사장님 여부
 * @param hasStore 가게 등록 여부
 */
public record StoreStatusResponse(
        boolean isBoss,
        boolean hasStore
) {
    public static StoreStatusResponse ok(boolean isBoss, boolean hasStore){
        return new StoreStatusResponse(isBoss, hasStore);
    }
}
