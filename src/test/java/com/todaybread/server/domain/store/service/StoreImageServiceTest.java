package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.store.dto.StoreImageResponse;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.entity.StoreImageEntity;
import com.todaybread.server.domain.store.repository.StoreImageRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreImageServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreImageRepository storeImageRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private StoreImageService storeImageService;

    private StoreEntity mockStore;

    @BeforeEach
    void setUp() {
        mockStore = StoreEntity.builder()
                .userId(1L)
                .name("테스트 가게")
                .phoneNumber("010-1234-5678")
                .description("설명")
                .addressLine1("주소1")
                .addressLine2("주소2")
                .latitude(new java.math.BigDecimal("37.1234567"))
                .longitude(new java.math.BigDecimal("127.1234567"))
                .endTime(java.sql.Time.valueOf("22:00:00"))
                .lastOrderTime(java.sql.Time.valueOf("21:30:00"))
                .orderTime("09:00~22:00")
                .build();
    }

    @Test
    void replaceImages_storeNotFound_throwsStoreNotFound() {
        when(storeRepository.findByUserIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());
        List<MultipartFile> files = List.of(createValidFile("test.jpg", "image/jpeg"));

        CustomException ex = assertThrows(CustomException.class,
                () -> storeImageService.replaceImages(99L, files));
        assertEquals(ErrorCode.STORE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void replaceImages_emptyFileList_throwsValidationFailed() {
        when(storeRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Optional.of(mockStore));

        CustomException ex = assertThrows(CustomException.class,
                () -> storeImageService.replaceImages(1L, Collections.emptyList()));
        assertEquals(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED, ex.getErrorCode());
    }

    @Test
    void replaceImages_nullFileList_throwsValidationFailed() {
        when(storeRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Optional.of(mockStore));

        CustomException ex = assertThrows(CustomException.class,
                () -> storeImageService.replaceImages(1L, null));
        assertEquals(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED, ex.getErrorCode());
    }

    @Test
    void replaceImages_exceedsMaxFileCount_throwsLimitExceeded() {
        when(storeRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Optional.of(mockStore));
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            files.add(createValidFile("img" + i + ".jpg", "image/jpeg"));
        }

        CustomException ex = assertThrows(CustomException.class,
                () -> storeImageService.replaceImages(1L, files));
        assertEquals(ErrorCode.STORE_IMAGE_LIMIT_EXCEEDED, ex.getErrorCode());
    }

    @Test
    void replaceImages_invalidContentType_throwsInvalidType() {
        when(storeRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Optional.of(mockStore));
        List<MultipartFile> files = List.of(
                createValidFile("doc.pdf", "application/pdf"));

        CustomException ex = assertThrows(CustomException.class,
                () -> storeImageService.replaceImages(1L, files));
        assertEquals(ErrorCode.STORE_IMAGE_INVALID_TYPE, ex.getErrorCode());
    }

    @Test
    void replaceImages_fileSizeExceeded_throwsSizeExceeded() {
        when(storeRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Optional.of(mockStore));
        byte[] largeContent = new byte[10 * 1024 * 1024 + 1]; // 10MB + 1 byte
        MockMultipartFile largeFile = new MockMultipartFile(
                "images", "large.jpg", "image/jpeg", largeContent);

        CustomException ex = assertThrows(CustomException.class,
                () -> storeImageService.replaceImages(1L, List.of(largeFile)));
        assertEquals(ErrorCode.STORE_IMAGE_SIZE_EXCEEDED, ex.getErrorCode());
    }

    @Test
    void replaceImages_emptyFile_throwsValidationFailed() {
        when(storeRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Optional.of(mockStore));
        MockMultipartFile emptyFile = new MockMultipartFile(
                "images", "empty.jpg", "image/jpeg", new byte[0]);

        CustomException ex = assertThrows(CustomException.class,
                () -> storeImageService.replaceImages(1L, List.of(emptyFile)));
        assertEquals(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED, ex.getErrorCode());
    }

    @Test
    void replaceImages_success_deletesExistingAndSavesNew() {
        when(storeRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Optional.of(mockStore));

        // Existing images
        StoreImageEntity existingImage = StoreImageEntity.builder()
                .storeId(mockStore.getId())
                .originalFilename("old.jpg")
                .storedFilename("store_1_1.jpg")
                .filePath("/images/store_1_1.jpg")
                .displayOrder(1)
                .build();
        when(storeImageRepository.findByStoreIdOrderByDisplayOrderAsc(mockStore.getId()))
                .thenReturn(List.of(existingImage));

        when(fileStorageService.store(any(), eq(mockStore.getId()), eq(1)))
                .thenReturn("store_1_1.jpg");
        when(fileStorageService.store(any(), eq(mockStore.getId()), eq(2)))
                .thenReturn("store_1_2.png");
        when(fileStorageService.getFileUrl("store_1_1.jpg")).thenReturn("/images/store_1_1.jpg");
        when(fileStorageService.getFileUrl("store_1_2.png")).thenReturn("/images/store_1_2.png");

        when(storeImageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<MultipartFile> files = List.of(
                createValidFile("photo1.jpg", "image/jpeg"),
                createValidFile("photo2.png", "image/png"));

        List<StoreImageResponse> result = storeImageService.replaceImages(1L, files);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).displayOrder());
        assertEquals(2, result.get(1).displayOrder());

        // Verify old images were deleted
        verify(fileStorageService).delete("store_1_1.jpg");
        verify(storeImageRepository).deleteByStoreId(mockStore.getId());
    }

    @Test
    void replaceImages_storageFailure_cleansUpAndThrows() {
        when(storeRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Optional.of(mockStore));
        when(storeImageRepository.findByStoreIdOrderByDisplayOrderAsc(mockStore.getId()))
                .thenReturn(Collections.emptyList());

        // First file stores successfully, second fails
        when(fileStorageService.store(any(), eq(mockStore.getId()), eq(1)))
                .thenReturn("store_1_1.jpg");
        when(fileStorageService.store(any(), eq(mockStore.getId()), eq(2)))
                .thenThrow(new CustomException(ErrorCode.STORE_IMAGE_STORAGE_FAILED));

        List<MultipartFile> files = List.of(
                createValidFile("photo1.jpg", "image/jpeg"),
                createValidFile("photo2.png", "image/png"));

        CustomException ex = assertThrows(CustomException.class,
                () -> storeImageService.replaceImages(1L, files));
        assertEquals(ErrorCode.STORE_IMAGE_STORAGE_FAILED, ex.getErrorCode());

        // Verify cleanup of already stored file
        verify(fileStorageService).delete("store_1_1.jpg");
    }

    @Test
    void replaceImages_existingFileDeleteFails_continuesWithNewImages() {
        when(storeRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Optional.of(mockStore));

        StoreImageEntity existingImage = StoreImageEntity.builder()
                .storeId(mockStore.getId())
                .originalFilename("old.jpg")
                .storedFilename("store_1_1.jpg")
                .filePath("/images/store_1_1.jpg")
                .displayOrder(1)
                .build();
        when(storeImageRepository.findByStoreIdOrderByDisplayOrderAsc(mockStore.getId()))
                .thenReturn(List.of(existingImage));

        // File deletion throws exception
        doThrow(new RuntimeException("disk error")).when(fileStorageService).delete("store_1_1.jpg");

        when(fileStorageService.store(any(), eq(mockStore.getId()), eq(1)))
                .thenReturn("store_1_1.jpg");
        when(fileStorageService.getFileUrl("store_1_1.jpg")).thenReturn("/images/store_1_1.jpg");
        when(storeImageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<MultipartFile> files = List.of(createValidFile("new.jpg", "image/jpeg"));

        List<StoreImageResponse> result = storeImageService.replaceImages(1L, files);

        // Should succeed despite file deletion failure
        assertEquals(1, result.size());
        verify(storeImageRepository).deleteByStoreId(mockStore.getId());
    }

    @Test
    void getImagesByStoreId_noImages_returnsEmptyList() {
        when(storeImageRepository.findByStoreIdOrderByDisplayOrderAsc(1L))
                .thenReturn(Collections.emptyList());

        var result = storeImageService.getImagesByStoreId(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getImagesByStoreId_withImages_returnsSortedSummaries() {
        StoreImageEntity img1 = StoreImageEntity.builder()
                .storeId(1L)
                .originalFilename("photo1.jpg")
                .storedFilename("store_1_1.jpg")
                .filePath("/images/store_1_1.jpg")
                .displayOrder(1)
                .build();
        StoreImageEntity img2 = StoreImageEntity.builder()
                .storeId(1L)
                .originalFilename("photo2.png")
                .storedFilename("store_1_2.png")
                .filePath("/images/store_1_2.png")
                .displayOrder(2)
                .build();

        when(storeImageRepository.findByStoreIdOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(img1, img2));
        when(fileStorageService.getFileUrl("store_1_1.jpg")).thenReturn("/images/store_1_1.jpg");
        when(fileStorageService.getFileUrl("store_1_2.png")).thenReturn("/images/store_1_2.png");

        var result = storeImageService.getImagesByStoreId(1L);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).displayOrder());
        assertEquals("/images/store_1_1.jpg", result.get(0).imageUrl());
        assertEquals(2, result.get(1).displayOrder());
        assertEquals("/images/store_1_2.png", result.get(1).imageUrl());
    }

    private MockMultipartFile createValidFile(String filename, String contentType) {
        return new MockMultipartFile("images", filename, contentType, "test-image-content".getBytes());
    }
}
