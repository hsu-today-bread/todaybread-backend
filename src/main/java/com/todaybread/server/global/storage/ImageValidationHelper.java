package com.todaybread.server.global.storage;

import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * 이미지 파일 검증을 위한 공통 헬퍼 클래스입니다.
 * StoreImageService와 BreadImageService에서 공통으로 사용합니다.
 */
public final class ImageValidationHelper {

    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/jpg", "image/gif"
    );
    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

    private ImageValidationHelper() {
    }

    /**
     * 단일 이미지 파일을 검증합니다.
     *
     * @param file 업로드된 파일
     * @param invalidTypeError 허용되지 않는 파일 형식 에러 코드
     * @param sizeExceededError 파일 크기 초과 에러 코드
     */
    public static void validateFile(MultipartFile file, ErrorCode invalidTypeError, ErrorCode sizeExceededError) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(invalidTypeError);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(sizeExceededError);
        }
    }

    /**
     * 이미지 파일 목록을 검증합니다.
     *
     * @param files 업로드된 파일 목록
     * @param maxCount 최대 파일 수
     * @param limitExceededError 파일 수 초과 에러 코드
     * @param invalidTypeError 허용되지 않는 파일 형식 에러 코드
     * @param sizeExceededError 파일 크기 초과 에러 코드
     */
    public static void validateFiles(List<MultipartFile> files, int maxCount,
                                     ErrorCode limitExceededError, ErrorCode invalidTypeError,
                                     ErrorCode sizeExceededError) {
        if (files == null || files.isEmpty()) {
            throw new CustomException(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED);
        }

        if (files.size() > maxCount) {
            throw new CustomException(limitExceededError);
        }

        for (MultipartFile file : files) {
            validateFile(file, invalidTypeError, sizeExceededError);
        }
    }
}
