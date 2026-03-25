package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.store.dto.StoreImageResponse;
import com.todaybread.server.domain.store.dto.StoreImageSummary;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.entity.StoreImageEntity;
import com.todaybread.server.domain.store.repository.StoreImageRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 가게 이미지 서비스 계층입니다.
 * 이미지 일괄 업로드(Replace All) 및 조회를 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class StoreImageService {

    private static final Logger log = LoggerFactory.getLogger(StoreImageService.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final int MAX_FILE_COUNT = 5;

    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
    private final FileStorageService fileStorageService;

    /**
     * 가게 이미지를 일괄 교체합니다 (Replace All 패턴).
     * 기존 이미지를 모두 삭제하고 새로 전달받은 이미지를 저장합니다.
     *
     * @param userId 사장님 유저 ID
     * @param files  업로드할 이미지 파일 목록
     * @return 저장된 이미지 목록
     */
    @Transactional
    public List<StoreImageResponse> replaceImages(Long userId, List<MultipartFile> files) {
        // 1. userId로 가게 조회
        StoreEntity store = storeRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        // 2. 파일 검증
        validateFiles(files);

        // 3. 기존 이미지 삭제
        deleteExistingImages(store.getId());

        // 4. 새 이미지 저장
        List<String> storedFilenames = new ArrayList<>();
        List<StoreImageEntity> savedEntities;
        try {
            List<StoreImageEntity> entities = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                int displayOrder = i + 1;

                String storedFilename = fileStorageService.store(file, store.getId(), displayOrder);
                storedFilenames.add(storedFilename);

                StoreImageEntity entity = StoreImageEntity.builder()
                        .storeId(store.getId())
                        .originalFilename(file.getOriginalFilename())
                        .storedFilename(storedFilename)
                        .filePath(fileStorageService.getFileUrl(storedFilename))
                        .displayOrder(displayOrder)
                        .build();
                entities.add(entity);
            }
            savedEntities = storeImageRepository.saveAll(entities);
        } catch (Exception e) {
            // 저장 중 오류 시 이미 저장된 파일 정리
            cleanupStoredFiles(storedFilenames);
            if (e instanceof CustomException) {
                throw e;
            }
            throw new CustomException(ErrorCode.STORE_IMAGE_STORAGE_FAILED);
        }

        // 5. 응답 반환
        return savedEntities.stream()
                .map(entity -> new StoreImageResponse(
                        entity.getId(),
                        fileStorageService.getFileUrl(entity.getStoredFilename()),
                        entity.getOriginalFilename(),
                        entity.getDisplayOrder()
                ))
                .toList();
    }

    /**
     * 가게 ID로 이미지 목록을 조회합니다.
     * display_order 오름차순으로 정렬된 이미지 목록을 반환합니다.
     * 이미지가 없으면 빈 목록을 반환합니다.
     *
     * @param storeId 가게 ID
     * @return 이미지 요약 목록
     */
    @Transactional(readOnly = true)
    public List<StoreImageSummary> getImagesByStoreId(Long storeId) {
        return storeImageRepository.findByStoreIdOrderByDisplayOrderAsc(storeId)
                .stream()
                .map(entity -> new StoreImageSummary(
                        entity.getId(),
                        fileStorageService.getFileUrl(entity.getStoredFilename()),
                        entity.getDisplayOrder()
                ))
                .toList();
    }

    /**
     * 업로드 파일 목록을 검증합니다.
     */
    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new CustomException(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED);
        }

        if (files.size() > MAX_FILE_COUNT) {
            throw new CustomException(ErrorCode.STORE_IMAGE_LIMIT_EXCEEDED);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new CustomException(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED);
            }

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw new CustomException(ErrorCode.STORE_IMAGE_INVALID_TYPE);
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new CustomException(ErrorCode.STORE_IMAGE_SIZE_EXCEEDED);
            }
        }
    }

    /**
     * 기존 이미지를 삭제합니다 (파일 + DB).
     * 파일 삭제 실패 시 로깅 후 계속 진행합니다.
     */
    private void deleteExistingImages(Long storeId) {
        List<StoreImageEntity> existingImages = storeImageRepository.findByStoreIdOrderByDisplayOrderAsc(storeId);

        for (StoreImageEntity image : existingImages) {
            try {
                fileStorageService.delete(image.getStoredFilename());
            } catch (Exception e) {
                log.warn("기존 이미지 파일 삭제 실패 (계속 진행): {}", image.getStoredFilename(), e);
            }
        }

        storeImageRepository.deleteByStoreId(storeId);
    }

    /**
     * 저장 중 오류 발생 시 이미 저장된 파일을 정리합니다.
     */
    private void cleanupStoredFiles(List<String> storedFilenames) {
        for (String filename : storedFilenames) {
            try {
                fileStorageService.delete(filename);
            } catch (Exception e) {
                log.warn("저장 실패 후 파일 정리 중 오류: {}", filename, e);
            }
        }
    }
}
