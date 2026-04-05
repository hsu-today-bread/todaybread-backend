package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.bread.dto.BreadCommonResponse;
import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.bread.service.BreadService;
import com.todaybread.server.domain.store.dto.BusinessHoursRequest;
import com.todaybread.server.domain.store.dto.NearbyStoreResponse;
import com.todaybread.server.domain.store.dto.StoreCommonRequest;
import com.todaybread.server.domain.store.dto.StoreCommonResponse;
import com.todaybread.server.domain.store.dto.StoreDetailResponse;
import com.todaybread.server.domain.store.dto.StoreImageResponse;
import com.todaybread.server.domain.store.dto.StoreInfoResponse;
import com.todaybread.server.domain.store.dto.StoreStatusResponse;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreImageRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.global.storage.FileStorage;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class StoreServiceTest {

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

    private StoreService storeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        storeService = new StoreService(
                storeRepository,
                storeBusinessHoursRepository,
                storeImageRepository,
                storeImageService,
                breadRepository,
                breadService,
                fileStorage,
                TestFixtures.FIXED_CLOCK
        );
    }

    @Test
    void getStoreStatus_reflectsRegistrationState() {
        given(storeRepository.existsByUserIdAndIsActiveTrue(1L)).willReturn(true);

        StoreStatusResponse response = storeService.getStoreStatus(1L);

        assertThat(response.hasStore()).isTrue();
    }

    @Test
    void getStoreInfo_returnsStoreAndImages() {
        StoreEntity store = TestFixtures.store(100L, 1L);
        List<StoreBusinessHoursEntity> hours = List.of(TestFixtures.businessHours(
                100L, 7, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)
        ));
        List<StoreImageResponse> images = List.of(new StoreImageResponse(1L, "https://cdn/store.jpg", 0));

        given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(store));
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(100L)).willReturn(hours);
        given(storeImageService.getImagesByStoreId(100L)).willReturn(images);

        StoreInfoResponse response = storeService.getStoreInfo(1L);

        assertThat(response.store().id()).isEqualTo(100L);
        assertThat(response.images()).hasSize(1);
    }

    @Test
    void getStoreDetail_returnsSellingStatus() {
        StoreEntity store = TestFixtures.store(100L, 1L);
        List<StoreBusinessHoursEntity> hours = List.of(TestFixtures.businessHours(
                100L, 7, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)
        ));
        List<StoreImageResponse> images = List.of(new StoreImageResponse(1L, "https://cdn/store.jpg", 0));
        List<BreadCommonResponse> breads = List.of(new BreadCommonResponse(10L, 100L, "bread", 4_000, 2_000, 3, "desc", null));

        given(storeRepository.findByIdAndIsActiveTrue(100L)).willReturn(Optional.of(store));
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(100L)).willReturn(hours);
        given(storeImageService.getImagesByStoreId(100L)).willReturn(images);
        given(breadService.getBreadsFromStore(100L)).willReturn(breads);

        StoreDetailResponse response = storeService.getStoreDetail(100L);

        assertThat(response.isSelling()).isTrue();
        assertThat(response.breads()).hasSize(1);
    }

    @Test
    void addStore_savesStoreBusinessHoursAndImages() {
        List<BusinessHoursRequest> businessHours = standardBusinessHours();
        List<MultipartFile> images = List.of(TestFixtures.imageFile("store.jpg"));
        StoreCommonRequest request = new StoreCommonRequest(
                "Store",
                "02-1234-5678",
                "desc",
                "addr1",
                "addr2",
                BigDecimal.valueOf(37.5),
                BigDecimal.valueOf(127.0),
                businessHours
        );
        List<StoreImageResponse> savedImages = List.of(new StoreImageResponse(1L, "https://cdn/store.jpg", 0));

        given(storeRepository.existsByUserIdAndIsActiveTrue(1L)).willReturn(false);
        given(storeRepository.existsByPhoneNumber("02-1234-5678")).willReturn(false);
        given(storeRepository.save(any(StoreEntity.class))).willAnswer(invocation -> {
            StoreEntity store = invocation.getArgument(0);
            ReflectionTestUtils.setField(store, "id", 100L);
            return store;
        });
        given(storeBusinessHoursRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(storeImageService.saveImages(100L, images)).willReturn(savedImages);

        StoreInfoResponse response = storeService.addStore(1L, request, images);

        assertThat(response.store().id()).isEqualTo(100L);
        assertThat(response.images()).hasSize(1);
    }

    @Test
    void addStore_rejectsDuplicatePhoneNumber() {
        StoreCommonRequest request = new StoreCommonRequest(
                "Store",
                "02-1234-5678",
                "desc",
                "addr1",
                "addr2",
                BigDecimal.valueOf(37.5),
                BigDecimal.valueOf(127.0),
                standardBusinessHours()
        );
        given(storeRepository.existsByUserIdAndIsActiveTrue(1L)).willReturn(false);
        given(storeRepository.existsByPhoneNumber("02-1234-5678")).willReturn(true);

        assertThatThrownBy(() -> storeService.addStore(1L, request, List.of(TestFixtures.imageFile("store.jpg"))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_PHONE_EXISTS);
    }

    @Test
    void updateStore_rejectsDuplicateDayOfWeek() {
        StoreEntity store = TestFixtures.store(100L, 1L);
        List<BusinessHoursRequest> duplicated = List.of(
                new BusinessHoursRequest(1, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)),
                new BusinessHoursRequest(1, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0))
        );
        StoreCommonRequest request = new StoreCommonRequest(
                "Store",
                store.getPhoneNumber(),
                "desc",
                "addr1",
                "addr2",
                BigDecimal.valueOf(37.5),
                BigDecimal.valueOf(127.0),
                duplicated
        );
        given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(store));

        assertThatThrownBy(() -> storeService.updateStore(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_DAY_OF_WEEK_DUPLICATE);
    }

    @Test
    void updateStore_updatesInfoAndBusinessHours() {
        StoreEntity store = TestFixtures.store(100L, 1L);
        List<BusinessHoursRequest> businessHours = standardBusinessHours();
        StoreCommonRequest request = new StoreCommonRequest(
                "New Store",
                store.getPhoneNumber(),
                "new desc",
                "new addr1",
                "new addr2",
                BigDecimal.valueOf(37.6),
                BigDecimal.valueOf(127.1),
                businessHours
        );

        given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(store));
        given(storeBusinessHoursRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        StoreCommonResponse response = storeService.updateStore(1L, request);

        assertThat(response.name()).isEqualTo("New Store");
        assertThat(store.getDescription()).isEqualTo("new desc");
        verify(storeBusinessHoursRepository).deleteByStoreId(100L);
    }

    @Test
    void getNearbyStores_returnsDistanceSortedResponses() {
        BigDecimal lat = BigDecimal.valueOf(37.5);
        BigDecimal lng = BigDecimal.valueOf(127.0);
        StoreEntity store1 = TestFixtures.store(100L, 1L);
        StoreEntity store2 = TestFixtures.store(200L, 2L);
        StoreBusinessHoursEntity hours1 = TestFixtures.businessHours(
                100L, 7, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)
        );
        StoreBusinessHoursEntity hours2 = TestFixtures.businessHours(
                200L, 7, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)
        );
        BreadEntity bread1 = TestFixtures.bread(10L, 100L, 3, 4_000, 2_000);
        BreadEntity bread2 = TestFixtures.bread(20L, 200L, 0, 4_000, 2_000);

        given(storeRepository.findActiveStoresWithinRadius(
                eq(lat), eq(lng), eq(1), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class)
        )).willReturn(List.of(
                TestFixtures.projection(100L, 1.5),
                TestFixtures.projection(200L, 0.5)
        ));
        given(storeRepository.findByIdInAndIsActiveTrue(List.of(100L, 200L))).willReturn(List.of(store1, store2));
        given(storeBusinessHoursRepository.findByStoreIdIn(List.of(100L, 200L))).willReturn(List.of(hours1, hours2));
        given(breadRepository.findByStoreIdIn(List.of(100L, 200L))).willReturn(List.of(bread1, bread2));
        given(storeImageRepository.findByStoreIdInOrderByStoreIdAscDisplayOrderAsc(List.of(100L, 200L)))
                .willReturn(List.of(TestFixtures.storeImage(1L, 100L, "store1.jpg", 0)));
        given(fileStorage.getFileUrl("store1.jpg")).willReturn("https://cdn/store1.jpg");

        List<NearbyStoreResponse> responses = storeService.getNearbyStores(lat, lng, 1);

        assertThat(responses).extracting(NearbyStoreResponse::storeId).containsExactly(200L, 100L);
        assertThat(responses.getFirst().isSelling()).isFalse();
        assertThat(responses.getLast().primaryImageUrl()).isEqualTo("https://cdn/store1.jpg");
    }

    private List<BusinessHoursRequest> standardBusinessHours() {
        return List.of(
                new BusinessHoursRequest(1, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)),
                new BusinessHoursRequest(2, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)),
                new BusinessHoursRequest(3, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)),
                new BusinessHoursRequest(4, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)),
                new BusinessHoursRequest(5, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)),
                new BusinessHoursRequest(6, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)),
                new BusinessHoursRequest(7, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0))
        );
    }
}
