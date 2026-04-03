package com.todaybread.server.domain.bread.service;

import com.todaybread.server.domain.bread.dto.BreadSortType;
import com.todaybread.server.domain.bread.dto.NearbyBreadResponse;
import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreDistanceProjection;
import com.todaybread.server.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

/**
 * {@link BreadService#getNearbyBreads}의 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class NearbyBreadServiceTest {

    @InjectMocks
    private BreadService breadService;

    @Mock
    private BreadRepository breadRepository;

    @Mock
    private BreadImageService breadImageService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreBusinessHoursRepository storeBusinessHoursRepository;

    @Mock
    private java.time.Clock clock;

    @BeforeEach
    void setUpClock() {
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-04-03T03:00:00Z"));
    }

    private StoreEntity createStore(Long storeId) {
        StoreEntity store = StoreEntity.builder()
                .userId(100L).name("가게" + storeId).phoneNumber("02-0000-" + String.format("%04d", storeId))
                .description("설명").addressLine1("서울").addressLine2("강남")
                .latitude(new BigDecimal("37.5")).longitude(new BigDecimal("127.0"))
                .build();
        ReflectionTestUtils.setField(store, "id", storeId);
        return store;
    }

    private BreadEntity createBread(Long breadId, Long storeId, int salePrice, int originalPrice, int qty) {
        BreadEntity bread = BreadEntity.builder()
                .storeId(storeId).name("빵" + breadId).description("설명")
                .originalPrice(originalPrice).salePrice(salePrice).remainingQuantity(qty).build();
        ReflectionTestUtils.setField(bread, "id", breadId);
        return bread;
    }

    private StoreBusinessHoursEntity createOpenHours(Long storeId, int dayOfWeek) {
        return StoreBusinessHoursEntity.builder()
                .storeId(storeId)
                .dayOfWeek(dayOfWeek)
                .isClosed(false)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .lastOrderTime(LocalTime.of(17, 30))
                .build();
    }

    private StoreDistanceProjection mockProjection(Long storeId, double distance) {
        return new StoreDistanceProjection() {
            @Override
            public Long getStoreId() {
                return storeId;
            }

            @Override
            public Double getDistance() {
                return distance;
            }
        };
    }

    @Nested
    @DisplayName("빈 결과")
    class EmptyResults {

        @Test
        @DisplayName("반경 내 가게가 없으면 빈 리스트")
        void noStores() {
            given(storeRepository.findActiveStoresWithinRadius(
                    any(), any(), anyInt(), any(), any(), any(), any()))
                    .willReturn(Collections.emptyList());

            List<NearbyBreadResponse> result = breadService.getNearbyBreads(
                    new BigDecimal("37.5"), new BigDecimal("127.0"), 1, BreadSortType.DISTANCE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("가게는 있지만 빵이 없으면 빈 리스트")
        void noBreads() {
            given(storeRepository.findActiveStoresWithinRadius(
                    any(), any(), anyInt(), any(), any(), any(), any()))
                    .willReturn(List.of(mockProjection(10L, 0.5)));
            given(storeRepository.findByIdInAndIsActiveTrue(List.of(10L)))
                    .willReturn(List.of(createStore(10L)));
            given(breadRepository.findByStoreIdIn(List.of(10L)))
                    .willReturn(Collections.emptyList());

            List<NearbyBreadResponse> result = breadService.getNearbyBreads(
                    new BigDecimal("37.5"), new BigDecimal("127.0"), 1, BreadSortType.DISTANCE);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("전체 빵 반환")
    class AllNearbyBreads {

        @Test
        @DisplayName("한 가게의 여러 빵을 모두 반환한다")
        void returnsAllBreadsFromStore() {
            given(storeRepository.findActiveStoresWithinRadius(
                    any(), any(), anyInt(), any(), any(), any(), any()))
                    .willReturn(List.of(mockProjection(10L, 0.5)));
            given(storeRepository.findByIdInAndIsActiveTrue(List.of(10L)))
                    .willReturn(List.of(createStore(10L)));

            BreadEntity first = createBread(1L, 10L, 5000, 6000, 10);
            BreadEntity second = createBread(2L, 10L, 2000, 3000, 5);
            BreadEntity soldOut = createBread(3L, 10L, 1000, 2000, 0);

            given(breadRepository.findByStoreIdIn(List.of(10L)))
                    .willReturn(List.of(first, second, soldOut));
            given(storeBusinessHoursRepository.findByStoreIdIn(List.of(10L)))
                    .willReturn(List.of(createOpenHours(10L, 5)));
            given(breadImageService.getImageUrls(List.of(1L, 2L, 3L)))
                    .willReturn(Map.of(2L, "/images/bread_2.jpg"));

            List<NearbyBreadResponse> result = breadService.getNearbyBreads(
                    new BigDecimal("37.5"), new BigDecimal("127.0"), 1, BreadSortType.DISTANCE);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(NearbyBreadResponse::id)
                    .containsExactly(1L, 2L, 3L);
            assertThat(result).extracting(NearbyBreadResponse::storeId)
                    .containsOnly(10L);
            assertThat(result).extracting(NearbyBreadResponse::isSelling)
                    .containsExactly(true, true, false);
            assertThat(result.get(1).imageUrl()).isEqualTo("/images/bread_2.jpg");
        }
    }

    @Nested
    @DisplayName("정렬")
    class Sorting {

        private void setupTwoStores() {
            given(storeRepository.findActiveStoresWithinRadius(
                    any(), any(), anyInt(), any(), any(), any(), any()))
                    .willReturn(List.of(mockProjection(10L, 2.0), mockProjection(20L, 0.5)));
            given(storeRepository.findByIdInAndIsActiveTrue(anyList()))
                    .willReturn(List.of(createStore(10L), createStore(20L)));

            BreadEntity bread1 = createBread(1L, 10L, 5000, 8000, 10);
            BreadEntity bread2 = createBread(2L, 20L, 2000, 3000, 5);
            BreadEntity bread3 = createBread(3L, 20L, 1500, 3000, 0);
            given(breadRepository.findByStoreIdIn(anyList()))
                    .willReturn(List.of(bread1, bread2, bread3));
            given(storeBusinessHoursRepository.findByStoreIdIn(anyList()))
                    .willReturn(List.of(createOpenHours(10L, 5), createOpenHours(20L, 5)));
            given(breadImageService.getImageUrls(anyList())).willReturn(Map.of());
        }

        @Test
        @DisplayName("DISTANCE는 가까운 가게의 빵부터 반환한다")
        void sortByDistance() {
            setupTwoStores();

            List<NearbyBreadResponse> result = breadService.getNearbyBreads(
                    new BigDecimal("37.5"), new BigDecimal("127.0"), 5, BreadSortType.DISTANCE);

            assertThat(result).hasSize(3);
            assertThat(result.subList(0, 2)).extracting(NearbyBreadResponse::storeId)
                    .containsOnly(20L);
            assertThat(result.get(2).storeId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("PRICE는 할인가 오름차순으로 정렬한다")
        void sortByPrice() {
            setupTwoStores();

            List<NearbyBreadResponse> result = breadService.getNearbyBreads(
                    new BigDecimal("37.5"), new BigDecimal("127.0"), 5, BreadSortType.PRICE);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(NearbyBreadResponse::salePrice)
                    .containsExactly(1500, 2000, 5000);
        }

        @Test
        @DisplayName("DISCOUNT는 할인율 내림차순으로 정렬한다")
        void sortByDiscount() {
            setupTwoStores();

            List<NearbyBreadResponse> result = breadService.getNearbyBreads(
                    new BigDecimal("37.5"), new BigDecimal("127.0"), 5, BreadSortType.DISCOUNT);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(NearbyBreadResponse::id)
                    .containsExactly(3L, 1L, 2L);
        }

        @Test
        @DisplayName("NONE은 랜덤 정렬 — 결과 개수만 검증")
        void sortByNone() {
            setupTwoStores();

            List<NearbyBreadResponse> result = breadService.getNearbyBreads(
                    new BigDecimal("37.5"), new BigDecimal("127.0"), 5, BreadSortType.NONE);

            assertThat(result).hasSize(3);
        }
    }
}
