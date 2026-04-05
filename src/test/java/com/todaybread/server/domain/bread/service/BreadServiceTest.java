package com.todaybread.server.domain.bread.service;

import com.todaybread.server.domain.bread.dto.BreadCommonRequest;
import com.todaybread.server.domain.bread.dto.BreadCommonResponse;
import com.todaybread.server.domain.bread.dto.BreadDetailResponse;
import com.todaybread.server.domain.bread.dto.BreadSortType;
import com.todaybread.server.domain.bread.dto.BreadStockUpdateRequest;
import com.todaybread.server.domain.bread.dto.BreadSuccessResponse;
import com.todaybread.server.domain.bread.dto.NearbyBreadResponse;
import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class BreadServiceTest {

    @Mock
    private BreadRepository breadRepository;

    @Mock
    private BreadImageService breadImageService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreBusinessHoursRepository storeBusinessHoursRepository;

    private BreadService breadService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        breadService = new BreadService(
                breadRepository,
                breadImageService,
                storeRepository,
                storeBusinessHoursRepository,
                TestFixtures.FIXED_CLOCK
        );
    }

    @Test
    void addBread_savesBreadAndUploadsImage() {
        StoreEntity store = TestFixtures.store(100L, 1L);
        MultipartFile image = TestFixtures.imageFile("bread.jpg");
        BreadCommonRequest request = new BreadCommonRequest("Sourdough", 4_000, 2_500, 3, "fresh");

        given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(store));
        given(breadRepository.save(any(BreadEntity.class))).willAnswer(invocation -> {
            BreadEntity bread = invocation.getArgument(0);
            ReflectionTestUtils.setField(bread, "id", 10L);
            return bread;
        });
        given(breadImageService.uploadImage(10L, image)).willReturn("https://cdn/bread.jpg");

        BreadCommonResponse response = breadService.addBread(1L, request, image);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.storeId()).isEqualTo(100L);
        assertThat(response.imageUrl()).isEqualTo("https://cdn/bread.jpg");
    }

    @Test
    void updateBread_rejectsBreadOwnedByAnotherStore() {
        StoreEntity ownerStore = TestFixtures.store(100L, 1L);
        BreadEntity otherStoreBread = TestFixtures.bread(10L, 200L, 3, 4_000, 2_000);
        given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(ownerStore));
        given(breadRepository.findById(10L)).willReturn(Optional.of(otherStoreBread));

        assertThatThrownBy(() -> breadService.updateBread(
                1L,
                10L,
                new BreadCommonRequest("new", 4_000, 2_000, 3, "fresh"),
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BREAD_ACCESS_DENIED);
    }

    @Test
    void changeQuantity_updatesOwnedBreadStock() {
        StoreEntity store = TestFixtures.store(100L, 1L);
        BreadEntity bread = TestFixtures.bread(10L, 100L, 3, 4_000, 2_000);
        given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(store));
        given(breadRepository.findById(10L)).willReturn(Optional.of(bread));

        BreadSuccessResponse response = breadService.changeQuantity(1L, 10L, new BreadStockUpdateRequest(7));

        assertThat(response.success()).isTrue();
        assertThat(bread.getRemainingQuantity()).isEqualTo(7);
    }

    @Test
    void deleteBread_deletesBreadAndImage() {
        StoreEntity store = TestFixtures.store(100L, 1L);
        BreadEntity bread = TestFixtures.bread(10L, 100L, 3, 4_000, 2_000);
        given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(store));
        given(breadRepository.findById(10L)).willReturn(Optional.of(bread));

        BreadSuccessResponse response = breadService.deleteBread(1L, 10L);

        assertThat(response.success()).isTrue();
        verify(breadImageService).deleteImage(10L);
        verify(breadRepository).delete(bread);
    }

    @Test
    void getBreadsFromStore_rejectsMissingStore() {
        given(storeRepository.findByIdAndIsActiveTrue(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> breadService.getBreadsFromStore(100L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    @Test
    void getBreadDetail_returnsSellingStatusAndImage() {
        BreadEntity bread = TestFixtures.bread(10L, 100L, 3, 4_000, 2_000);
        StoreEntity store = TestFixtures.store(100L, 1L);
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(
                100L, 7, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)
        );
        given(breadRepository.findById(10L)).willReturn(Optional.of(bread));
        given(storeRepository.findByIdAndIsActiveTrue(100L)).willReturn(Optional.of(store));
        given(breadImageService.getImageUrl(10L)).willReturn("https://cdn/bread.jpg");
        given(storeBusinessHoursRepository.findByStoreIdAndDayOfWeek(100L, 7)).willReturn(Optional.of(hours));

        BreadDetailResponse response = breadService.getBreadDetail(10L);

        assertThat(response.storeName()).isEqualTo(store.getName());
        assertThat(response.imageUrl()).isEqualTo("https://cdn/bread.jpg");
        assertThat(response.isSelling()).isTrue();
    }

    @Test
    void getNearbyBreads_filtersClosedStoresAndSortsByPrice() {
        BigDecimal lat = BigDecimal.valueOf(37.5);
        BigDecimal lng = BigDecimal.valueOf(127.0);
        StoreEntity store1 = TestFixtures.store(100L, 1L);
        StoreEntity store2 = TestFixtures.store(200L, 2L);
        StoreEntity store3 = TestFixtures.store(300L, 3L);
        BreadEntity bread1 = TestFixtures.bread(10L, 100L, 5, 4_000, 2_500);
        BreadEntity bread2 = TestFixtures.bread(20L, 200L, 5, 5_000, 1_500);
        BreadEntity bread3 = TestFixtures.bread(30L, 300L, 5, 6_000, 3_000);
        StoreBusinessHoursEntity open1 = TestFixtures.businessHours(
                100L, 7, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)
        );
        StoreBusinessHoursEntity open2 = TestFixtures.businessHours(
                200L, 7, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)
        );
        StoreBusinessHoursEntity closed3 = TestFixtures.businessHours(
                300L, 7, true, null, null, null
        );

        given(storeRepository.findActiveStoresWithinRadius(
                eq(lat), eq(lng), eq(1), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class)
        )).willReturn(List.of(
                TestFixtures.projection(100L, 1.0),
                TestFixtures.projection(200L, 0.7),
                TestFixtures.projection(300L, 0.5)
        ));
        given(storeRepository.findByIdInAndIsActiveTrue(List.of(100L, 200L, 300L)))
                .willReturn(List.of(store1, store2, store3));
        given(storeBusinessHoursRepository.findByStoreIdIn(List.of(100L, 200L, 300L)))
                .willReturn(List.of(open1, open2, closed3));
        given(breadRepository.findByStoreIdIn(List.of(100L, 200L, 300L)))
                .willReturn(List.of(bread1, bread2, bread3));
        given(breadImageService.getImageUrls(List.of(10L, 20L)))
                .willReturn(Map.of(10L, "https://cdn/bread1.jpg", 20L, "https://cdn/bread2.jpg"));

        List<NearbyBreadResponse> responses = breadService.getNearbyBreads(lat, lng, 1, BreadSortType.PRICE);

        assertThat(responses).extracting(NearbyBreadResponse::id).containsExactly(20L, 10L);
        assertThat(responses).extracting(NearbyBreadResponse::storeId).doesNotContain(300L);
    }
}
