package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.store.dto.FavouriteStoreResponse;
import com.todaybread.server.domain.store.dto.FavouriteStoreToggleRequest;
import com.todaybread.server.domain.store.dto.FavouriteStoreToggleResponse;
import com.todaybread.server.domain.store.entity.FavouriteStoreEntity;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.FavouriteStoreRepository;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreImageRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.global.storage.FileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link FavouriteStoreService}의 단위 테스트입니다.
 * 단골 가게 토글(추가/해제), 목록 조회, 판매중 판별 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class FavouriteStoreServiceTest {

    @InjectMocks
    private FavouriteStoreService favouriteStoreService;

    @Mock
    private FavouriteStoreRepository favouriteStoreRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private BreadRepository breadRepository;

    @Mock
    private StoreImageRepository storeImageRepository;

    @Mock
    private StoreBusinessHoursRepository storeBusinessHoursRepository;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private java.time.Clock clock;

    @Test
    @DisplayName("toggleFavouriteStore_추가_성공")
    void toggleFavouriteStore_add_success() {
        // given
        Long userId = 1L;
        Long storeId = 10L;
        FavouriteStoreToggleRequest request = new FavouriteStoreToggleRequest(storeId);

        when(favouriteStoreRepository.findByUserIdAndStoreId(userId, storeId))
                .thenReturn(Optional.empty());
        when(storeRepository.findByIdAndIsActiveTrue(storeId))
                .thenReturn(Optional.of(createStore(storeId)));
        when(favouriteStoreRepository.countByUserIdWithLock(userId)).thenReturn(0L);
        when(favouriteStoreRepository.save(any())).thenReturn(null);

        // when
        FavouriteStoreToggleResponse response = favouriteStoreService.toggleFavouriteStore(userId, request);

        // then
        assertThat(response.added()).isTrue();
        verify(favouriteStoreRepository).save(any(FavouriteStoreEntity.class));
    }

    @Test
    @DisplayName("toggleFavouriteStore_해제_성공")
    void toggleFavouriteStore_remove_success() {
        // given
        Long userId = 1L;
        Long storeId = 10L;
        FavouriteStoreToggleRequest request = new FavouriteStoreToggleRequest(storeId);
        FavouriteStoreEntity existing = FavouriteStoreEntity.builder()
                .userId(userId).storeId(storeId).build();

        when(favouriteStoreRepository.findByUserIdAndStoreId(userId, storeId))
                .thenReturn(Optional.of(existing));

        // when
        FavouriteStoreToggleResponse response = favouriteStoreService.toggleFavouriteStore(userId, request);

        // then
        assertThat(response.added()).isFalse();
        verify(favouriteStoreRepository).delete(existing);
    }

    @Test
    @DisplayName("toggleFavouriteStore_존재하지않는가게_에러")
    void toggleFavouriteStore_storeNotFound_error() {
        // given
        Long userId = 1L;
        Long storeId = 999L;
        FavouriteStoreToggleRequest request = new FavouriteStoreToggleRequest(storeId);

        when(favouriteStoreRepository.findByUserIdAndStoreId(userId, storeId))
                .thenReturn(Optional.empty());
        when(storeRepository.findByIdAndIsActiveTrue(storeId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> favouriteStoreService.toggleFavouriteStore(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    @Test
    @DisplayName("toggleFavouriteStore_5개초과_에러")
    void toggleFavouriteStore_limitExceeded_error() {
        // given
        Long userId = 1L;
        Long storeId = 10L;
        FavouriteStoreToggleRequest request = new FavouriteStoreToggleRequest(storeId);

        when(favouriteStoreRepository.findByUserIdAndStoreId(userId, storeId))
                .thenReturn(Optional.empty());
        when(storeRepository.findByIdAndIsActiveTrue(storeId))
                .thenReturn(Optional.of(createStore(storeId)));
        when(favouriteStoreRepository.countByUserIdWithLock(userId)).thenReturn(5L);

        // when & then
        assertThatThrownBy(() -> favouriteStoreService.toggleFavouriteStore(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FAVOURITE_STORE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("getMyFavouriteStores_빈목록")
    void getMyFavouriteStores_empty() {
        // given
        Long userId = 1L;
        when(favouriteStoreRepository.findByUserId(userId)).thenReturn(List.of());

        // when
        List<FavouriteStoreResponse> result = favouriteStoreService.getMyFavouriteStores(userId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getMyFavouriteStores_판매중판별_정확성")
    void getMyFavouriteStores_isSelling_accuracy() {
        // given
        Long userId = 1L;
        Long storeId = 10L;

        FavouriteStoreEntity favourite = FavouriteStoreEntity.builder()
                .userId(userId).storeId(storeId).build();

        StoreEntity store = createStore(storeId);

        // 오늘 요일에 해당하는 영업시간 엔티티 생성 (00:00~23:59 — 항상 영업시간 내)
        int todayDayOfWeek = LocalDate.now().getDayOfWeek().getValue();
        StoreBusinessHoursEntity todayHours = StoreBusinessHoursEntity.builder()
                .storeId(storeId)
                .dayOfWeek(todayDayOfWeek)
                .isClosed(false)
                .startTime(LocalTime.of(0, 0))
                .endTime(LocalTime.of(23, 59))
                .build();

        BreadEntity bread = BreadEntity.builder()
                .storeId(storeId).name("테스트빵").description("설명")
                .originalPrice(3000).salePrice(2000).remainingQuantity(5).build();

        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(clock.instant()).thenReturn(Instant.parse("2026-04-03T03:00:00Z"));
        when(favouriteStoreRepository.findByUserId(userId)).thenReturn(List.of(favourite));
        when(storeRepository.findByIdInAndIsActiveTrue(List.of(storeId))).thenReturn(List.of(store));
        when(breadRepository.findByStoreIdIn(List.of(storeId))).thenReturn(List.of(bread));
        when(storeBusinessHoursRepository.findByStoreIdIn(List.of(storeId))).thenReturn(List.of(todayHours));
        when(storeImageRepository.findByStoreIdInOrderByStoreIdAscDisplayOrderAsc(List.of(storeId)))
                .thenReturn(List.of());

        // when
        List<FavouriteStoreResponse> result = favouriteStoreService.getMyFavouriteStores(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).storeId()).isEqualTo(storeId);
        assertThat(result.get(0).isSelling()).isTrue();
    }

    private StoreEntity createStore(Long storeId) {
        StoreEntity store = StoreEntity.builder()
                .userId(100L)
                .name("테스트 가게")
                .phoneNumber("010-0000-0000")
                .description("설명")
                .addressLine1("서울시")
                .addressLine2("강남구")
                .latitude(BigDecimal.valueOf(37.5))
                .longitude(BigDecimal.valueOf(127.0))
                .build();
        ReflectionTestUtils.setField(store, "id", storeId);
        return store;
    }
}
