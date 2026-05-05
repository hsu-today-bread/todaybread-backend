package com.todaybread.server.domain.bread.entity;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 1: Soft delete 상태 전환
 *
 * For any 유효한 BreadEntity(isDeleted=false)에 대해 softDelete(now)를 호출하면,
 * 해당 엔티티의 isDeleted는 true가 되고 deletedAt은 전달된 시각과 동일해야 한다.
 *
 * **Validates: Requirements 2.1, 2.2**
 */
@Tag("Feature: bread-soft-delete, Property 1: Soft delete 상태 전환")
class BreadEntitySoftDeletePropertyTest {

    /**
     * Property 1: 임의의 유효한 BreadEntity에 대해 softDelete(now) 호출 후
     * isDeleted == true이고 deletedAt == now임을 검증한다.
     *
     * **Validates: Requirements 2.1, 2.2**
     */
    @Property(tries = 100)
    void softDelete_setsIsDeletedTrueAndDeletedAtToNow(
            @ForAll("validBreadEntities") BreadEntity bread,
            @ForAll("arbitraryLocalDateTimes") LocalDateTime now
    ) {
        // Pre-condition: 새로 생성된 BreadEntity는 삭제 상태가 아니어야 한다
        assertThat(bread.isDeleted()).isFalse();
        assertThat(bread.getDeletedAt()).isNull();

        // Act
        bread.softDelete(now);

        // Assert
        assertThat(bread.isDeleted()).isTrue();
        assertThat(bread.getDeletedAt()).isEqualTo(now);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<BreadEntity> validBreadEntities() {
        Arbitrary<Long> storeIds = Arbitraries.longs().between(1L, 10000L);
        Arbitrary<String> names = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50);
        Arbitrary<String> descriptions = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(100);
        Arbitrary<Integer> originalPrices = Arbitraries.integers().between(0, 100000);
        Arbitrary<Integer> salePrices = Arbitraries.integers().between(0, 100000);
        Arbitrary<Integer> quantities = Arbitraries.integers().between(0, 1000);

        return Combinators.combine(storeIds, names, descriptions, originalPrices, salePrices, quantities)
                .as((storeId, name, description, originalPrice, salePrice, quantity) ->
                        BreadEntity.builder()
                                .storeId(storeId)
                                .name(name)
                                .description(description)
                                .originalPrice(originalPrice)
                                .salePrice(salePrice)
                                .remainingQuantity(quantity)
                                .build()
                );
    }

    @Provide
    Arbitrary<LocalDateTime> arbitraryLocalDateTimes() {
        return Arbitraries.integers().between(2020, 2030)
                .flatMap(year -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .flatMap(day -> Arbitraries.integers().between(0, 23)
                                        .flatMap(hour -> Arbitraries.integers().between(0, 59)
                                                .flatMap(minute -> Arbitraries.integers().between(0, 59)
                                                        .map(second -> LocalDateTime.of(year, month, day, hour, minute, second))
                                                )
                                        )
                                )
                        )
                );
    }
}
