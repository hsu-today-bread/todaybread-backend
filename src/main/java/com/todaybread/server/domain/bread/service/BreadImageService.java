package com.todaybread.server.domain.bread.service;

import com.todaybread.server.domain.bread.entity.BreadImageEntity;
import com.todaybread.server.domain.bread.repository.BreadImageRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.global.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bread Image를 처리합니다.
 * 이미지는 Bread 당 최대 1장이며, 선택사항입니다.
 */
@Service
@RequiredArgsConstructor
public class BreadImageService {

    private static final Logger log = LoggerFactory.getLogger(BreadImageService.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/jpg", "image/gif"
    );
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

    private final FileStorage fileStorage;
    private final BreadImageRepository breadImageRepository;

    /**
     * 음식 이미지를 업로드합니다 (1장 교체 방식).
     * 기존 이미지가 있으면 삭제 후 새 이미지로 교체합니다.
     * 소유권 검증은 호출하는 서비스에서 처리합니다.
     *
     * @param breadId 음식 ID
     * @param file    업로드할 이미지 파일
     * @return 저장된 이미지 URL
     */
    @Transactional
    public String uploadImage(Long breadId, MultipartFile file) {
        // 1. 파일 검증
        validateFile(file);

        // 2. 새 이미지 먼저 저장 (파일시스템)
        String storedFilename;
        try {
            storedFilename = fileStorage.store(file, "bread", breadId);
        } catch (Exception e) {
            log.error("이미지 저장 실패: breadId={}", breadId, e);
            throw new CustomException(ErrorCode.BREAD_IMAGE_STORAGE_FAILED);
        }

        // 롤백 시 새 파일 정리 (orphan file 방지)
        String newStoredFilename = storedFilename;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        fileStorage.delete(newStoredFilename);
                        log.info("트랜잭션 롤백으로 새 이미지 파일 정리: {}", newStoredFilename);
                    } catch (Exception e) {
                        log.warn("롤백 후 새 이미지 파일 정리 실패: {}", newStoredFilename, e);
                    }
                }
            }
        });

        // 3. 기존 이미지 있으면 DB 삭제 + 커밋 후 old 파일 삭제 예약
        Optional<BreadImageEntity> existingImage = breadImageRepository.findByBreadId(breadId);
        if (existingImage.isPresent()) {
            BreadImageEntity existing = existingImage.get();
            String oldStoredFilename = existing.getStoredFilename();
            breadImageRepository.delete(existing);
            breadImageRepository.flush();

            // 커밋 후 old 파일 삭제 (롤백 시 old 파일 유지)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        fileStorage.delete(oldStoredFilename);
                    } catch (Exception e) {
                        log.warn("기존 이미지 파일 삭제 실패 (커밋 후): {}", oldStoredFilename, e);
                    }
                }
            });
        }

        // 4. 새 이미지 DB 저장
        BreadImageEntity entity = BreadImageEntity.builder()
                .breadId(breadId)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .build();
        breadImageRepository.save(entity);

        return fileStorage.getFileUrl(storedFilename);
    }

    /**
     * 음식 이미지 URL을 조회합니다.
     *
     * @param breadId 음식 ID
     * @return 이미지 URL (없으면 null)
     */
    @Transactional(readOnly = true)
    public String getImageUrl(Long breadId) {
        Optional<BreadImageEntity> imageOptional = breadImageRepository.findByBreadId(breadId);
        if (imageOptional.isEmpty()) {
            return null;
        }
        return fileStorage.getFileUrl(imageOptional.get().getStoredFilename());
    }

    /**
     * 여러 음식의 이미지 URL을 한 번에 조회합니다.
     * N+1 쿼리 방지용입니다.
     *
     * @param breadIds 음식 ID 목록
     * @return breadId → imageUrl 매핑 (이미지 없는 bread는 포함되지 않음)
     */
    @Transactional(readOnly = true)
    public Map<Long, String> getImageUrls(List<Long> breadIds) {
        if (breadIds == null || breadIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BreadImageEntity> images = breadImageRepository.findByBreadIdIn(breadIds);
        Map<Long, String> result = new HashMap<>();
        for (BreadImageEntity image : images) {
            result.put(image.getBreadId(), fileStorage.getFileUrl(image.getStoredFilename()));
        }
        return result;
    }

    /**
     * 음식 이미지를 삭제합니다 (파일 + DB).
     *
     * @param breadId 음식 ID
     */
    @Transactional
    public void deleteImage(Long breadId) {
        Optional<BreadImageEntity> imageOptional = breadImageRepository.findByBreadId(breadId);
        if (imageOptional.isPresent()) {
            BreadImageEntity image = imageOptional.get();
            String oldStoredFilename = image.getStoredFilename();
            breadImageRepository.delete(image);

            // 커밋 후 파일 삭제 (롤백 시 파일 유지)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        fileStorage.delete(oldStoredFilename);
                    } catch (Exception e) {
                        log.warn("이미지 파일 삭제 실패 (커밋 후): {}", oldStoredFilename, e);
                    }
                }
            });
        }
    }

    /**
     * 업로드 파일을 검증합니다.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.BREAD_IMAGE_INVALID_TYPE);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.BREAD_IMAGE_SIZE_EXCEEDED);
        }
    }
}
