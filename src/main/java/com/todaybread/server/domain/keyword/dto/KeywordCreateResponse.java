package com.todaybread.server.domain.keyword.dto;

/**
 * 키워드 등록 후 성공 시 반환합니다.
 *
 * @param success true
 */
public record KeywordCreateResponse(boolean success) {
    public static KeywordCreateResponse ok() {
        return new KeywordCreateResponse(true);
    }
}
