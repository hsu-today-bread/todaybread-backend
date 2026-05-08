package com.todaybread.server.domain.notification.event;

import java.time.LocalDateTime;

/**
 * 빵 등록/품절 해제 알림 이벤트입니다.
 *
 * @param breadId         빵 ID
 * @param eventType       재고 이벤트 타입
 * @param eventOccurredAt 이벤트 발생 시각
 */
public record StockNotificationEvent(
        Long breadId,
        StockEventType eventType,
        LocalDateTime eventOccurredAt
) {
}
