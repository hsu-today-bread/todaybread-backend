package com.todaybread.server.domain.bread.service;

import com.todaybread.server.domain.bread.dto.BreadAddRequest;
import com.todaybread.server.domain.bread.dto.BreadCommonResponse;
import com.todaybread.server.domain.bread.dto.BreadUpdateRequest;
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
import java.util.Objects;
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
    public BreadCommonResponse addBread(Long userId, BreadAddRequest request, MultipartFile image) {
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
            imageUrl = breadImageService.uploadImage(userId, breadEntity.getId(), image);
        }

        return BreadCommonResponse.fromEntity(breadEntity, imageUrl);
    }

    /**
     * 빵을 업데이트합니다.
     * 삭제/품절 여부 혹은 빵 정보를 수정합니다.
     *
     * @param userId 사장님 ID
     * @param request 재고 업데이트 요청
     * @return 빵 공통 응답
     */
    @Transactional
    public BreadCommonResponse updateBread(Long userId, BreadUpdateRequest request, MultipartFile image) {

        StoreEntity storeEntity = getStoreByUserId(userId);
        Optional<BreadEntity> breadEntityOptional = breadRepository.findById(request.id());

        if (breadEntityOptional.isEmpty()) {
            throw new CustomException(ErrorCode.BREAD_NOT_FOUND);
        }
        BreadEntity breadEntity = breadEntityOptional.get();

        if(!Objects.equals(storeEntity.getId(), breadEntity.getStoreId())){
            throw new CustomException(ErrorCode.BREAD_ACCESS_DENIED);
        }

        boolean deletion = request.delete();
        boolean soldOut = request.soldOut();

        // 삭제하는 경우
        if(deletion) {
            breadRepository.delete(breadEntity);
            breadImageService.deleteImage(breadEntity.getId());
            return BreadCommonResponse.deletedOrSoldout();
        }

        // 품절의 경우
        if(soldOut) {
            breadEntity.setQuantity(0);
            return BreadCommonResponse.deletedOrSoldout();
        }

        // 정보 업데이트
        BreadAddRequest breadAddRequest = request.breadAddRequest();
        breadEntity.updateInfo(breadAddRequest.name(),
                breadAddRequest.originalPrice(),
                breadAddRequest.salePrice(),
                breadAddRequest.remainingQuantity(),
                breadAddRequest.description());
        breadRepository.save(breadEntity);

        // 이미지가 있으면 교체
        String imageUrl;
        if (image != null && !image.isEmpty()) {
            imageUrl = breadImageService.uploadImage(userId, breadEntity.getId(), image);
        } else {
            imageUrl = breadImageService.getImageUrl(breadEntity.getId());
        }

        return BreadCommonResponse.fromEntity(breadEntity, imageUrl);
    }

    /**
     * 특정 가게의 빵 정보를 불러옵니다.
     *
     * @param storeId 가게 ID
     * @return 빵 공통 응답 리스트
     */
    @Transactional(readOnly = true)
    public List<BreadCommonResponse> getBreadsFromStore (Long storeId) {
        Optional<StoreEntity> storeEntityOptional = storeRepository.findById(storeId);
        if (storeEntityOptional.isEmpty()) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }

        List<BreadEntity> breadEntityList = breadRepository.findBreadEntitiesByStoreId(storeId);

        List<BreadCommonResponse> breadCommonResponseList = new ArrayList<>();
        for (BreadEntity breadEntity : breadEntityList) {
            breadCommonResponseList.add(BreadCommonResponse.fromEntity(breadEntity));
            breadImageService.getImageUrl(breadEntity.getId());
        }
        return breadCommonResponseList;
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
        List<BreadEntity> breadEntityList = breadRepository.findBreadEntitiesByStoreId(storeEntity.getId());

        List<BreadCommonResponse> breadCommonResponseList = new ArrayList<>();
        for (BreadEntity breadEntity : breadEntityList) {
            breadCommonResponseList.add(BreadCommonResponse.fromEntity(breadEntity));
            breadImageService.getImageUrl(breadEntity.getId());
        }
        return breadCommonResponseList;
    }

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
}