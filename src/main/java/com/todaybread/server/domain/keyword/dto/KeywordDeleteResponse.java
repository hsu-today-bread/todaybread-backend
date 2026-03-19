package com.todaybread.server.domain.keyword.dto;

/**
 * 키워드 삭제 응답 DTO입니다.
 */
public record KeywordDeleteResponse(boolean success, String message) {
    public static KeywordDeleteResponse ok() {
        return new KeywordDeleteResponse(true, "키워드가 성공적으로 삭제되었습니다.");
    }
}
