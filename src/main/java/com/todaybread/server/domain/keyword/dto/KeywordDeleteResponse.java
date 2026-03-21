package com.todaybread.server.domain.keyword.dto;

/**
 * 키워드 삭제 후 반환합니다.
 * @param status 삭제 성공 여부
 * @param message 삭제 결과 메시지
 */
public record KeywordDeleteResponse(boolean status, String message) {
    public static KeywordDeleteResponse ok() {
        return new KeywordDeleteResponse(true, "키워드가 삭제되었습니다.");
    }
}
