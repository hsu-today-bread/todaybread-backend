package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.store.dto.StoreImageResponse;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.entity.StoreImageEntity;
import com.todaybread.server.domain.store.repository.StoreImageRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.global.storage.FileStorage;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StoreImageServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreImageRepository storeImageRepository;

    @Mock
    private FileStorage fileStorage;

    @InjectMocks
    private StoreImageService storeImageService;

    @BeforeEach
    void initSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void replaceImages_rejectsMissingStore() {
        given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> storeImageService.replaceImages(1L, List.of(TestFixtures.imageFile("store.jpg"))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    @Test
    void replaceImages_replacesExistingImagesAndDeletesOldFilesAfterCommit() {
        StoreEntity store = TestFixtures.store(100L, 1L);
        MultipartFile first = TestFixtures.imageFile("store-1.jpg");
        MultipartFile second = TestFixtures.imageFile("store-2.jpg");
        List<StoreImageEntity> existing = List.of(
                TestFixtures.storeImage(1L, 100L, "old-1.jpg", 0),
                TestFixtures.storeImage(2L, 100L, "old-2.jpg", 1)
        );
        List<StoreImageEntity> saved = List.of(
                TestFixtures.storeImage(10L, 100L, "new-1.jpg", 0),
                TestFixtures.storeImage(11L, 100L, "new-2.jpg", 1)
        );

        given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(store));
        given(storeImageRepository.findByStoreIdOrderByDisplayOrderAsc(100L)).willReturn(existing);
        given(fileStorage.store(first, "store", 100L)).willReturn("new-1.jpg");
        given(fileStorage.store(second, "store", 100L)).willReturn("new-2.jpg");
        given(storeImageRepository.saveAll(any())).willReturn(saved);
        given(fileStorage.getFileUrl("new-1.jpg")).willReturn("https://cdn/new-1.jpg");
        given(fileStorage.getFileUrl("new-2.jpg")).willReturn("https://cdn/new-2.jpg");

        List<StoreImageResponse> responses = storeImageService.replaceImages(1L, List.of(first, second));
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(StoreImageResponse::imageUrl)
                .containsExactly("https://cdn/new-1.jpg", "https://cdn/new-2.jpg");
        verify(storeImageRepository).deleteByStoreId(100L);
        verify(storeImageRepository).flush();
        verify(fileStorage).delete("old-1.jpg");
        verify(fileStorage).delete("old-2.jpg");
    }

    @Test
    void saveImages_cleansUpStoredFilesWhenPersistenceFails() {
        MultipartFile first = TestFixtures.imageFile("store-1.jpg");
        MultipartFile second = TestFixtures.imageFile("store-2.jpg");
        given(fileStorage.store(first, "store", 100L)).willReturn("new-1.jpg");
        given(fileStorage.store(second, "store", 100L)).willReturn("new-2.jpg");
        given(storeImageRepository.saveAll(any())).willThrow(new RuntimeException("db error"));

        assertThatThrownBy(() -> storeImageService.saveImages(100L, List.of(first, second)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMON_IMAGE_STORAGE_FAILED);

        verify(fileStorage).delete("new-1.jpg");
        verify(fileStorage).delete("new-2.jpg");
    }

    @Test
    void getImagesByStoreId_returnsMappedResponses() {
        given(storeImageRepository.findByStoreIdOrderByDisplayOrderAsc(100L)).willReturn(List.of(
                TestFixtures.storeImage(1L, 100L, "store-1.jpg", 0),
                TestFixtures.storeImage(2L, 100L, "store-2.jpg", 1)
        ));
        given(fileStorage.getFileUrl("store-1.jpg")).willReturn("https://cdn/store-1.jpg");
        given(fileStorage.getFileUrl("store-2.jpg")).willReturn("https://cdn/store-2.jpg");

        List<StoreImageResponse> responses = storeImageService.getImagesByStoreId(100L);

        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().displayOrder()).isEqualTo(0);
        assertThat(responses.getLast().displayOrder()).isEqualTo(1);
    }
}
