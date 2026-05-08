package com.todaybread.server.domain.interestarea.dto;

import java.util.List;

/**
 * 알림 대상 유저 정보 DTO
 *
 * @param userId 유저 ID
 * @param matchedKeywords 매칭된 키워드 목록
 * @param storeInfo 매장 정보
 * @param breadInfo 빵 정보
 * @param minutesUntilLastOrder 라스트 오더까지 남은 시간 (분)
 */
public record NotificationTarget(
        Long userId,
        List<String> matchedKeywords,
        StoreInfo storeInfo,
        BreadInfo breadInfo,
        long minutesUntilLastOrder
) {
}
