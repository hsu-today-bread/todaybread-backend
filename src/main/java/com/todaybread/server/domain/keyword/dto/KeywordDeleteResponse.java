package com.todaybread.server.domain.keyword.dto;

/**
 * 키워드 삭제 후 반환합니다.
 *
 * @param success 삭제 성공 여부
 */
public record KeywordDeleteResponse(boolean success) {
    public static KeywordDeleteResponse ok() {
        return new KeywordDeleteResponse(true);
    }
}
