package com.todaybread.server.domain.keyword.dto;

/**
 * 키워드 조회 응답 DTO 입니다.
 * @param userKeywordId 사용자-키워드 관계 ID
 * @param displayText 사용자가 입력한 원본 키워드 텍스트
 */
public record KeywordResponse(
        Long userKeywordId,
        String displayText
) {
}
