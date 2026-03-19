package com.todaybread.server.domain.keyword.dto;

/**
 * 키워드 존재 여부 확인 응답 DTO입니다.
 */
public record KeywordExistResponse(boolean success, String message, boolean exists) {
    public static KeywordExistResponse of(boolean exists) {
        String message = exists ? "등록된 키워드가 존재합니다." : "등록된 키워드가 없습니다.";
        return new KeywordExistResponse(true, message, exists);
    }
}
