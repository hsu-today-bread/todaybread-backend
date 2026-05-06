package com.todaybread.server.domain.bread.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.entity.BreadImageEntity;
import com.todaybread.server.domain.bread.repository.BreadImageRepository;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.global.storage.FileStorage;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 4: 과거 데이터 경로에서 삭제된 빵 접근 보장 (실제 DB 기반)
 *
 * For any 삭제된 빵(isDeleted=true)에 대해 findAllById()로 조회하면 해당 빵이 결과에 포함되어야 하며,
 * BreadImageService.getImageUrls()로 이미지를 요청하면 정상적으로 URL이 반환되어야 한다.
 *
 * **Validates: Requirements 6.2, 6.4, 7.2**
 */
@Tag("Feature: bread-soft-delete, Property 4: 과거 데이터 경로에서 삭제된 빵 접근 보장")
class BreadHistoricalAccessPropertyTest {

    private ConfigurableApplicationContext context;
    private BreadRepository breadRepository;
    private BreadImageRepository breadImageRepository;
    private BreadImageService breadImageService;

    @BeforeProperty
    void setUp() {
        context = SpringApplication.run(
                com.todaybread.server.ServerApplication.class,
                "--spring.profiles.active=test",
                "--server.port=0"
        );
        breadRepository = context.getBean(BreadRepository.class);
        breadImageRepository = context.getBean(BreadImageRepository.class);
        breadImageService = context.getBean(BreadImageService.class);
    }

    @AfterProperty
    void tearDown() {
        if (breadImageRepository != null) {
            breadImageRepository.deleteAllInBatch();
        }
        if (breadRepository != null) {
            breadRepository.deleteAllInBatch();
        }
        if (context != null) {
            context.close();
        }
    }

    /**
     * Property 4-1: findAllById()는 삭제된 빵을 결과에 포함한다.
     *
     * 임의의 삭제된 빵(isDeleted=true) 목록을 생성하여 findAllById() 호출 시
     * 모든 삭제된 빵이 결과에 포함됨을 검증한다.
     *
     * **Validates: Requirements 6.2, 6.4**
     */
    @Property(tries = 50)
    void findAllById_returnsDeletedBreads(
            @ForAll("deletedBreadSpecs") List<DeletedBreadSpec> specs
    ) {
        // Arrange: 테스트 전 데이터 정리
        breadImageRepository.deleteAllInBatch();
        breadRepository.deleteAllInBatch();

        Long storeId = 1L;
        List<BreadEntity> savedBreads = new ArrayList<>();

        for (DeletedBreadSpec spec : specs) {
            BreadEntity bread = BreadEntity.builder()
                    .storeId(storeId)
                    .name(spec.name())
                    .description("desc-" + spec.name())
                    .originalPrice(spec.originalPrice())
                    .salePrice(spec.salePrice())
                    .remainingQuantity(spec.quantity())
                    .build();
            BreadEntity saved = breadRepository.save(bread);
            saved.softDelete(spec.deletedAt());
            breadRepository.save(saved);
            savedBreads.add(saved);
        }

        List<Long> breadIds = savedBreads.stream().map(BreadEntity::getId).collect(Collectors.toList());

        // Act: JPA 기본 findAllById는 is_deleted 조건 없이 모든 빵을 반환
        List<BreadEntity> result = breadRepository.findAllById(breadIds);

        // Assert: 모든 삭제된 빵이 결과에 포함되어야 한다
        assertThat(result).hasSize(specs.size());
        for (BreadEntity bread : result) {
            assertThat(bread.isDeleted()).isTrue();
            assertThat(breadIds).contains(bread.getId());
        }

        // Assert: 반환된 빵의 ID 집합이 요청한 ID 집합과 동일해야 한다
        Set<Long> resultIds = result.stream().map(BreadEntity::getId).collect(Collectors.toSet());
        assertThat(resultIds).containsExactlyInAnyOrderElementsOf(breadIds);
    }

