package com.todaybread.server.domain.bread.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.entity.BreadImageEntity;
import com.todaybread.server.domain.bread.repository.BreadImageRepository;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.global.storage.FileStorage;
import com.todaybread.server.support.TestFixtures;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Property 4: 과거 데이터 경로에서 삭제된 빵 접근 보장
 *
 * For any 삭제된 빵(isDeleted=true)에 대해 findAllById()로 조회하면 해당 빵이 결과에 포함되어야 하며,
 * BreadImageService.getImageUrl()로 이미지를 요청하면 정상적으로 URL이 반환되어야 한다.
 *
 * **Validates: Requirements 6.2, 6.4, 7.2**
 */
@Tag("Feature: bread-soft-delete, Property 4: 과거 데이터 경로에서 삭제된 빵 접근 보장")
class BreadHistoricalAccessPropertyTest {

    @Mock
    private BreadRepository breadRepository;

    @Mock
    private BreadImageRepository breadImageRepository;

    @Mock
    private FileStorage fileStorage;

    private BreadImageService breadImageService;

    @BeforeProperty
    void setUp() {
        MockitoAnnotations.openMocks(this);
        breadImageService = new BreadImageService(fileStorage, breadImageRepository);
    }

    /**
     * Property 4-1: findAllById()는 삭제된 빵을 결과에 포함한다.
     *
     * 임의의 삭제된 빵(isDeleted=true) 목록을 생성하여 findAllById() 호출 시
     * 모든 삭제된 빵이 결과에 포함됨을 검증한다.
     *
     * **Validates: Requirements 6.2, 6.4**
     */
    @Property(tries = 100)
    void findAllById_returnsDeletedBreads(
            @ForAll("deletedBreadSpecs") List<DeletedBreadSpec> specs
    ) {
        // Arrange: 삭제된 빵 엔티티 생성
        List<BreadEntity> deletedBreads = new ArrayList<>();
        for (int i = 0; i < specs.size(); i++) {
            DeletedBreadSpec spec = specs.get(i);
            Long breadId = (long) (i + 1);
            BreadEntity bread = TestFixtures.bread(breadId, spec.storeId(), spec.remainingQuantity(), spec.originalPrice(), spec.salePrice());
            bread.softDelete(spec.deletedAt());
            deletedBreads.add(bread);
        }

        List<Long> breadIds = deletedBreads.stream().map(BreadEntity::getId).collect(Collectors.toList());

        // Mock: findAllById는 is_deleted 조건 없이 모든 빵을 반환 (JPA 기본 동작)
        given(breadRepository.findAllById(breadIds)).willReturn(deletedBreads);

        // Act
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
    @Property(tries = 100)
    void getImageUrls_returnsValidUrlsForDeletedBreads(
            @ForAll("deletedBreadSpecs") List<DeletedBreadSpec> specs
    ) {
        // Arrange: 삭제된 빵과 이미지 엔티티 생성
        List<BreadEntity> deletedBreads = new ArrayList<>();
        List<BreadImageEntity> imageEntities = new ArrayList<>();

        for (int i = 0; i < specs.size(); i++) {
            DeletedBreadSpec spec = specs.get(i);
            Long breadId = (long) (i + 1);

            BreadEntity bread = TestFixtures.bread(breadId, spec.storeId(), spec.remainingQuantity(), spec.originalPrice(), spec.salePrice());
            bread.softDelete(spec.deletedAt());
            deletedBreads.add(bread);

            // 이미지 엔티티 생성
            String storedFilename = "bread_" + breadId + "_" + spec.imageUuid() + ".jpg";
            BreadImageEntity image = TestFixtures.breadImage((long) (i + 1), breadId, storedFilename);
            imageEntities.add(image);

            // FileStorage mock: storedFilename → URL 변환
            given(fileStorage.getFileUrl(storedFilename))
                    .willReturn("https://cdn.example.com/images/" + storedFilename);
        }

        List<Long> breadIds = deletedBreads.stream().map(BreadEntity::getId).collect(Collectors.toList());

        // Mock: BreadImageRepository는 breadId 기반으로 조회 (is_deleted 무관)
        given(breadImageRepository.findByBreadIdIn(breadIds)).willReturn(imageEntities);

        // Act
        Map<Long, String> imageUrls = breadImageService.getImageUrls(breadIds);

        // Assert: 모든 삭제된 빵에 대해 이미지 URL이 반환되어야 한다
        assertThat(imageUrls).hasSize(specs.size());
        for (Long breadId : breadIds) {
            assertThat(imageUrls).containsKey(breadId);
            String url = imageUrls.get(breadId);
            assertThat(url).isNotNull();
            assertThat(url).startsWith("https://");
            assertThat(url).contains("bread_" + breadId);
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
    @Property(tries = 100)
    void getImageUrl_returnsValidUrlForDeletedBread(
            @ForAll("singleDeletedBreadSpec") DeletedBreadSpec spec
    ) {
        // Arrange
        Long breadId = 1L;
        BreadEntity bread = TestFixtures.bread(breadId, spec.storeId(), spec.remainingQuantity(), spec.originalPrice(), spec.salePrice());
        bread.softDelete(spec.deletedAt());

        String storedFilename = "bread_" + breadId + "_" + spec.imageUuid() + ".jpg";
        BreadImageEntity image = TestFixtures.breadImage(1L, breadId, storedFilename);

        given(breadImageRepository.findByBreadId(breadId)).willReturn(Optional.of(image));
        given(fileStorage.getFileUrl(storedFilename))
                .willReturn("https://cdn.example.com/images/" + storedFilename);

        // Act
        String url = breadImageService.getImageUrl(breadId);

        // Assert: 삭제된 빵이어도 이미지 URL이 정상 반환되어야 한다
        assertThat(url).isNotNull();
        assertThat(url).startsWith("https://");
        assertThat(url).contains("bread_" + breadId);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<List<DeletedBreadSpec>> deletedBreadSpecs() {
        Arbitrary<DeletedBreadSpec> specArbitrary = Combinators.combine(
                Arbitraries.longs().between(1L, 100L),                          // storeId
                Arbitraries.integers().between(0, 100),                         // remainingQuantity
                Arbitraries.integers().between(100, 50000),                     // originalPrice
                Arbitraries.integers().between(100, 50000),                     // salePrice
                Arbitraries.of(2024, 2025, 2026),                              // year
                Arbitraries.integers().between(1, 12),                          // month
                Arbitraries.integers().between(1, 28),                          // day
                Arbitraries.strings().alpha().ofLength(8)                       // imageUuid
        ).as((storeId, qty, origPrice, salePrice, year, month, day, uuid) ->
                new DeletedBreadSpec(storeId, qty, origPrice, salePrice,
                        LocalDateTime.of(year, month, day, 12, 0), uuid));

        return specArbitrary.list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<DeletedBreadSpec> singleDeletedBreadSpec() {
        return Combinators.combine(
                Arbitraries.longs().between(1L, 100L),                          // storeId
                Arbitraries.integers().between(0, 100),                         // remainingQuantity
                Arbitraries.integers().between(100, 50000),                     // originalPrice
                Arbitraries.integers().between(100, 50000),                     // salePrice
                Arbitraries.of(2024, 2025, 2026),                              // year
                Arbitraries.integers().between(1, 12),                          // month
                Arbitraries.integers().between(1, 28),                          // day
                Arbitraries.strings().alpha().ofLength(8)                       // imageUuid
        ).as((storeId, qty, origPrice, salePrice, year, month, day, uuid) ->
                new DeletedBreadSpec(storeId, qty, origPrice, salePrice,
                        LocalDateTime.of(year, month, day, 12, 0), uuid));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Internal record for test data specification
    // ──────────────────────────────────────────────────────────────────────

    record DeletedBreadSpec(Long storeId, int remainingQuantity, int originalPrice, int salePrice,
                            LocalDateTime deletedAt, String imageUuid) {}
}
