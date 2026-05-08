package com.todaybread.server.domain.notification.service;

import com.todaybread.server.domain.notification.event.StockEventType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 9: event_key 형식 정확성 (Event Key Format Correctness)
 *
 * For any breadId, orderId, and eventOccurredAt value:
 * - EventKeyGenerator.keywordStockKey(BREAD_CREATED, breadId, _) SHALL produce "KEYWORD_STOCK:BREAD_CREATED:{breadId}"
 * - EventKeyGenerator.keywordStockKey(BREAD_RESTOCK, breadId, eventOccurredAt) SHALL produce "KEYWORD_STOCK:BREAD_RESTOCK:{breadId}:{eventOccurredAt}"
 * - EventKeyGenerator.favouriteStoreStockKey(BREAD_CREATED, breadId, _) SHALL produce "FAVORITE_STORE_STOCK:BREAD_CREATED:{breadId}"
 * - EventKeyGenerator.favouriteStoreStockKey(BREAD_RESTOCK, breadId, eventOccurredAt) SHALL produce "FAVORITE_STORE_STOCK:BREAD_RESTOCK:{breadId}:{eventOccurredAt}"
 * - EventKeyGenerator.orderCreatedKey(orderId) SHALL produce "ORDER_CREATED:{orderId}"
 *
 * **Validates: Requirements 6.10, 6.11, 7.10, 7.11, 8.10**
 */
@Tag("Feature: fcm-push-notification, Property 9: event_key 형식 정확성")
class EventKeyGeneratorPropertyTest {

    // ──────────────────────────────────────────────────────────────────────
    // Example-Based Unit Tests
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Example-based unit tests")
    class ExampleBasedTests {

        @Test
        @DisplayName("keywordStockKey with BREAD_CREATED produces correct format")
        void keywordStockKey_breadCreated() {
            String key = EventKeyGenerator.keywordStockKey(StockEventType.BREAD_CREATED, 42L, null);
            assertThat(key).isEqualTo("KEYWORD_STOCK:BREAD_CREATED:42");
        }

        @Test
        @DisplayName("keywordStockKey with BREAD_RESTOCK produces correct format")
        void keywordStockKey_breadRestock() {
            LocalDateTime eventTime = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
            String key = EventKeyGenerator.keywordStockKey(StockEventType.BREAD_RESTOCK, 99L, eventTime);
            assertThat(key).isEqualTo("KEYWORD_STOCK:BREAD_RESTOCK:99:2024-06-15T10:30");
        }

        @Test
        @DisplayName("favouriteStoreStockKey with BREAD_CREATED produces correct format")
        void favouriteStoreStockKey_breadCreated() {
            String key = EventKeyGenerator.favouriteStoreStockKey(StockEventType.BREAD_CREATED, 7L, null);
            assertThat(key).isEqualTo("FAVORITE_STORE_STOCK:BREAD_CREATED:7");
        }

        @Test
        @DisplayName("favouriteStoreStockKey with BREAD_RESTOCK produces correct format")
        void favouriteStoreStockKey_breadRestock() {
            LocalDateTime eventTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
            String key = EventKeyGenerator.favouriteStoreStockKey(StockEventType.BREAD_RESTOCK, 123L, eventTime);
            assertThat(key).isEqualTo("FAVORITE_STORE_STOCK:BREAD_RESTOCK:123:2025-01-01T00:00");
        }

