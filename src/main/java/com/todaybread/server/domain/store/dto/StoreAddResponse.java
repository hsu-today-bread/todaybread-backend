package com.todaybread.server.domain.store.dto;

/**
 * 가게 등록 응답 DTO
 * @param success true
 * @param message 메시지
 */
public record StoreAddResponse (
        boolean success,
        String message
) {
    public static StoreAddResponse ok(){
        return new  StoreAddResponse(true, "가게 등록에 성공했습니다.");
    }
}
