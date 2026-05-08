package com.todaybread.server.domain.interestarea.dto;

import java.util.List;

/**
 * 알림 대상 유저 정보 DTO
 *
 * @param userId 유저 ID
 * @param interestAreaName 유저의 관심지역 이름
 * @param matchedKeywords 매칭된 키워드 목록
 * @param storeInfo 매장 정보
 * @param breadInfo 빵 정보
 * @param minutesUntilLastOrder 라스트 오더까지 남은 시간 (분)
 */
public record NotificationTarget(
        Long userId,
        String interestAreaName,
        List<String> matchedKeywords,
        StoreInfo storeInfo,
        BreadInfo breadInfo,
        long minutesUntilLastOrder
) {
}
