package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.bread.service.BreadService;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.entity.StoreImageEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreDistanceProjection;
import com.todaybread.server.domain.store.repository.StoreImageRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.storage.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * StoreService.getNearbyStores() 단위 테스트입니다.
 * Mockito 기반으로 비즈니스 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class StoreServiceNearbyTest {

    @InjectMocks
    private StoreService storeService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreBusinessHoursRepository storeBusinessHoursRepository;

    @Mock
    private StoreImageRepository storeImageRepository;

    @Mock
    private StoreImageService storeImageService;

    @Mock
    private BreadRepository breadRepository;

    @Mock
    private BreadService breadService;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private Clock clock;

    // ========== 공통 테스트 픽스처 ==========

    /** 고정 시간: 2024-01-08 월요일 10:00 KST */
    private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
    private static final Instant FIXED_INSTANT =
            java.time.LocalDateTime.of(2024, 1, 8, 10, 0)
                    .atZone(ZONE_KST).toInstant();

    // 샘플 가게 엔티티
    private StoreEntity store1;
    private StoreEntity store2;

    // 샘플 영업시간 (월요일, dayOfWeek=1)
    private StoreBusinessHoursEntity hours1;
    private StoreBusinessHoursEntity hours2;

    // 샘플 빵 엔티티
    private BreadEntity bread1;
    private BreadEntity bread2;

    // 샘플 가게 이미지
    private StoreImageEntity image1;

    // 샘플 StoreDistanceProjection (mock)
    private StoreDistanceProjection projection1;
    private StoreDistanceProjection projection2;

    // 유저 좌표 (서울 강남역 부근)
    private final BigDecimal userLat = new BigDecimal("37.4979");
    private final BigDecimal userLng = new BigDecimal("127.0276");

    /**
     * 리플렉션으로 엔티티의 id 필드를 설정합니다.
     * 테스트에서 persist 없이 ID를 부여하기 위해 사용합니다.
     */
    private static void setEntityId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set entity id via reflection", e);
        }
    }

    @BeforeEach
    void setUp() {
        // 고정 Clock 설정 — 2024-01-08 월요일 10:00 KST
        // lenient: 모든 테스트에서 사용되지 않을 수 있으나, getNearbyStores 호출 시 필요
        org.mockito.Mockito.lenient().when(clock.instant()).thenReturn(FIXED_INSTANT);
        org.mockito.Mockito.lenient().when(clock.getZone()).thenReturn(ZONE_KST);

        // 가게 1: 강남역 근처 (약 0.5km)
        store1 = StoreEntity.builder()
                .userId(100L)
                .name("강남빵집")
                .phoneNumber("02-1111-1111")
                .description("맛있는 빵집")
                .addressLine1("서울시 강남구")
                .addressLine2("역삼동 100")
                .latitude(new BigDecimal("37.5000"))
                .longitude(new BigDecimal("127.0300"))
                .build();
        setEntityId(store1, 1L);

        // 가게 2: 강남역 근처 (약 0.8km)
        store2 = StoreEntity.builder()
                .userId(200L)
                .name("서초빵집")
                .phoneNumber("02-2222-2222")
                .description("신선한 빵집")
                .addressLine1("서울시 서초구")
                .addressLine2("서초동 200")
                .latitude(new BigDecimal("37.4950"))
                .longitude(new BigDecimal("127.0250"))
                .build();
        setEntityId(store2, 2L);

        // 영업시간: 월요일(1), 09:00~22:00, lastOrder 21:30
        hours1 = StoreBusinessHoursEntity.builder()
                .storeId(store1.getId())
                .dayOfWeek(1)
                .isClosed(false)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(22, 0))
                .lastOrderTime(LocalTime.of(21, 30))
                .build();

        hours2 = StoreBusinessHoursEntity.builder()
                .storeId(store2.getId())
                .dayOfWeek(1)
                .isClosed(false)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(20, 0))
                .lastOrderTime(null)
                .build();

        // 빵 엔티티 (재고 있음)
        bread1 = BreadEntity.builder()
                .storeId(store1.getId())
                .name("소금빵")
                .description("바삭한 소금빵")
                .originalPrice(3000)
                .salePrice(2500)
                .remainingQuantity(10)
                .build();

        bread2 = BreadEntity.builder()
                .storeId(store2.getId())
                .name("크로와상")
                .description("버터 크로와상")
                .originalPrice(4000)
                .salePrice(3500)
                .remainingQuantity(5)
                .build();

        // 가게 이미지 (displayOrder=0 → 대표 이미지)
        image1 = StoreImageEntity.builder()
                .storeId(store1.getId())
                .originalFilename("bread.jpg")
                .storedFilename("store_1_uuid.jpg")
                .displayOrder(0)
                .build();

        // StoreDistanceProjection mock (lenient: 모든 테스트에서 사용되지 않을 수 있음)
        projection1 = org.mockito.Mockito.mock(StoreDistanceProjection.class);
        org.mockito.Mockito.lenient().when(projection1.getStoreId()).thenReturn(store1.getId());
        org.mockito.Mockito.lenient().when(projection1.getDistance()).thenReturn(0.5);

        projection2 = org.mockito.Mockito.mock(StoreDistanceProjection.class);
        org.mockito.Mockito.lenient().when(projection2.getStoreId()).thenReturn(store2.getId());
        org.mockito.Mockito.lenient().when(projection2.getDistance()).thenReturn(0.8);
    }

    // 테스트 메서드는 5.2, 5.3 태스크에서 추가됩니다.

    @Nested
    @DisplayName("getNearbyStores - 기본 설정 검증")
    class SetupVerification {

        @Test
        @DisplayName("테스트 픽스처가 올바르게 초기화됨")
        void fixturesInitialized() {
            // 픽스처 초기화 검증
            assertThat(store1.getName()).isEqualTo("강남빵집");
            assertThat(store2.getName()).isEqualTo("서초빵집");
            assertThat(hours1.getDayOfWeek()).isEqualTo(1);
            assertThat(bread1.getRemainingQuantity()).isEqualTo(10);
            assertThat(image1.getDisplayOrder()).isEqualTo(0);
            assertThat(projection1.getDistance()).isEqualTo(0.5);
            assertThat(projection2.getDistance()).isEqualTo(0.8);
        }
    }

    @Nested
    @DisplayName("getNearbyStores - 정상 흐름")
    class NormalFlow {

        @Test
        @DisplayName("반경 내 가게 2개 반환, 거리순 정렬, 필드 매핑 검증")
        void getNearbyStores_success() {
            // given
            given(storeRepository.findActiveStoresWithinRadius(
                    eq(userLat), eq(userLng), eq(1),
                    any(BigDecimal.class), any(BigDecimal.class),
                    any(BigDecimal.class), any(BigDecimal.class)))
                    .willReturn(List.of(projection1, projection2));

            given(storeRepository.findByIdInAndIsActiveTrue(List.of(1L, 2L)))
                    .willReturn(List.of(store1, store2));

            given(storeBusinessHoursRepository.findByStoreIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(hours1, hours2));

            given(breadRepository.findByStoreIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(bread1, bread2));

            given(storeImageRepository.findByStoreIdInOrderByStoreIdAscDisplayOrderAsc(List.of(1L, 2L)))
                    .willReturn(List.of(image1));

            given(fileStorage.getFileUrl("store_1_uuid.jpg"))
                    .willReturn("https://cdn.example.com/store_1_uuid.jpg");

            // when
            var result = storeService.getNearbyStores(userLat, userLng, 1);

            // then
            assertThat(result).hasSize(2);

            // 거리순 오름차순 정렬 검증 (0.5 → 0.8)
            assertThat(result.get(0).distance()).isEqualTo(0.5);
            assertThat(result.get(1).distance()).isEqualTo(0.8);

            // store1 필드 매핑 검증
            var resp1 = result.get(0);
            assertThat(resp1.storeId()).isEqualTo(1L);
            assertThat(resp1.name()).isEqualTo("강남빵집");
            assertThat(resp1.latitude()).isEqualByComparingTo(new BigDecimal("37.5000"));
            assertThat(resp1.longitude()).isEqualByComparingTo(new BigDecimal("127.0300"));
            assertThat(resp1.isSelling()).isTrue();
            assertThat(resp1.primaryImageUrl()).isEqualTo("https://cdn.example.com/store_1_uuid.jpg");
            assertThat(resp1.lastOrderTime()).isEqualTo(LocalTime.of(21, 30));

            // store2 필드 매핑 검증
            var resp2 = result.get(1);
            assertThat(resp2.storeId()).isEqualTo(2L);
            assertThat(resp2.name()).isEqualTo("서초빵집");
            assertThat(resp2.latitude()).isEqualByComparingTo(new BigDecimal("37.4950"));
            assertThat(resp2.longitude()).isEqualByComparingTo(new BigDecimal("127.0250"));
            assertThat(resp2.isSelling()).isTrue();
            assertThat(resp2.primaryImageUrl()).isNull();
            assertThat(resp2.lastOrderTime()).isNull();
        }

        @Test
        @DisplayName("반경 내 가게 없으면 빈 리스트 반환")
        void getNearbyStores_emptyResult() {
            // given
            given(storeRepository.findActiveStoresWithinRadius(
                    eq(userLat), eq(userLng), eq(1),
                    any(BigDecimal.class), any(BigDecimal.class),
                    any(BigDecimal.class), any(BigDecimal.class)))
                    .willReturn(Collections.emptyList());

            // when
            var result = storeService.getNearbyStores(userLat, userLng, 1);

            // then
            assertThat(result).isEmpty();

            // 다른 리포지터리 메서드가 호출되지 않았는지 검증
            verifyNoInteractions(storeBusinessHoursRepository);
            verifyNoInteractions(breadRepository);
            verifyNoInteractions(storeImageRepository);
        }
    }

    @Nested
    @DisplayName("getNearbyStores - 엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("빵이 없는 가게는 isSelling=false")
        void getNearbyStores_noBreads_isSellingFalse() {
            // given — store1만 반경 내, 빵 없음
            given(storeRepository.findActiveStoresWithinRadius(
                    eq(userLat), eq(userLng), eq(1),
                    any(BigDecimal.class), any(BigDecimal.class),
                    any(BigDecimal.class), any(BigDecimal.class)))
                    .willReturn(List.of(projection1));

            given(storeRepository.findByIdInAndIsActiveTrue(List.of(1L)))
                    .willReturn(List.of(store1));

            given(storeBusinessHoursRepository.findByStoreIdIn(List.of(1L)))
                    .willReturn(List.of(hours1));

            // 빵 없음 → hasStock=false → isSelling=false
            given(breadRepository.findByStoreIdIn(List.of(1L)))
                    .willReturn(Collections.emptyList());

            given(storeImageRepository.findByStoreIdInOrderByStoreIdAscDisplayOrderAsc(List.of(1L)))
                    .willReturn(List.of(image1));

            given(fileStorage.getFileUrl("store_1_uuid.jpg"))
                    .willReturn("https://cdn.example.com/store_1_uuid.jpg");

            // when
            var result = storeService.getNearbyStores(userLat, userLng, 1);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isSelling()).isFalse();
        }

        @Test
        @DisplayName("이미지가 없는 가게는 primaryImageUrl=null")
        void getNearbyStores_noImages_primaryImageUrlNull() {
            // given — store1만 반경 내, 이미지 없음
            given(storeRepository.findActiveStoresWithinRadius(
                    eq(userLat), eq(userLng), eq(1),
                    any(BigDecimal.class), any(BigDecimal.class),
                    any(BigDecimal.class), any(BigDecimal.class)))
                    .willReturn(List.of(projection1));

            given(storeRepository.findByIdInAndIsActiveTrue(List.of(1L)))
                    .willReturn(List.of(store1));

            given(storeBusinessHoursRepository.findByStoreIdIn(List.of(1L)))
                    .willReturn(List.of(hours1));

            given(breadRepository.findByStoreIdIn(List.of(1L)))
                    .willReturn(List.of(bread1));

            // 이미지 없음 → primaryImageUrl=null
            given(storeImageRepository.findByStoreIdInOrderByStoreIdAscDisplayOrderAsc(List.of(1L)))
                    .willReturn(Collections.emptyList());

            // when
            var result = storeService.getNearbyStores(userLat, userLng, 1);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).primaryImageUrl()).isNull();
        }

        @Test
        @DisplayName("lastOrderTime 설정/미설정 시 올바른 매핑")
        void getNearbyStores_lastOrderTime_mapping() {
            // given — store1(lastOrderTime=21:30), store2(lastOrderTime=null)
            given(storeRepository.findActiveStoresWithinRadius(
                    eq(userLat), eq(userLng), eq(1),
                    any(BigDecimal.class), any(BigDecimal.class),
                    any(BigDecimal.class), any(BigDecimal.class)))
                    .willReturn(List.of(projection1, projection2));

            given(storeRepository.findByIdInAndIsActiveTrue(List.of(1L, 2L)))
                    .willReturn(List.of(store1, store2));

            given(storeBusinessHoursRepository.findByStoreIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(hours1, hours2));

            given(breadRepository.findByStoreIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(bread1, bread2));

            given(storeImageRepository.findByStoreIdInOrderByStoreIdAscDisplayOrderAsc(List.of(1L, 2L)))
                    .willReturn(List.of(image1));

            given(fileStorage.getFileUrl("store_1_uuid.jpg"))
                    .willReturn("https://cdn.example.com/store_1_uuid.jpg");

            // when
            var result = storeService.getNearbyStores(userLat, userLng, 1);

            // then
            assertThat(result).hasSize(2);

            // store1: lastOrderTime=21:30 (hours1에서 설정됨)
            var resp1 = result.stream()
                    .filter(r -> r.storeId().equals(1L))
                    .findFirst().orElseThrow();
            assertThat(resp1.lastOrderTime()).isEqualTo(LocalTime.of(21, 30));

            // store2: lastOrderTime=null (hours2에서 null)
            var resp2 = result.stream()
                    .filter(r -> r.storeId().equals(2L))
                    .findFirst().orElseThrow();
            assertThat(resp2.lastOrderTime()).isNull();
        }
    }

}
