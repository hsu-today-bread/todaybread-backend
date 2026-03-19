package com.todaybread.server.domain.keyword.dto;

import java.util.List;

/**
 * 키워드 목록 조회 응답 DTO입니다.
 */
public record KeywordListResponse(boolean success, String message, List<KeywordResponse> keywords) {
    public static KeywordListResponse ok(List<KeywordResponse> keywords) {
        return new KeywordListResponse(true, "키워드 목록 조회가 완료되었습니다.", keywords);
    }
}
