package com.todaybread.server.domain.keyword.dto;

import com.todaybread.server.domain.keyword.entity.UserKeywordEntity;

/**
 * 키워드 응답 DTO입니다.
 * @param keywordId 키워드 ID
 * @param displayText 표시용 텍스트
 */
public record KeywordResponse(Long keywordId, String displayText) {
    public static KeywordResponse from(UserKeywordEntity entity) {
        return new KeywordResponse(entity.getKeywordId(), entity.getDisplayText());
    }
}
