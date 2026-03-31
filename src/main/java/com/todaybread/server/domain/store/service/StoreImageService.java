package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.store.dto.StoreImageResponse;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.entity.StoreImageEntity;
import com.todaybread.server.domain.store.repository.StoreImageRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
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
import java.util.List;
import java.util.Optional;

/**
 * 가게 이미지 서비스 계층입니다.
 * 이미지 일괄 업로드(Replace All) 및 조회를 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class StoreImageService {

    private static final Logger log = LoggerFactory.getLogger(StoreImageService.class);

    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
    private final FileStorage fileStorage;

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
        Optional<StoreEntity> storeOptional = storeRepository.findByUserIdAndIsActiveTrue(userId);

        if (storeOptional.isEmpty()) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }
        StoreEntity store = storeOptional.get();

        // 2. 파일 검증
        ImageValidationHelper.validateFiles(files, 5, ErrorCode.STORE_IMAGE_LIMIT_EXCEEDED, ErrorCode.COMMON_IMAGE_INVALID_TYPE, ErrorCode.COMMON_FILE_SIZE_EXCEEDED);

        // 3. 기존 이미지 삭제
        deleteExistingImages(store.getId());

        // 4. 새 이미지 저장
        List<String> storedFilenames = new ArrayList<>();
        List<StoreImageEntity> savedEntities;
        try {
            List<StoreImageEntity> entities = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                int displayOrder = i;

                String storedFilename = fileStorage.store(file, "store", store.getId());
                storedFilenames.add(storedFilename);

                StoreImageEntity entity = StoreImageEntity.builder()
                        .storeId(store.getId())
                        .originalFilename(file.getOriginalFilename())
                        .storedFilename(storedFilename)
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
            throw new CustomException(ErrorCode.COMMON_IMAGE_STORAGE_FAILED);
        }

        // 5. 응답 반환
        return getStoreImageResponses(savedEntities);
    }

    /**
     * 소유권 검증 없이 이미지를 저장합니다.
     * 이미 소유권이 검증된 서비스 내부 호출용입니다 (가게 등록 시).
     *
     * @param storeId 가게 ID
     * @param files 업로드할 이미지 파일 목록
     * @return 저장된 이미지 목록
     */
    @Transactional
    public List<StoreImageResponse> saveImages(Long storeId, List<MultipartFile> files) {
        ImageValidationHelper.validateFiles(files, 5, ErrorCode.STORE_IMAGE_LIMIT_EXCEEDED, ErrorCode.COMMON_IMAGE_INVALID_TYPE, ErrorCode.COMMON_FILE_SIZE_EXCEEDED);

        List<String> storedFilenames = new ArrayList<>();
        List<StoreImageEntity> savedEntities;
        try {
            List<StoreImageEntity> entities = new ArrayList<>();
            for (int displayOrder = 0; displayOrder < files.size(); displayOrder++) {
                MultipartFile file = files.get(displayOrder);

                String storedFilename = fileStorage.store(file, "store", storeId);
                storedFilenames.add(storedFilename);

                StoreImageEntity entity = StoreImageEntity.builder()
                        .storeId(storeId)
                        .originalFilename(file.getOriginalFilename())
                        .storedFilename(storedFilename)
                        .displayOrder(displayOrder)
                        .build();
                entities.add(entity);
            }
            savedEntities = storeImageRepository.saveAll(entities);
        } catch (Exception e) {
            cleanupStoredFiles(storedFilenames);
            if (e instanceof CustomException) {
                throw e;
            }
            throw new CustomException(ErrorCode.COMMON_IMAGE_STORAGE_FAILED);
        }

        return getStoreImageResponses(savedEntities);
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
    public List<StoreImageResponse> getImagesByStoreId(Long storeId) {
        List<StoreImageEntity> images = storeImageRepository.findByStoreIdOrderByDisplayOrderAsc(storeId);
        return getStoreImageResponses(images);
    }

    /**
     * 이미지 엔티티에서 응답으로 변환합니다
     * @param images 이미지 엔티티 리스트
     * @return 이미지 응답 형식
     */
    private List<StoreImageResponse> getStoreImageResponses(List<StoreImageEntity> images) {
        List<StoreImageResponse> responses = new ArrayList<>();
        for (StoreImageEntity entity : images) {
            StoreImageResponse response = new StoreImageResponse(
                    entity.getId(),
                    fileStorage.getFileUrl(entity.getStoredFilename()),
                    entity.getDisplayOrder()
            );
            responses.add(response);
        }
        return responses;
    }

    /**
     * 기존 이미지를 삭제합니다 (DB 즉시 삭제, 파일은 커밋 후 삭제).
     * 롤백 시 파일이 유지되도록 afterCommit 패턴을 사용합니다.
     */
    private void deleteExistingImages(Long storeId) {
        List<StoreImageEntity> existingImages = storeImageRepository.findByStoreIdOrderByDisplayOrderAsc(storeId);

        List<String> filesToDelete = new ArrayList<>();
        for (StoreImageEntity image : existingImages) {
            filesToDelete.add(image.getStoredFilename());
        }

        storeImageRepository.deleteByStoreId(storeId);
        storeImageRepository.flush();

        if (!filesToDelete.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (String filename : filesToDelete) {
                        try {
                            fileStorage.delete(filename);
                        } catch (Exception e) {
                            log.warn("기존 이미지 파일 삭제 실패 (커밋 후): {}", filename, e);
                        }
                    }
                }
            });
        }
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