        @Test
        @DisplayName("orderCreatedKey produces correct format")
        void orderCreatedKey() {
            String key = EventKeyGenerator.orderCreatedKey(555L);
            assertThat(key).isEqualTo("ORDER_CREATED:555");
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Property-Based Tests
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Property 9.1: keywordStockKey(BREAD_CREATED, breadId, _) produces "KEYWORD_STOCK:BREAD_CREATED:{breadId}"
     *
     * **Validates: Requirements 6.10**
     */
    @Property(tries = 100)
    void keywordStockKey_breadCreated_formatCorrectness(
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long breadId,
            @ForAll("localDateTimes") LocalDateTime eventOccurredAt
    ) {
        String key = EventKeyGenerator.keywordStockKey(StockEventType.BREAD_CREATED, breadId, eventOccurredAt);

        assertThat(key).isEqualTo("KEYWORD_STOCK:BREAD_CREATED:" + breadId);
        assertThat(key).startsWith("KEYWORD_STOCK:BREAD_CREATED:");
        assertThat(key).doesNotContain(eventOccurredAt.toString());
    }

    /**
     * Property 9.2: keywordStockKey(BREAD_RESTOCK, breadId, eventOccurredAt) produces
     * "KEYWORD_STOCK:BREAD_RESTOCK:{breadId}:{eventOccurredAt}"
     *
     * **Validates: Requirements 6.11**
     */
    @Property(tries = 100)
    void keywordStockKey_breadRestock_formatCorrectness(
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long breadId,
            @ForAll("localDateTimes") LocalDateTime eventOccurredAt
    ) {
        String key = EventKeyGenerator.keywordStockKey(StockEventType.BREAD_RESTOCK, breadId, eventOccurredAt);

        String expected = "KEYWORD_STOCK:BREAD_RESTOCK:" + breadId + ":" + eventOccurredAt.toString();
        assertThat(key).isEqualTo(expected);
        assertThat(key).startsWith("KEYWORD_STOCK:BREAD_RESTOCK:");
        assertThat(key).contains(breadId.toString());
        assertThat(key).contains(eventOccurredAt.toString());
    }

    /**
     * Property 9.3: favouriteStoreStockKey(BREAD_CREATED, breadId, _) produces
     * "FAVORITE_STORE_STOCK:BREAD_CREATED:{breadId}"
     *
     * **Validates: Requirements 7.10**
     */
    @Property(tries = 100)
    void favouriteStoreStockKey_breadCreated_formatCorrectness(
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long breadId,
            @ForAll("localDateTimes") LocalDateTime eventOccurredAt
    ) {
        String key = EventKeyGenerator.favouriteStoreStockKey(StockEventType.BREAD_CREATED, breadId, eventOccurredAt);

        assertThat(key).isEqualTo("FAVORITE_STORE_STOCK:BREAD_CREATED:" + breadId);
        assertThat(key).startsWith("FAVORITE_STORE_STOCK:BREAD_CREATED:");
        assertThat(key).doesNotContain(eventOccurredAt.toString());
    }

    /**
     * Property 9.4: favouriteStoreStockKey(BREAD_RESTOCK, breadId, eventOccurredAt) produces
     * "FAVORITE_STORE_STOCK:BREAD_RESTOCK:{breadId}:{eventOccurredAt}"
     *
     * **Validates: Requirements 7.11**
     */
    @Property(tries = 100)
    void favouriteStoreStockKey_breadRestock_formatCorrectness(
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long breadId,
            @ForAll("localDateTimes") LocalDateTime eventOccurredAt
    ) {
        String key = EventKeyGenerator.favouriteStoreStockKey(StockEventType.BREAD_RESTOCK, breadId, eventOccurredAt);

        String expected = "FAVORITE_STORE_STOCK:BREAD_RESTOCK:" + breadId + ":" + eventOccurredAt.toString();
        assertThat(key).isEqualTo(expected);
        assertThat(key).startsWith("FAVORITE_STORE_STOCK:BREAD_RESTOCK:");
        assertThat(key).contains(breadId.toString());
        assertThat(key).contains(eventOccurredAt.toString());
    }

    /**
     * Property 9.5: orderCreatedKey(orderId) produces "ORDER_CREATED:{orderId}"
     *
     * **Validates: Requirements 8.10**
     */
    @Property(tries = 100)
    void orderCreatedKey_formatCorrectness(
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long orderId
    ) {
        String key = EventKeyGenerator.orderCreatedKey(orderId);

        assertThat(key).isEqualTo("ORDER_CREATED:" + orderId);
        assertThat(key).startsWith("ORDER_CREATED:");
        assertThat(key).contains(orderId.toString());
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 임의의 LocalDateTime 값을 생성합니다.
     * 2020-01-01 ~ 2030-12-31 범위의 현실적인 날짜/시간을 생성합니다.
     */
    @Provide
    Arbitrary<LocalDateTime> localDateTimes() {
        Arbitrary<Integer> years = Arbitraries.integers().between(2020, 2030);
        Arbitrary<Integer> months = Arbitraries.integers().between(1, 12);
        Arbitrary<Integer> days = Arbitraries.integers().between(1, 28); // 모든 월에 안전한 범위
        Arbitrary<Integer> hours = Arbitraries.integers().between(0, 23);
        Arbitrary<Integer> minutes = Arbitraries.integers().between(0, 59);
        Arbitrary<Integer> seconds = Arbitraries.integers().between(0, 59);

        return Combinators.combine(years, months, days, hours, minutes, seconds)
                .as(LocalDateTime::of);
    }
}
