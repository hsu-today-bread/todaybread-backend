package com.todaybread.server.domain.bread.service;

import com.todaybread.server.domain.bread.dto.BreadCommonRequest;
import com.todaybread.server.domain.bread.dto.BreadCommonResponse;
import com.todaybread.server.domain.bread.dto.BreadStockUpdateRequest;
import com.todaybread.server.domain.bread.dto.BreadSuccessResponse;
import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BreadServiceTest {

    @InjectMocks
    private BreadService breadService;

    @Mock
    private BreadRepository breadRepository;

    @Mock
    private BreadImageService breadImageService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private java.time.Clock clock;

    private StoreEntity createStoreEntity(Long userId, Long storeId) {
        StoreEntity storeEntity = StoreEntity.builder()
                .userId(userId)
                .name("테스트빵집")
                .phoneNumber("02-1234-5678")
                .description("맛있는 빵집")
                .addressLine1("서울시 강남구")
                .addressLine2("역삼동 123")
                .latitude(new BigDecimal("37.1234567"))
                .longitude(new BigDecimal("127.1234567"))
                .build();
        ReflectionTestUtils.setField(storeEntity, "id", storeId);
        return storeEntity;
    }

    private BreadEntity createBreadEntity(Long breadId, Long storeId) {
        BreadEntity breadEntity = BreadEntity.builder()
                .storeId(storeId)
                .name("소금빵")
                .description("겉바속촉")
                .originalPrice(5000)
                .salePrice(3500)
                .remainingQuantity(10)
                .build();
        ReflectionTestUtils.setField(breadEntity, "id", breadId);
        return breadEntity;
    }

    @Nested
    @DisplayName("updateBread")
    class UpdateBread {

        @Test
        @DisplayName("이미지 없이 수정하면 기존 이미지 URL과 함께 응답한다")
        void successWithoutImage() {
            StoreEntity storeEntity = createStoreEntity(1L, 10L);
            BreadEntity breadEntity = createBreadEntity(100L, 10L);
            BreadCommonRequest request = new BreadCommonRequest(
                    "크루아상", 6000, 4200, 5, "버터 풍미"
            );

            given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(storeEntity));
            given(breadRepository.findById(100L)).willReturn(Optional.of(breadEntity));
            given(breadImageService.getImageUrl(100L)).willReturn("/images/bread_100.jpg");

            BreadCommonResponse result = breadService.updateBread(1L, 100L, request, null);

            assertThat(result.id()).isEqualTo(100L);
            assertThat(result.name()).isEqualTo("크루아상");
            assertThat(result.salePrice()).isEqualTo(4200);
            assertThat(result.remainingQuantity()).isEqualTo(5);
            assertThat(result.imageUrl()).isEqualTo("/images/bread_100.jpg");
        }
    }

    @Nested
    @DisplayName("changeQuantity")
    class ChangeQuantity {

        @Test
        @DisplayName("품절 처리 시 재고를 0으로 바꾼다")
        void soldOut() {
            StoreEntity storeEntity = createStoreEntity(1L, 10L);
            BreadEntity breadEntity = createBreadEntity(100L, 10L);
            BreadStockUpdateRequest request = new BreadStockUpdateRequest(0);

            given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(storeEntity));
            given(breadRepository.findById(100L)).willReturn(Optional.of(breadEntity));

            BreadSuccessResponse result = breadService.changeQuantity(1L, 100L, request);

            assertThat(result.success()).isTrue();
            assertThat(breadEntity.getRemainingQuantity()).isZero();
        }

        @Test
        @DisplayName("재고를 지정한 수량으로 변경한다")
        void changeToSpecificQuantity() {
            StoreEntity storeEntity = createStoreEntity(1L, 10L);
            BreadEntity breadEntity = createBreadEntity(100L, 10L);
            BreadStockUpdateRequest request = new BreadStockUpdateRequest(25);

            given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(storeEntity));
            given(breadRepository.findById(100L)).willReturn(Optional.of(breadEntity));

            BreadSuccessResponse result = breadService.changeQuantity(1L, 100L, request);

            assertThat(result.success()).isTrue();
            assertThat(breadEntity.getRemainingQuantity()).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("deleteBread")
    class DeleteBread {

        @Test
        @DisplayName("삭제 시 이미지를 먼저 지우고 빵 엔티티를 삭제한다")
        void deletesImageBeforeBread() {
            StoreEntity storeEntity = createStoreEntity(1L, 10L);
            BreadEntity breadEntity = createBreadEntity(100L, 10L);

            given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(storeEntity));
            given(breadRepository.findById(100L)).willReturn(Optional.of(breadEntity));

            BreadSuccessResponse result = breadService.deleteBread(1L, 100L);

            InOrder inOrder = inOrder(breadImageService, breadRepository);
            inOrder.verify(breadImageService).deleteImage(100L);
            inOrder.verify(breadRepository).delete(breadEntity);
            assertThat(result.success()).isTrue();
        }
    }

    @Nested
    @DisplayName("getBreadsFromStore")
    class GetBreadsFromStore {

        @Test
        @DisplayName("조회 응답에 각 빵의 imageUrl을 채운다")
        void includesImageUrl() {
            StoreEntity storeEntity = createStoreEntity(1L, 10L);
            BreadEntity firstBread = createBreadEntity(100L, 10L);
            BreadEntity secondBread = createBreadEntity(101L, 10L);

            given(storeRepository.findByIdAndIsActiveTrue(10L)).willReturn(Optional.of(storeEntity));
            given(breadRepository.findByStoreId(10L)).willReturn(List.of(firstBread, secondBread));
            given(breadImageService.getImageUrls(List.of(100L, 101L)))
                    .willReturn(java.util.Map.of(100L, "/images/bread_100.jpg"));

            List<BreadCommonResponse> result = breadService.getBreadsFromStore(10L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).imageUrl()).isEqualTo("/images/bread_100.jpg");
            assertThat(result.get(1).imageUrl()).isNull();
            verify(breadImageService).getImageUrls(List.of(100L, 101L));
        }
    }
}
