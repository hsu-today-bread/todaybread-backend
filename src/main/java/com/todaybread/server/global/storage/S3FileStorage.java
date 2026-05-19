package com.todaybread.server.global.storage;

import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * {@link FileStorage}의 S3 구현체입니다.
 * EC2 프로필에서 업로드된 이미지를 S3에 저장하고 public read URL을 반환합니다.
 */
@Service
@Profile("ec2")
public class S3FileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorage.class);
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final String CACHE_CONTROL = "public, max-age=31536000";

    private final S3Client s3Client;
    private final String bucket;
    private final String region;

    public S3FileStorage(S3Client s3Client,
                         @Value("${app.s3.bucket}") String bucket,
                         @Value("${app.s3.region}") String region) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.region = region;
    }

    @Override
    public String store(MultipartFile file, String domain, Long entityId) {
        String storedFilename = buildStoredFilename(file, domain, entityId);
        String contentType = resolveContentType(file);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storedFilename)
                .contentType(contentType)
                .contentLength(file.getSize())
                .cacheControl(CACHE_CONTROL)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return storedFilename;
        } catch (IOException | RuntimeException e) {
            log.error("S3 파일 저장 실패: bucket={}, key={}", bucket, storedFilename, e);
            throw new CustomException(ErrorCode.COMMON_IMAGE_STORAGE_FAILED);
        }
    }

    @Override
    public void delete(String storedFilename) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(storedFilename)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (RuntimeException e) {
            log.error("S3 파일 삭제 실패: bucket={}, key={}", bucket, storedFilename, e);
        }
    }

    @Override
    public String getFileUrl(String storedFilename) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + storedFilename;
    }

    private String buildStoredFilename(MultipartFile file, String domain, Long entityId) {
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return domain + "_" + entityId + "_" + uuid + "." + extension;
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType)) {
            return contentType;
        }
        return DEFAULT_CONTENT_TYPE;
    }
}
