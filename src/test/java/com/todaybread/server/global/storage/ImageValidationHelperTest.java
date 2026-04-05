package com.todaybread.server.global.storage;

import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageValidationHelperTest {

    @Test
    void validateFile_acceptsSupportedImage() {
        MockMultipartFile file = new MockMultipartFile("file", "bread.jpg", "image/jpeg", new byte[]{1, 2, 3});

        assertThatCode(() -> ImageValidationHelper.validateFile(
                file,
                ErrorCode.COMMON_IMAGE_INVALID_TYPE,
                ErrorCode.COMMON_FILE_SIZE_EXCEEDED
        )).doesNotThrowAnyException();
    }

    @Test
    void validateFile_rejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> ImageValidationHelper.validateFile(
                file,
                ErrorCode.COMMON_IMAGE_INVALID_TYPE,
                ErrorCode.COMMON_FILE_SIZE_EXCEEDED
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMON_IMAGE_INVALID_TYPE);
    }

    @Test
    void validateFiles_rejectsTooManyFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "bread.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> ImageValidationHelper.validateFiles(
                List.of(file, file, file),
                2,
                ErrorCode.STORE_IMAGE_LIMIT_EXCEEDED,
                ErrorCode.COMMON_IMAGE_INVALID_TYPE,
                ErrorCode.COMMON_FILE_SIZE_EXCEEDED
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_IMAGE_LIMIT_EXCEEDED);
    }
}
