package com.todaybread.server.domain.bread.service;

import com.todaybread.server.domain.bread.entity.BreadImageEntity;
import com.todaybread.server.domain.bread.repository.BreadImageRepository;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.global.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
            "image/jpeg", "image/png", "image/webp", "image/jpg"
    );
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

    private final FileStorage fileStorage;
    private final StoreRepository storeRepository;
    private final BreadRepository breadRepository;
    private final BreadImageRepository breadImageRepository;

    /**
     * 음식 이미지를 업로드합니다 (1장 교체 방식).
     * 기존 이미지가 있으면 삭제 후 새 이미지로 교체합니다.
     *
     * @param userId  사장님 유저 ID (JWT에서 추출)
     * @param breadId 음식 ID
     * @param file    업로드할 이미지 파일
     * @return 저장된 이미지 URL (null이면 이미지 없음)
     */
    @Transactional
    public String uploadImage(Long userId, Long breadId, MultipartFile file) {
        // 1. Store 소유권 검증
        Optional<StoreEntity> storeOptional = storeRepository.findByUserIdAndIsActiveTrue(userId);
        if (storeOptional.isEmpty()) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }
        StoreEntity store = storeOptional.get();

        // 2. Bread 존재 + 소유권 검증
        Optional<BreadEntity> breadOptional = breadRepository.findById(breadId);
        if (breadOptional.isEmpty()) {
            throw new CustomException(ErrorCode.BREAD_NOT_FOUND);
        }
        BreadEntity bread = breadOptional.get();

        if (!bread.getStoreId().equals(store.getId())) {
            throw new CustomException(ErrorCode.BREAD_ACCESS_DENIED);
        }

        // 3. 파일 검증
        validateFile(file);

        // 4. 기존 이미지 있으면 삭제
        Optional<BreadImageEntity> existingImage = breadImageRepository.findByStoreId(breadId);
        if (existingImage.isPresent()) {
            BreadImageEntity existing = existingImage.get();
            try {
                fileStorage.delete(existing.getStoredFilename());
            } catch (Exception e) {
                log.warn("기존 이미지 파일 삭제 실패 (계속 진행): {}", existing.getStoredFilename(), e);
            }
            breadImageRepository.delete(existing);
        }

        // 5. 새 이미지 저장
        String storedFilename;
        try {
            storedFilename = fileStorage.store(file, "bread", breadId, 0);
        } catch (Exception e) {
            log.error("이미지 저장 실패: breadId={}", breadId, e);
            throw new CustomException(ErrorCode.BREAD_IMAGE_STORAGE_FAILED);
        }

        BreadImageEntity entity = BreadImageEntity.builder()
                .breadId(breadId)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .filePath(fileStorage.getFileUrl(storedFilename))
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
        Optional<BreadImageEntity> imageOptional = breadImageRepository.findByStoreId(breadId);
        if (imageOptional.isEmpty()) {
            return null;
        }
        return fileStorage.getFileUrl(imageOptional.get().getStoredFilename());
    }

    /**
     * 음식 이미지를 삭제합니다 (파일 + DB).
     *
     * @param breadId 음식 ID
     */
    @Transactional
    public void deleteImage(Long breadId) {
        Optional<BreadImageEntity> imageOptional = breadImageRepository.findByStoreId(breadId);
        if (imageOptional.isPresent()) {
            BreadImageEntity image = imageOptional.get();
            try {
                fileStorage.delete(image.getStoredFilename());
            } catch (Exception e) {
                log.warn("이미지 파일 삭제 실패 (계속 진행): {}", image.getStoredFilename(), e);
            }
            breadImageRepository.delete(image);
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
