package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.bread.service.BreadService;
import com.todaybread.server.domain.store.dto.*;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @InjectMocks
    private StoreService storeService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreBusinessHoursRepository storeBusinessHoursRepository;

    @Mock
    private StoreImageService storeImageService;

    @Mock
    private BreadService breadService;

    @Mock
    private java.time.Clock clock;

    /**
     * 7개의 유효한 BusinessHoursRequest (월~일)를 생성합니다.
     */
    private List<BusinessHoursRequest> createBusinessHoursList() {
        List<BusinessHoursRequest> list = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            list.add(new BusinessHoursRequest(
                    day, false,
                    LocalTime.of(9, 0),
                    LocalTime.of(22, 0),
                    LocalTime.of(21, 30)
            ));
        }
        return list;
    }

    /**
     * 7개의 StoreBusinessHoursEntity (월~일)를 생성합니다.
     */
    private List<StoreBusinessHoursEntity> createBusinessHoursEntities(Long storeId) {
        List<StoreBusinessHoursEntity> list = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            list.add(StoreBusinessHoursEntity.builder()
                    .storeId(storeId)
                    .dayOfWeek(day)
                    .isClosed(false)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(22, 0))
                    .lastOrderTime(LocalTime.of(21, 30))
                    .build());
        }
        return list;
    }

    private StoreCommonRequest createRequest() {
        return new StoreCommonRequest(
                "테스트빵집", "02-1234-5678", "맛있는 빵집",
                "서울시 강남구", "역삼동 123",
                new BigDecimal("37.1234567"), new BigDecimal("127.1234567"),
                createBusinessHoursList()
        );
    }

    private StoreEntity createStoreEntity(Long userId) {
        return StoreEntity.builder()
                .userId(userId)
                .name("테스트빵집")
                .phoneNumber("02-1234-5678")
                .description("맛있는 빵집")
                .addressLine1("서울시 강남구")
                .addressLine2("역삼동 123")
                .latitude(new BigDecimal("37.1234567"))
                .longitude(new BigDecimal("127.1234567"))
                .build();
    }

    // ========== getStoreStatus ==========

    @Nested
    @DisplayName("getStoreStatus")
    class GetStoreStatus {

        @Test
        @DisplayName("매장 미등록 시 hasStore=false")
        void noStore_returnsFalse() {
            given(storeRepository.existsByUserIdAndIsActiveTrue(1L))
                    .willReturn(false);

            StoreStatusResponse result = storeService.getStoreStatus(1L);

            assertThat(result.hasStore()).isFalse();
        }

        @Test
        @DisplayName("매장 등록 시 hasStore=true")
        void hasStore_returnsTrue() {
            given(storeRepository.existsByUserIdAndIsActiveTrue(1L))
                    .willReturn(true);

            StoreStatusResponse result = storeService.getStoreStatus(1L);

            assertThat(result.hasStore()).isTrue();
        }
    }

    // ========== getStoreInfo ==========

    @Nested
    @DisplayName("getStoreInfo")
    class GetStoreInfo {

        @Test
        @DisplayName("정상 조회 — 매장 정보 + 이미지 + 영업시간 반환")
        void success() {
            StoreEntity store = createStoreEntity(1L);
            given(storeRepository.findByUserIdAndIsActiveTrue(1L))
                    .willReturn(Optional.of(store));
            given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(any()))
                    .willReturn(createBusinessHoursEntities(store.getId()));
            given(storeImageService.getImagesByStoreId(any()))
                    .willReturn(List.of(
                            new StoreImageResponse(1L, "/images/store_1_0.jpg", 0),
                            new StoreImageResponse(2L, "/images/store_1_1.jpg", 1)
                    ));

            StoreInfoResponse result = storeService.getStoreInfo(1L);

            assertThat(result.store().name()).isEqualTo("테스트빵집");
            assertThat(result.store().businessHours()).hasSize(7);
            assertThat(result.images()).hasSize(2);
            assertThat(result.images().get(0).displayOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("매장 미등록 시 STORE_NOT_FOUND")
        void noStore_throwsNotFound() {
            given(storeRepository.findByUserIdAndIsActiveTrue(1L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.getStoreInfo(1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.STORE_NOT_FOUND));
        }
    }

    // ========== addStore ==========

    @Nested
    @DisplayName("addStore")
    class AddStore {

        @Test
        @DisplayName("정상 등록")
        void success() {
            given(storeRepository.existsByUserIdAndIsActiveTrue(1L)).willReturn(false);
            given(storeRepository.existsByPhoneNumber("02-1234-5678")).willReturn(false);
            given(storeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(storeBusinessHoursRepository.saveAll(any())).willAnswer(inv -> inv.getArgument(0));
            given(storeImageService.saveImages(any(), any())).willReturn(List.of());

            StoreInfoResponse result = storeService.addStore(1L, createRequest(),
                    List.of(new org.springframework.mock.web.MockMultipartFile(
                            "img", "test.jpg", "image/jpeg", new byte[1024])));

            assertThat(result.store().name()).isEqualTo("테스트빵집");
            assertThat(result.store().businessHours()).hasSize(7);
        }

        @Test
        @DisplayName("중복 매장 — STORE_ALREADY_EXISTS")
        void duplicateStore() {
            given(storeRepository.existsByUserIdAndIsActiveTrue(1L)).willReturn(true);

            assertThatThrownBy(() -> storeService.addStore(1L, createRequest(),
                    List.of(new org.springframework.mock.web.MockMultipartFile(
                            "img", "test.jpg", "image/jpeg", new byte[1024]))))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.STORE_ALREADY_EXISTS));
        }

        @Test
        @DisplayName("중복 전화번호 — STORE_PHONE_EXISTS")
        void duplicatePhone() {
            given(storeRepository.existsByUserIdAndIsActiveTrue(1L)).willReturn(false);
            given(storeRepository.existsByPhoneNumber("02-1234-5678")).willReturn(true);

            assertThatThrownBy(() -> storeService.addStore(1L, createRequest(),
                    List.of(new org.springframework.mock.web.MockMultipartFile(
                            "img", "test.jpg", "image/jpeg", new byte[1024]))))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.STORE_PHONE_EXISTS));
        }
    }

    // ========== updateStore ==========

    @Nested
    @DisplayName("updateStore")
    class UpdateStore {

        @Test
        @DisplayName("정상 수정")
        void success() {
            StoreEntity store = createStoreEntity(1L);
            given(storeRepository.findByUserIdAndIsActiveTrue(1L))
                    .willReturn(Optional.of(store));
            given(storeBusinessHoursRepository.saveAll(any())).willAnswer(inv -> inv.getArgument(0));

            List<BusinessHoursRequest> updatedHours = new ArrayList<>();
            for (int day = 1; day <= 7; day++) {
                updatedHours.add(new BusinessHoursRequest(
                        day, false,
                        LocalTime.of(10, 0),
                        LocalTime.of(23, 0),
                        LocalTime.of(22, 30)
                ));
            }

            StoreCommonRequest updateRequest = new StoreCommonRequest(
                    "수정빵집", "02-1234-5678", "수정된 설명",
                    "서울시 강남구", "역삼동 456",
                    new BigDecimal("37.1234567"), new BigDecimal("127.1234567"),
                    updatedHours
            );

            StoreCommonResponse result = storeService.updateStore(1L, updateRequest);

            assertThat(result.name()).isEqualTo("수정빵집");
            assertThat(result.businessHours()).hasSize(7);
            verify(storeBusinessHoursRepository).deleteByStoreId(any());
        }

        @Test
        @DisplayName("매장 미등록 — STORE_NOT_FOUND")
        void noStore() {
            given(storeRepository.findByUserIdAndIsActiveTrue(1L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.updateStore(1L, createRequest()))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.STORE_NOT_FOUND));
        }

        @Test
        @DisplayName("전화번호 중복 — STORE_PHONE_EXISTS")
        void duplicatePhone() {
            StoreEntity store = createStoreEntity(1L);
            given(storeRepository.findByUserIdAndIsActiveTrue(1L))
                    .willReturn(Optional.of(store));
            given(storeRepository.existsByPhoneNumber("02-9999-9999")).willReturn(true);

            List<BusinessHoursRequest> hours = createBusinessHoursList();
            StoreCommonRequest updateRequest = new StoreCommonRequest(
                    "테스트빵집", "02-9999-9999", "맛있는 빵집",
                    "서울시 강남구", "역삼동 123",
                    new BigDecimal("37.1234567"), new BigDecimal("127.1234567"),
                    hours
            );

            assertThatThrownBy(() -> storeService.updateStore(1L, updateRequest))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.STORE_PHONE_EXISTS));
        }
    }
}
