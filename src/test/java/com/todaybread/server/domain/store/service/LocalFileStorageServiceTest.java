package com.todaybread.server.domain.store.service;

import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService service;

    @BeforeEach
    void setUp() {
        service = new LocalFileStorageService(tempDir.toString());
    }

    @Test
    void store_savesFileWithCorrectNamingConvention() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", "test-content".getBytes());

        String storedFilename = service.store(file, 5L, 1);

        assertEquals("store_5_1.jpg", storedFilename);
        assertTrue(Files.exists(tempDir.resolve("store_5_1.jpg")));
    }

    @Test
    void store_createsDirectoryIfNotExists() {
        Path nestedDir = tempDir.resolve("nested/uploads");
        LocalFileStorageService nestedService = new LocalFileStorageService(nestedDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.png", "image/png", "test-content".getBytes());

        String storedFilename = nestedService.store(file, 3L, 2);

        assertEquals("store_3_2.png", storedFilename);
        assertTrue(Files.exists(nestedDir.resolve("store_3_2.png")));
    }

    @Test
    void store_throwsCustomExceptionOnIOError() {
        // Use a path that cannot be written to (file as directory)
        LocalFileStorageService badService = new LocalFileStorageService("/dev/null/impossible");
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", "test-content".getBytes());

        CustomException ex = assertThrows(CustomException.class,
                () -> badService.store(file, 1L, 1));
        assertEquals(ErrorCode.STORE_IMAGE_STORAGE_FAILED, ex.getErrorCode());
    }

    @Test
    void delete_removesExistingFile() throws IOException {
        Path filePath = tempDir.resolve("store_1_1.jpg");
        Files.writeString(filePath, "content");
        assertTrue(Files.exists(filePath));

        service.delete("store_1_1.jpg");

        assertFalse(Files.exists(filePath));
    }

    @Test
    void delete_doesNotThrowWhenFileDoesNotExist() {
        assertDoesNotThrow(() -> service.delete("nonexistent_file.jpg"));
    }

    @Test
    void getFileUrl_returnsCorrectUrlPath() {
        String url = service.getFileUrl("store_5_1.jpg");

        assertEquals("/images/store_5_1.jpg", url);
    }
}
