package com.todaybread.server.domain.bread.repository;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 2: 판매/노출 경로에서 삭제된 빵 제외
 *
 * For any 빵 목록(일부 is_deleted=true, 일부 is_deleted=false)에서
 * 판매/노출 경로 조회 메서드(findByStoreIdAndIsDeletedFalse, findByIdAndIsDeletedFalse)를
 * 호출하면, 반환 결과에는 isDeleted=true인 빵이 포함되지 않아야 한다.
 *
 * **Validates: Requirements 3.1, 3.2, 3.3**
 */
@Tag("Feature: bread-soft-delete, Property 2: 판매/노출 경로에서 삭제된 빵 제외")
class BreadRepositorySoftDeletePropertyTest {

    private ConfigurableApplicationContext context;
    private BreadRepository breadRepository;

    @BeforeProperty
    void setUp() {
        context = SpringApplication.run(
                com.todaybread.server.ServerApplication.class,
                "--spring.profiles.active=test",
                "--server.port=0"
        );
        breadRepository = context.getBean(BreadRepository.class);
    }

    @AfterProperty
    void tearDown() {
        if (breadRepository != null) {
            breadRepository.deleteAllInBatch();
        }
        if (context != null) {
            context.close();
        }
    }

    /**
     * Property 2-1: findByStoreIdAndIsDeletedFalse는 삭제된 빵을 절대 반환하지 않는다.
     *
     * 임의의 빵 목록(삭제/미삭제 혼합)을 저장한 후 findByStoreIdAndIsDeletedFalse를 호출하면,
     * 반환된 모든 빵의 isDeleted는 false여야 한다.
     *
     * **Validates: Requirements 3.1, 3.2, 3.3**
     */
    @Property(tries = 100)
    void findByStoreIdAndIsDeletedFalse_neverReturnsDeletedBreads(
            @ForAll("breadListsWithMixedDeleteStatus") List<BreadSpec> breadSpecs
    ) {
        // Arrange: 테스트 전 데이터 정리
        breadRepository.deleteAllInBatch();

        Long storeId = 1L;
        List<BreadEntity> savedBreads = new ArrayList<>();

        for (BreadSpec spec : breadSpecs) {
            BreadEntity bread = BreadEntity.builder()
                    .storeId(storeId)
                    .name(spec.name())
                    .description("desc-" + spec.name())
                    .originalPrice(spec.originalPrice())
                    .salePrice(spec.salePrice())
                    .remainingQuantity(spec.quantity())
                    .build();
            BreadEntity saved = breadRepository.save(bread);

            if (spec.deleted()) {
                saved.softDelete(LocalDateTime.of(2026, 1, 1, 12, 0));
                breadRepository.save(saved);
            }
            savedBreads.add(saved);
        }

        // Act
        List<BreadEntity> result = breadRepository.findByStoreIdAndIsDeletedFalse(storeId);

        // Assert: 반환된 빵 중 삭제된 빵이 없어야 한다
        assertThat(result).allMatch(bread -> !bread.isDeleted(),
                "findByStoreIdAndIsDeletedFalse should never return deleted breads");

        // Assert: 반환된 빵의 수는 미삭제 빵의 수와 동일해야 한다
        long expectedCount = breadSpecs.stream().filter(spec -> !spec.deleted()).count();
        assertThat(result).hasSize((int) expectedCount);
    }

    /**
     * Property 2-2: findByIdAndIsDeletedFalse는 삭제된 빵에 대해 empty를 반환한다.
     *
     * 삭제된 빵의 ID로 findByIdAndIsDeletedFalse를 호출하면 Optional.empty()를 반환하고,
     * 미삭제 빵의 ID로 호출하면 해당 빵을 반환해야 한다.
     *
     * **Validates: Requirements 3.1, 3.2, 3.3**
     */
    @Property(tries = 100)
    void findByIdAndIsDeletedFalse_returnsEmptyForDeletedBreads(
            @ForAll("breadListsWithMixedDeleteStatus") List<BreadSpec> breadSpecs
    ) {
        // Arrange: 테스트 전 데이터 정리
        breadRepository.deleteAllInBatch();

        Long storeId = 1L;
        List<BreadEntity> savedBreads = new ArrayList<>();

        for (BreadSpec spec : breadSpecs) {
            BreadEntity bread = BreadEntity.builder()
                    .storeId(storeId)
                    .name(spec.name())
                    .description("desc-" + spec.name())
                    .originalPrice(spec.originalPrice())
                    .salePrice(spec.salePrice())
                    .remainingQuantity(spec.quantity())
                    .build();
            BreadEntity saved = breadRepository.save(bread);

            if (spec.deleted()) {
                saved.softDelete(LocalDateTime.of(2026, 1, 1, 12, 0));
                breadRepository.save(saved);
            }
            savedBreads.add(saved);
        }

        // Act & Assert: 각 빵에 대해 findByIdAndIsDeletedFalse 검증
        for (int i = 0; i < savedBreads.size(); i++) {
            BreadEntity bread = savedBreads.get(i);
            BreadSpec spec = breadSpecs.get(i);
            Optional<BreadEntity> result = breadRepository.findByIdAndIsDeletedFalse(bread.getId());

            if (spec.deleted()) {
                assertThat(result).isEmpty();
            } else {
                assertThat(result).isPresent();
                assertThat(result.get().isDeleted()).isFalse();
                assertThat(result.get().getId()).isEqualTo(bread.getId());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<List<BreadSpec>> breadListsWithMixedDeleteStatus() {
        Arbitrary<BreadSpec> breadSpecArbitrary = Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
                Arbitraries.integers().between(100, 50000),
                Arbitraries.integers().between(100, 50000),
                Arbitraries.integers().between(0, 100),
                Arbitraries.of(true, false)
        ).as((name, origPrice, salePrice, qty, deleted) -> {
            int adjustedSalePrice = Math.min(salePrice, origPrice);
            return new BreadSpec(name, origPrice, adjustedSalePrice, qty, deleted);
        });

        return breadSpecArbitrary.list().ofMinSize(1).ofMaxSize(10);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Internal record for test data specification
    // ──────────────────────────────────────────────────────────────────────

    record BreadSpec(String name, int originalPrice, int salePrice, int quantity, boolean deleted) {}
}
