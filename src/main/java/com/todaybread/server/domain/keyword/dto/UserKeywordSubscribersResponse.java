package com.todaybread.server.domain.keyword.dto;

import java.util.List;

/**
 * 특정 키워드를 구독 중인 유저 목록 조회 응답 DTO입니다.
 */
public record UserKeywordSubscribersResponse(boolean success, String message, List<SubscriberResponse> subscribers) {
    public static UserKeywordSubscribersResponse ok(List<SubscriberResponse> subscribers) {
        return new UserKeywordSubscribersResponse(true, "구독 유저 조회가 완료되었습니다.", subscribers);
    }
}
