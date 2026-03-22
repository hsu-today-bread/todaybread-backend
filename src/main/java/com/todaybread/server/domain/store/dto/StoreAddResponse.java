package com.todaybread.server.domain.store.dto;

/**
 * 가게 등록 응답 DTO
 * @param success true
 * @param message 메시지
 * @param storeInfo 등록한 가게 정보
 */
public record StoreAddResponse (
        boolean success,
        String message,
        StoreInfo storeInfo
) {
    public static StoreAddResponse ok(StoreInfo storeInfo) {
        return new  StoreAddResponse(true, "가게 등록에 성공했습니다.", storeInfo);
    }
}
