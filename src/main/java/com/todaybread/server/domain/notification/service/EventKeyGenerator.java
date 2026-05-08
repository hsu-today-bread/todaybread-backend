package com.todaybread.server.domain.notification.service;

import com.todaybread.server.domain.notification.event.StockEventType;

import java.time.LocalDateTime;

/**
 * 알림 중복 방지를 위한 event_key를 생성하는 유틸리티 클래스.
 * <p>
 * BREAD_CREATED 이벤트는 빵 ID만으로 키를 생성하고 (동일 빵에 대해 1회만 발송),
 * BREAD_RESTOCK 이벤트는 빵 ID + eventOccurredAt으로 키를 생성한다 (재입고 시점마다 발송 가능).
 */
public class EventKeyGenerator {

    private EventKeyGenerator() {
        // utility class
    }

    /**
     * KEYWORD_STOCK 알림 타입의 event_key를 생성한다.
     *
     * @param eventType       재고 이벤트 유형 (BREAD_CREATED 또는 BREAD_RESTOCK)
     * @param breadId         빵 ID
     * @param eventOccurredAt 이벤트 발생 시각 (BREAD_RESTOCK에서만 사용)
     * @return event_key 문자열
     */
    public static String keywordStockKey(StockEventType eventType, Long breadId, LocalDateTime eventOccurredAt) {
        return switch (eventType) {
            case BREAD_CREATED -> "KEYWORD_STOCK:BREAD_CREATED:" + breadId;
            case BREAD_RESTOCK -> "KEYWORD_STOCK:BREAD_RESTOCK:" + breadId + ":" + eventOccurredAt.toString();
        };
    }

    /**
     * FAVORITE_STORE_STOCK 알림 타입의 event_key를 생성한다.
     *
     * @param eventType       재고 이벤트 유형 (BREAD_CREATED 또는 BREAD_RESTOCK)
     * @param breadId         빵 ID
     * @param eventOccurredAt 이벤트 발생 시각 (BREAD_RESTOCK에서만 사용)
     * @return event_key 문자열
     */
    public static String favouriteStoreStockKey(StockEventType eventType, Long breadId, LocalDateTime eventOccurredAt) {
        return switch (eventType) {
            case BREAD_CREATED -> "FAVORITE_STORE_STOCK:BREAD_CREATED:" + breadId;
            case BREAD_RESTOCK -> "FAVORITE_STORE_STOCK:BREAD_RESTOCK:" + breadId + ":" + eventOccurredAt.toString();
        };
    }

    /**
     * ORDER_CREATED 알림 타입의 event_key를 생성한다.
     *
     * @param orderId 주문 ID
     * @return event_key 문자열
     */
    public static String orderCreatedKey(Long orderId) {
        return "ORDER_CREATED:" + orderId;
    }
}
