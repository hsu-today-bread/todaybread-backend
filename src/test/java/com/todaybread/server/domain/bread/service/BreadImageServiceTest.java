package com.todaybread.server.domain.bread.service;

import com.todaybread.server.domain.bread.entity.BreadImageEntity;
import com.todaybread.server.domain.bread.repository.BreadImageRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BreadImageServiceTest {

    @Mock
    private FileStorage fileStorage;

    @Mock
    private BreadImageRepository breadImageRepository;

    @InjectMocks
    private BreadImageService breadImageService;

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
    void uploadImage_storesAndPersistsImage() {
        MultipartFile file = TestFixtures.imageFile("bread.jpg");
        given(fileStorage.store(file, "bread", 1L)).willReturn("bread-1.jpg");
        given(breadImageRepository.findByBreadId(1L)).willReturn(Optional.empty());
        given(fileStorage.getFileUrl("bread-1.jpg")).willReturn("https://cdn/bread-1.jpg");

        String imageUrl = breadImageService.uploadImage(1L, file);

        assertThat(imageUrl).isEqualTo("https://cdn/bread-1.jpg");
        verify(breadImageRepository).save(any(BreadImageEntity.class));
    }

    @Test
    void uploadImage_deletesOldFileAfterCommitWhenReplacing() {
        MultipartFile file = TestFixtures.imageFile("bread.jpg");
        BreadImageEntity existing = TestFixtures.breadImage(1L, 1L, "old-bread.jpg");
        given(fileStorage.store(file, "bread", 1L)).willReturn("new-bread.jpg");
        given(breadImageRepository.findByBreadId(1L)).willReturn(Optional.of(existing));
        given(fileStorage.getFileUrl("new-bread.jpg")).willReturn("https://cdn/new-bread.jpg");

        breadImageService.uploadImage(1L, file);

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        verify(breadImageRepository).delete(existing);
        verify(breadImageRepository).flush();
        verify(fileStorage).delete("old-bread.jpg");
    }

    @Test
    void uploadImage_wrapsStorageFailure() {
        MultipartFile file = TestFixtures.imageFile("bread.jpg");
        given(fileStorage.store(file, "bread", 1L)).willThrow(new RuntimeException("disk full"));

        assertThatThrownBy(() -> breadImageService.uploadImage(1L, file))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMON_IMAGE_STORAGE_FAILED);
    }

    @Test
    void getImageUrls_returnsMapForBreadIds() {
        given(breadImageRepository.findByBreadIdIn(List.of(1L, 2L))).willReturn(List.of(
                TestFixtures.breadImage(10L, 1L, "a.jpg"),
                TestFixtures.breadImage(11L, 2L, "b.jpg")
        ));
        given(fileStorage.getFileUrl("a.jpg")).willReturn("https://cdn/a.jpg");
        given(fileStorage.getFileUrl("b.jpg")).willReturn("https://cdn/b.jpg");

        Map<Long, String> result = breadImageService.getImageUrls(List.of(1L, 2L));

        assertThat(result).containsEntry(1L, "https://cdn/a.jpg");
        assertThat(result).containsEntry(2L, "https://cdn/b.jpg");
    }

    @Test
    void deleteImage_deletesFileAfterCommit() {
        BreadImageEntity existing = TestFixtures.breadImage(1L, 1L, "old-bread.jpg");
        given(breadImageRepository.findByBreadId(1L)).willReturn(Optional.of(existing));

        breadImageService.deleteImage(1L);
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        verify(breadImageRepository).delete(existing);
        verify(fileStorage).delete("old-bread.jpg");
    }
}
