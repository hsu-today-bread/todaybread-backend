package com.todaybread.server.domain.store.dto;

/**
 * 가게 등록 응답 DTO
 * @param success true
 * @param message 메시지
 * @param storeCommonResponse 등록한 가게 정보
 */
public record StoreAddResponse (
        boolean success,
        String message,
        StoreCommonResponse storeCommonResponse
) {
    public static StoreAddResponse ok(StoreCommonResponse storeCommonResponse) {
        return new  StoreAddResponse(true, "가게 등록에 성공했습니다.", storeCommonResponse);
    }
}
