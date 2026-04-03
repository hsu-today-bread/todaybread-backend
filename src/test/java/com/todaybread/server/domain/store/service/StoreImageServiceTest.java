package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.store.dto.StoreImageResponse;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.entity.StoreImageEntity;
import com.todaybread.server.domain.store.repository.StoreImageRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.global.storage.FileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StoreImageServiceTest {

    @InjectMocks
    private StoreImageService storeImageService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreImageRepository storeImageRepository;

    @Mock
    private FileStorage fileStorage;

    private StoreEntity createStoreEntity() {
        return StoreEntity.builder()
                .userId(1L)
                .name("테스트빵집").phoneNumber("02-1234-5678").description("설명")
                .addressLine1("주소1").addressLine2("주소2")
                .latitude(new BigDecimal("37.0")).longitude(new BigDecimal("127.0"))
                .build();
    }

    private MockMultipartFile createValidImage(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg", new byte[1024]);
    }

    // ========== replaceImages ==========

    @Nested
    @DisplayName("replaceImages")
    class ReplaceImages {

        @Test
        @DisplayName("5장 초과 — STORE_IMAGE_LIMIT_EXCEEDED")
        void exceedLimit() {
            StoreEntity store = createStoreEntity();
            given(storeRepository.findByUserIdAndIsActiveTrue(1L))
                    .willReturn(Optional.of(store));

            List<MultipartFile> files = List.of(
                    createValidImage("a"), createValidImage("b"), createValidImage("c"),
                    createValidImage("d"), createValidImage("e"), createValidImage("f")
            );

            assertThatThrownBy(() -> storeImageService.replaceImages(1L, files))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.STORE_IMAGE_LIMIT_EXCEEDED));
        }

        @Test
        @DisplayName("잘못된 파일 형식 — COMMON_IMAGE_INVALID_TYPE")
        void invalidType() {
            StoreEntity store = createStoreEntity();
            given(storeRepository.findByUserIdAndIsActiveTrue(1L))
                    .willReturn(Optional.of(store));

            MockMultipartFile badFile = new MockMultipartFile(
                    "file", "test.bmp", "image/bmp", new byte[1024]);

            assertThatThrownBy(() -> storeImageService.replaceImages(1L, List.of(badFile)))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.COMMON_IMAGE_INVALID_TYPE));
        }

        @Test
        @DisplayName("5MB 초과 — COMMON_FILE_SIZE_EXCEEDED")
        void exceedSize() {
            StoreEntity store = createStoreEntity();
            given(storeRepository.findByUserIdAndIsActiveTrue(1L))
                    .willReturn(Optional.of(store));

            byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
            MockMultipartFile largeFile = new MockMultipartFile(
                    "file", "large.jpg", "image/jpeg", largeContent);

            assertThatThrownBy(() -> storeImageService.replaceImages(1L, List.of(largeFile)))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.COMMON_FILE_SIZE_EXCEEDED));
        }
    }

    // ========== getImagesByStoreId ==========

    @Nested
    @DisplayName("getImagesByStoreId")
    class GetImagesByStoreId {

        @Test
        @DisplayName("displayOrder 오름차순 정렬")
        void orderedByDisplayOrder() {
            List<StoreImageEntity> entities = List.of(
                    StoreImageEntity.builder()
                            .storeId(1L).originalFilename("a.jpg")
                            .storedFilename("store_1_0.jpg")
                            .displayOrder(0).build(),
                    StoreImageEntity.builder()
                            .storeId(1L).originalFilename("b.jpg")
                            .storedFilename("store_1_1.jpg")
                            .displayOrder(1).build()
            );
            given(storeImageRepository.findByStoreIdOrderByDisplayOrderAsc(1L))
                    .willReturn(entities);
            given(fileStorage.getFileUrl("store_1_0.jpg")).willReturn("/images/store_1_0.jpg");
            given(fileStorage.getFileUrl("store_1_1.jpg")).willReturn("/images/store_1_1.jpg");

            List<StoreImageResponse> result = storeImageService.getImagesByStoreId(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).displayOrder()).isEqualTo(0);
            assertThat(result.get(1).displayOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("이미지 없으면 빈 목록")
        void emptyList() {
            given(storeImageRepository.findByStoreIdOrderByDisplayOrderAsc(1L))
                    .willReturn(List.of());

            List<StoreImageResponse> result = storeImageService.getImagesByStoreId(1L);

            assertThat(result).isEmpty();
        }
    }
}
