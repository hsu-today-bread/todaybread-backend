package com.todaybread.server.domain.bread.service;

import com.todaybread.server.domain.bread.dto.BreadCommonRequest;
import com.todaybread.server.domain.bread.dto.BreadCommonResponse;
import com.todaybread.server.domain.bread.dto.BreadStockUpdateRequest;
import com.todaybread.server.domain.bread.dto.BreadSuccessResponse;
import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bread 도메인 서비스 계층입니다.
 */
@Service
@RequiredArgsConstructor
public class BreadService {

    private final BreadRepository breadRepository;
    private final BreadImageService breadImageService;
    private final StoreRepository storeRepository;

    /**
     * 요청자의 ID를 검증하고, 빵을 추가합니다.
     * 이미지가 있으면 함께 저장합니다.
     *
     * @param userId  사장님 ID
     * @param request 빵 생성 요청
     * @param image   이미지 파일 (선택사항)
     * @return 빵 공통 응답
     */
    @Transactional
    public BreadCommonResponse addBread(Long userId, BreadCommonRequest request, MultipartFile image) {
        StoreEntity storeEntity = getStoreByUserId(userId);

        BreadEntity breadEntity = BreadEntity.builder().
                storeId(storeEntity.getId()).
                name(request.name()).
                description(request.description()).
                originalPrice(request.originalPrice()).
                salePrice(request.salePrice()).
                remainingQuantity(request.remainingQuantity()).
                build();

        breadRepository.save(breadEntity);

        // 이미지가 있으면 저장
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = breadImageService.uploadImage(breadEntity.getId(), image);
        }

        return BreadCommonResponse.fromEntity(breadEntity, imageUrl);
    }

    /**
     * 빵 정보를 업데이트합니다.
     *
     * @param userId 사장님 ID
     * @param breadId 빵 ID
     * @param request 수정 요청
     * @return 빵 공통 응답
     */
    @Transactional
    public BreadCommonResponse updateBread(Long userId, Long breadId, BreadCommonRequest request, MultipartFile image) {
        BreadEntity breadEntity = getOwnedBread(userId, breadId);

        // 정보 업데이트
        breadEntity.updateInfo(request.name(),
                request.originalPrice(),
                request.salePrice(),
                request.remainingQuantity(),
                request.description());

        // 이미지가 있으면 교체
        String imageUrl;
        if (image != null && !image.isEmpty()) {
            imageUrl = breadImageService.uploadImage(breadEntity.getId(), image);
        } else {
            imageUrl = breadImageService.getImageUrl(breadEntity.getId());
        }

        return BreadCommonResponse.fromEntity(breadEntity, imageUrl);
    }

    /**
     * 빵의 재고를 수정합니다. 빵의 품절 및 해제 처리를 합니다.
     *
     * @param userId 유저 ID
     * @param breadId 빵 ID
     * @param request 빵 재고 수정 요청
     * @return 빵 재고 수정 응답
     */
    @Transactional
    public BreadSuccessResponse changeQuantity(Long userId, Long breadId,
                                                   BreadStockUpdateRequest request) {
        BreadEntity breadEntity = getOwnedBread(userId, breadId);
        int numberOfBread = request.remainingQuantity();
        breadEntity.changeQuantity(numberOfBread);

        return BreadSuccessResponse.ok();
    }

    /**
     * 빵을 삭제합니다.
     *
     * @param userId 사장님 ID
     * @param breadId 빵 ID
     * @return 삭제 응답
     */
    @Transactional
    public BreadSuccessResponse deleteBread(Long userId, Long breadId) {
        BreadEntity breadEntity = getOwnedBread(userId, breadId);

        breadImageService.deleteImage(breadEntity.getId());
        breadRepository.delete(breadEntity);

        return BreadSuccessResponse.ok();
    }

    /**
     * 특정 가게의 빵 정보를 불러옵니다.
     *
     * @param storeId 가게 ID
     * @return 빵 공통 응답 리스트
     */
    @Transactional(readOnly = true)
    public List<BreadCommonResponse> getBreadsFromStore(Long storeId) {
        Optional<StoreEntity> storeEntityOptional = storeRepository.findByIdAndIsActiveTrue(storeId);
        if (storeEntityOptional.isEmpty()) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }

        List<BreadEntity> breadEntityList = breadRepository.findByStoreId(storeId);

        return toBreadResponse(breadEntityList);
    }

    /**
     * 사장님이 자신의 아이디로 자신 가게의 빵 정보를 가져옵니다.
     *
     * @param userId 사장님 ID
     * @return 빵 공통 응답 리스트
     */
    @Transactional(readOnly = true)
    public List<BreadCommonResponse> getMyBreads(Long userId) {
        StoreEntity storeEntity = getStoreByUserId(userId);
        List<BreadEntity> breadEntityList = breadRepository.findByStoreId(storeEntity.getId());

        return toBreadResponse(breadEntityList);
    }

    /*
     * =======================
     * 헬퍼 리스트 구간
     * =======================
     */

    /**
     * 사장님이 자신의 가게를 리턴합니다.
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

    /**
     * 사장님이 소유한 빵을 조회합니다.
     *
     * @param userId 사장님 ID
     * @param breadId 빵 ID
     * @return 빵 엔티티
     */
    private BreadEntity getOwnedBread(Long userId, Long breadId) {
        StoreEntity storeEntity = getStoreByUserId(userId);
        Optional<BreadEntity> breadEntityOptional = breadRepository.findById(breadId);

        if (breadEntityOptional.isEmpty()) {
            throw new CustomException(ErrorCode.BREAD_NOT_FOUND);
        }

        BreadEntity breadEntity = breadEntityOptional.get();
        if (!storeEntity.getId().equals(breadEntity.getStoreId())) {
            throw new CustomException(ErrorCode.BREAD_ACCESS_DENIED);
        }
        return breadEntity;
    }

    /**
     * 빵 엔티티를 빵 공통 응답 리스트 형태로 변환합니다.
     *
     * @param breadEntityList 빵 엔티티 리스트
     * @return 빵 공통 응답 리스트
     */
    private List<BreadCommonResponse> toBreadResponse(List<BreadEntity> breadEntityList) {
        // 이미지 URL을 한 번에 조회 (N+1 방지)
        List<Long> breadIds = new ArrayList<>();
        for (BreadEntity breadEntity : breadEntityList) {
            breadIds.add(breadEntity.getId());
        }
        Map<Long, String> imageUrlMap = breadImageService.getImageUrls(breadIds);

        List<BreadCommonResponse> breadCommonResponseList = new ArrayList<>();
        for (BreadEntity breadEntity : breadEntityList) {
            String imageUrl = imageUrlMap.get(breadEntity.getId());
            breadCommonResponseList.add(BreadCommonResponse.fromEntity(breadEntity, imageUrl));
        }
        return breadCommonResponseList;
    }
}
