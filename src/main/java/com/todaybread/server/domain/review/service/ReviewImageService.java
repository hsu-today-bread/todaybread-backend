package com.todaybread.server.domain.review.service;

import com.todaybread.server.domain.review.entity.ReviewImageEntity;
import com.todaybread.server.domain.review.repository.ReviewImageRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.global.storage.FileStorage;
import com.todaybread.server.global.storage.ImageValidationHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 리뷰 이미지를 처리합니다.
 * 이미지는 리뷰당 최대 2장이며, 선택사항입니다.
 */
@Service
@RequiredArgsConstructor
public class ReviewImageService {

    private static final Logger log = LoggerFactory.getLogger(ReviewImageService.class);

    /** 리뷰당 최대 이미지 수 */
    private static final int MAX_IMAGE_COUNT = 2;

    private final FileStorage fileStorage;
    private final ReviewImageRepository reviewImageRepository;

    /**
     * 리뷰 이미지를 업로드합니다 (최대 2장).
     * 소유권 검증은 호출하는 서비스에서 처리합니다.
     *
     * @param reviewId 리뷰 ID
     * @param files    업로드할 이미지 파일 목록
     * @return 저장된 이미지 파일명 목록
     * @throws CustomException REVIEW_IMAGE_LIMIT_EXCEEDED — 3장 이상 첨부 시
     * @throws CustomException COMMON_IMAGE_INVALID_TYPE — 허용되지 않는 파일 형식
     * @throws CustomException COMMON_FILE_SIZE_EXCEEDED — 파일 크기 5MB 초과
     * @throws CustomException COMMON_IMAGE_STORAGE_FAILED — 파일 저장 실패
     */
    @Transactional
    public List<String> uploadImages(Long reviewId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 파일 검증 (개수 + 타입 + 크기)
        if (files.size() > MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED);
        }
        for (MultipartFile file : files) {
            ImageValidationHelper.validateFile(file,
                    ErrorCode.COMMON_IMAGE_INVALID_TYPE,
                    ErrorCode.COMMON_FILE_SIZE_EXCEEDED);
        }

        // 2. 파일 저장 및 엔티티 생성
        List<String> storedFilenames = new ArrayList<>();
        List<ReviewImageEntity> entities = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                String storedFilename = fileStorage.store(file, "review", reviewId);
                storedFilenames.add(storedFilename);

                ReviewImageEntity entity = ReviewImageEntity.builder()
                        .reviewId(reviewId)
                        .originalFilename(file.getOriginalFilename())
                        .storedFilename(storedFilename)
                        .build();
                entities.add(entity);
            }
            reviewImageRepository.saveAll(entities);
        } catch (Exception e) {
            // 저장 중 오류 시 이미 저장된 파일 정리
            cleanupStoredFiles(storedFilenames);
            if (e instanceof CustomException) {
                throw e;
            }
            log.error("리뷰 이미지 저장 실패: reviewId={}", reviewId, e);
            throw new CustomException(ErrorCode.COMMON_IMAGE_STORAGE_FAILED);
        }

        // 3. 롤백 시 새 파일 정리 (orphan file 방지)
        List<String> savedFilenames = new ArrayList<>(storedFilenames);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    for (String filename : savedFilenames) {
                        try {
                            fileStorage.delete(filename);
                            log.info("트랜잭션 롤백으로 리뷰 이미지 파일 정리: {}", filename);
                        } catch (Exception e) {
                            log.warn("롤백 후 리뷰 이미지 파일 정리 실패: {}", filename, e);
                        }
                    }
                }
            }
        });

        return storedFilenames;
    }

    /**
     * 리뷰 이미지 URL 목록을 조회합니다.
     *
     * @param reviewId 리뷰 ID
     * @return 이미지 URL 목록 (없으면 빈 목록)
     */
    @Transactional(readOnly = true)
    public List<String> getImageUrls(Long reviewId) {
        List<ReviewImageEntity> images = reviewImageRepository.findByReviewId(reviewId);
        List<String> urls = new ArrayList<>();
        for (ReviewImageEntity image : images) {
            urls.add(fileStorage.getFileUrl(image.getStoredFilename()));
        }
        return urls;
    }

    /**
     * 여러 리뷰의 이미지 URL을 한 번에 조회합니다.
     * N+1 쿼리 방지용입니다.
     *
     * @param reviewIds 리뷰 ID 목록
     * @return reviewId → imageUrl 목록 매핑 (이미지 없는 리뷰는 빈 목록)
     */
    @Transactional(readOnly = true)
    public Map<Long, List<String>> getImageUrlsByReviewIds(List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ReviewImageEntity> images = reviewImageRepository.findByReviewIdIn(reviewIds);

        Map<Long, List<String>> result = new HashMap<>();
        for (ReviewImageEntity image : images) {
            result.computeIfAbsent(image.getReviewId(), k -> new ArrayList<>())
                    .add(fileStorage.getFileUrl(image.getStoredFilename()));
        }
        return result;
    }

    /**
     * 저장 중 오류 발생 시 이미 저장된 파일을 정리합니다.
     */
    private void cleanupStoredFiles(List<String> storedFilenames) {
        for (String filename : storedFilenames) {
            try {
                fileStorage.delete(filename);
            } catch (Exception e) {
                log.warn("저장 실패 후 파일 정리 중 오류: {}", filename, e);
            }
        }
    }
}
