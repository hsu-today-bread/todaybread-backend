package com.todaybread.server.global.storage;

import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3FileStorageTest {

    private static final String BUCKET = "todaybread-demo-images";
    private static final String REGION = "ap-northeast-2";

    @Mock
    private S3Client s3Client;

    @Test
    void store_uploadsFileToS3AndReturnsStoredFilename() {
        S3FileStorage storage = new S3FileStorage(s3Client, BUCKET, REGION);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bread.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().build());

        String storedFilename = storage.store(file, "bread", 42L);

        assertThat(storedFilename).matches("bread_42_[0-9a-f]{32}\\.jpg");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo(BUCKET);
        assertThat(request.key()).isEqualTo(storedFilename);
        assertThat(request.contentType()).isEqualTo("image/jpeg");
        assertThat(request.contentLength()).isEqualTo(file.getSize());
        assertThat(request.cacheControl()).isEqualTo("public, max-age=31536000");
    }

    @Test
    void store_usesDefaultContentTypeWhenMissing() {
        S3FileStorage storage = new S3FileStorage(s3Client, BUCKET, REGION);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "store.png",
                null,
                new byte[]{1}
        );
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().build());

        storage.store(file, "store", 1L);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void store_wrapsS3FailureAsStorageError() {
        S3FileStorage storage = new S3FileStorage(s3Client, BUCKET, REGION);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "review.webp",
                "image/webp",
                new byte[]{1}
        );
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(SdkClientException.create("s3 unavailable"));

        assertThatThrownBy(() -> storage.store(file, "review", 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMON_IMAGE_STORAGE_FAILED);
    }

    @Test
    void delete_deletesObjectFromS3() {
        S3FileStorage storage = new S3FileStorage(s3Client, BUCKET, REGION);

        storage.delete("store_1_abc.jpg");

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo(BUCKET);
        assertThat(request.key()).isEqualTo("store_1_abc.jpg");
    }

    @Test
    void delete_ignoresS3Failure() {
        S3FileStorage storage = new S3FileStorage(s3Client, BUCKET, REGION);
        given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .willThrow(SdkClientException.create("delete failed"));

        assertThatCode(() -> storage.delete("bread_1_abc.jpg"))
                .doesNotThrowAnyException();
    }

    @Test
    void getFileUrl_returnsPublicS3Url() {
        S3FileStorage storage = new S3FileStorage(s3Client, BUCKET, REGION);

        String url = storage.getFileUrl("store_1_abc.jpg");

        assertThat(url).isEqualTo("https://todaybread-demo-images.s3.ap-northeast-2.amazonaws.com/store_1_abc.jpg");
    }
}
