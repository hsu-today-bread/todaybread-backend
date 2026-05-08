package com.todaybread.server.domain.interestarea.dto;

/**
 * 관심지역 삭제 응답 DTO
 *
 * @param success 삭제 성공 여부
 * @param keywordNotificationDisabled 키워드 알림 비활성화 여부 (키워드가 1개 이상 등록된 경우 true)
 */
public record InterestAreaDeleteResponse(
        boolean success,
        boolean keywordNotificationDisabled
) {
}
