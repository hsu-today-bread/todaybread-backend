package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.store.dto.StoreCommonRequest;
import com.todaybread.server.domain.store.dto.StoreCommonResponse;
import com.todaybread.server.domain.store.dto.StoreInfoResponse;
import com.todaybread.server.domain.store.dto.StoreStatusResponse;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * store 도메인 서비스 계층입니다.
 */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreImageService storeImageService;

    /**
     * 사장님 탭 진입 상태를 조회합니다.
     * @param userId 유저 ID
     * @return 가게 등록 여부
     */
    @Transactional(readOnly = true)
    public StoreStatusResponse getStoreStatus(Long userId) {
        Optional<StoreEntity> storeEntityOptional = storeRepository.findByUserIdAndIsActiveTrue(userId);

        if (storeEntityOptional.isEmpty()) {
            return StoreStatusResponse.notRegistered();
        }
        return StoreStatusResponse.registered();
    }

    /**
     * 매장 정보와 이미지를 한번에 조회합니다.
     * @param userId 유저 ID
     * @return 매장 정보 + 이미지 목록
     */
    @Transactional(readOnly = true)
    public StoreInfoResponse getStoreInfo(Long userId) {
        StoreEntity storeEntity = getStoreByUserId(userId);

        StoreCommonResponse storeResponse = StoreCommonResponse.from(storeEntity);
        var images = storeImageService.getImagesByStoreId(storeEntity.getId());

        return StoreInfoResponse.of(storeResponse, images);
    }

    /**
     * 가게 등록 요청을 처리합니다.
     * 가게 정보와 이미지를 함께 저장합니다.
     *
     * @param userId 유저 ID
     * @param request 요청 DTO
     * @param images 이미지 파일 목록 (1~5장)
     * @return 가게 정보 + 이미지 응답
     */
    @Transactional
    public StoreInfoResponse addStore(Long userId, StoreCommonRequest request, List<MultipartFile> images) {
        if (storeRepository.existsByUserIdAndIsActiveTrue(userId)) {
            throw new CustomException(ErrorCode.STORE_ALREADY_EXISTS);
        }

        if (storeRepository.existsByPhoneNumber(request.phone())) {
            throw new CustomException(ErrorCode.STORE_PHONE_EXISTS);
        }

        StoreEntity storeEntity = StoreEntity.builder()
                .userId(userId)
                .name(request.name())
                .phoneNumber(request.phone())
                .description(request.description())
                .addressLine1(request.addressLine1())
                .addressLine2(request.addressLine2())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .endTime(request.endTime())
                .lastOrderTime(request.lastOrderTime())
                .orderTime(request.orderTime())
                .build();

        storeRepository.save(storeEntity);

        // 이미지 저장 (소유권 검증 생략 — 방금 생성한 가게)
        var savedImages = storeImageService.saveImages(storeEntity.getId(), images);

        StoreCommonResponse storeResponse = StoreCommonResponse.from(storeEntity);
        return StoreInfoResponse.of(storeResponse, savedImages);
    }

    /**
     * 가게 정보를 업데이트 합니다.
     * 중복 검사도 진행합니다.
     *
     * @param userId 유저 ID
     * @param request 요청 DTO
     * @return 공통 응답 DTO
     */
    @Transactional
    public StoreCommonResponse updateStore(Long userId, StoreCommonRequest request) {
        StoreEntity storeEntity = getStoreByUserId(userId);
        String phone = request.phone();

        if (!storeEntity.getPhoneNumber().equals(phone)
                && storeRepository.existsByPhoneNumber(phone)) {
            throw new CustomException(ErrorCode.STORE_PHONE_EXISTS);
        }

        storeEntity.updateInfo(request.name(), request.phone(), request.description(),
                request.addressLine1(), request.addressLine2(), request.latitude(),
                request.longitude(), request.endTime(), request.lastOrderTime(), request.orderTime());

        return StoreCommonResponse.from(storeEntity);
    }

    /**
     * 사장님의 활성 가게를 조회합니다.
     *
     * @param userId 사장님 ID
     * @return 가게 엔티티
     */
    private StoreEntity getStoreByUserId(Long userId) {
        Optional<StoreEntity> storeEntityOptional = storeRepository.findByUserIdAndIsActiveTrue(userId);

        if (storeEntityOptional.isEmpty()) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }
        return storeEntityOptional.get();
    }
}