    /**
     * Property 4-2: BreadImageService.getImageUrls()는 삭제된 빵의 이미지 URL을 정상 반환한다.
     *
     * 임의의 삭제된 빵에 대해 BreadImageService.getImageUrls()를 호출하면
     * 해당 빵의 이미지 URL이 정상적으로 반환됨을 검증한다.
     *
     * **Validates: Requirements 6.2, 7.2**
     */
    @Property(tries = 50)
    void getImageUrls_returnsValidUrlsForDeletedBreads(
            @ForAll("deletedBreadSpecs") List<DeletedBreadSpec> specs
    ) {
        // Arrange: 테스트 전 데이터 정리
        breadImageRepository.deleteAllInBatch();
        breadRepository.deleteAllInBatch();

        Long storeId = 1L;
        List<BreadEntity> savedBreads = new ArrayList<>();

        for (DeletedBreadSpec spec : specs) {
            BreadEntity bread = BreadEntity.builder()
                    .storeId(storeId)
                    .name(spec.name())
                    .description("desc-" + spec.name())
                    .originalPrice(spec.originalPrice())
                    .salePrice(spec.salePrice())
                    .remainingQuantity(spec.quantity())
                    .build();
            BreadEntity saved = breadRepository.save(bread);
            saved.softDelete(spec.deletedAt());
            breadRepository.save(saved);
            savedBreads.add(saved);

            // 이미지 엔티티 저장
            String storedFilename = "bread_" + saved.getId() + "_" + spec.imageUuid() + ".jpg";
            BreadImageEntity image = BreadImageEntity.builder()
                    .breadId(saved.getId())
                    .originalFilename("original_" + saved.getId() + ".jpg")
                    .storedFilename(storedFilename)
                    .build();
            breadImageRepository.save(image);
        }

        List<Long> breadIds = savedBreads.stream().map(BreadEntity::getId).collect(Collectors.toList());

        // Act: BreadImageService는 breadId 기반으로 조회 (is_deleted 무관)
        Map<Long, String> imageUrls = breadImageService.getImageUrls(breadIds);

        // Assert: 모든 삭제된 빵에 대해 이미지 URL이 반환되어야 한다
        assertThat(imageUrls).hasSize(specs.size());
        for (Long breadId : breadIds) {
            assertThat(imageUrls).containsKey(breadId);
            String url = imageUrls.get(breadId);
            assertThat(url).isNotNull();
        }
    }

    /**
     * Property 4-3: BreadImageService.getImageUrl()는 개별 삭제된 빵의 이미지 URL을 정상 반환한다.
     *
     * 임의의 삭제된 빵에 대해 BreadImageService.getImageUrl()를 호출하면
     * 해당 빵의 이미지 URL이 정상적으로 반환됨을 검증한다.
     *
     * **Validates: Requirements 7.2**
     */
    @Property(tries = 50)
    void getImageUrl_returnsValidUrlForDeletedBread(
            @ForAll("singleDeletedBreadSpec") DeletedBreadSpec spec
    ) {
        // Arrange: 테스트 전 데이터 정리
        breadImageRepository.deleteAllInBatch();
        breadRepository.deleteAllInBatch();

        Long storeId = 1L;
        BreadEntity bread = BreadEntity.builder()
                .storeId(storeId)
                .name(spec.name())
                .description("desc-" + spec.name())
                .originalPrice(spec.originalPrice())
                .salePrice(spec.salePrice())
                .remainingQuantity(spec.quantity())
                .build();
        BreadEntity saved = breadRepository.save(bread);
        saved.softDelete(spec.deletedAt());
        breadRepository.save(saved);

        String storedFilename = "bread_" + saved.getId() + "_" + spec.imageUuid() + ".jpg";
        BreadImageEntity image = BreadImageEntity.builder()
                .breadId(saved.getId())
                .originalFilename("original_" + saved.getId() + ".jpg")
                .storedFilename(storedFilename)
                .build();
        breadImageRepository.save(image);

        // Act: 삭제된 빵이어도 이미지 URL이 정상 반환되어야 한다
        String url = breadImageService.getImageUrl(saved.getId());

        // Assert
        assertThat(url).isNotNull();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<List<DeletedBreadSpec>> deletedBreadSpecs() {
        Arbitrary<DeletedBreadSpec> specArbitrary = Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
                Arbitraries.integers().between(100, 50000),
                Arbitraries.integers().between(100, 50000),
                Arbitraries.integers().between(0, 100),
                Arbitraries.of(2024, 2025, 2026),
                Arbitraries.integers().between(1, 12),
                Arbitraries.integers().between(1, 28),
                Arbitraries.strings().alpha().ofLength(8)
        ).as((name, origPrice, salePrice, qty, year, month, day, uuid) -> {
            int adjustedSalePrice = Math.min(salePrice, origPrice);
            return new DeletedBreadSpec(name, origPrice, adjustedSalePrice, qty,
                    LocalDateTime.of(year, month, day, 12, 0), uuid);
        });

        return specArbitrary.list().ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<DeletedBreadSpec> singleDeletedBreadSpec() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
                Arbitraries.integers().between(100, 50000),
                Arbitraries.integers().between(100, 50000),
                Arbitraries.integers().between(0, 100),
                Arbitraries.of(2024, 2025, 2026),
                Arbitraries.integers().between(1, 12),
                Arbitraries.integers().between(1, 28),
                Arbitraries.strings().alpha().ofLength(8)
        ).as((name, origPrice, salePrice, qty, year, month, day, uuid) -> {
            int adjustedSalePrice = Math.min(salePrice, origPrice);
            return new DeletedBreadSpec(name, origPrice, adjustedSalePrice, qty,
                    LocalDateTime.of(year, month, day, 12, 0), uuid);
        });
    }

    // ──────────────────────────────────────────────────────────────────────
    // Internal record for test data specification
    // ──────────────────────────────────────────────────────────────────────

    record DeletedBreadSpec(String name, int originalPrice, int salePrice, int quantity,
                            LocalDateTime deletedAt, String imageUuid) {}
}
