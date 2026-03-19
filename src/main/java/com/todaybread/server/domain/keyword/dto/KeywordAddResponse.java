package com.todaybread.server.domain.keyword.dto;

/**
 * 키워드 추가 응답 DTO입니다.
 */
public record KeywordAddResponse(boolean success, String message, Long keywordId) {
    public static KeywordAddResponse ok(Long keywordId) {
        return new KeywordAddResponse(true, "키워드가 성공적으로 추가되었습니다.", keywordId);
    }
}
