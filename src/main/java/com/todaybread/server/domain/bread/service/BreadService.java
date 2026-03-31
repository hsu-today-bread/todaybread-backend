package com.todaybread.server.domain.bread.service;

import com.todaybread.server.domain.bread.dto.BreadCommonRequest;
import com.todaybread.server.domain.bread.dto.BreadCommonResponse;
import com.todaybread.server.domain.bread.dto.BreadDetailResponse;
import com.todaybread.server.domain.bread.dto.BreadStockUpdateRequest;
import com.todaybread.server.domain.bread.dto.BreadSuccessResponse;
import com.todaybread.server.domain.bread.dto.NearbyBreadResponse;
import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreDistanceProjection;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    /**
     * 빵 상세 정보를 조회합니다.
     *
     * @param breadId 빵 ID
     * @return 빵 상세 응답
     */
    @Transactional(readOnly = true)
    public BreadDetailResponse getBreadDetail(Long breadId) {
        BreadEntity breadEntity = breadRepository.findById(breadId)
                .orElseThrow(() -> new CustomException(ErrorCode.BREAD_NOT_FOUND));

        StoreEntity storeEntity = storeRepository.findByIdAndIsActiveTrue(breadEntity.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        String imageUrl = breadImageService.getImageUrl(breadEntity.getId());

        return BreadDetailResponse.of(breadEntity, storeEntity, imageUrl);
    }

    /**
     * 유저 좌표 기준 반경 내 활성 가게의 빵 목록을 조회합니다.
     * 가게당 대표 빵 1개(할인가 최저)를 선택하고, sort 파라미터에 따라 정렬합니다.
     *
     * @param lat      유저 위도
     * @param lng      유저 경도
     * @param radiusKm 검색 반경 (km)
     * @param sort     정렬 기준 (none, distance, price, discount)
     * @return 근처 빵 응답 리스트
     */
    @Transactional(readOnly = true)
    public List<NearbyBreadResponse> getNearbyBreads(BigDecimal lat, BigDecimal lng,
                                                     int radiusKm, String sort) {
        // 1. Bounding Box 계산
        double latDouble = lat.doubleValue();
        double deltaLat = radiusKm / 111.0;
        double deltaLng = radiusKm / (111.0 * Math.cos(Math.toRadians(latDouble)));

        BigDecimal minLat = lat.subtract(BigDecimal.valueOf(deltaLat));
        BigDecimal maxLat = lat.add(BigDecimal.valueOf(deltaLat));
        BigDecimal minLng = lng.subtract(BigDecimal.valueOf(deltaLng));
        BigDecimal maxLng = lng.add(BigDecimal.valueOf(deltaLng));

        // 2. 반경 내 활성 가게 + 거리 조회 (Haversine)
        List<StoreDistanceProjection> storeResults = storeRepository.findActiveStoresWithinRadius(
                lat, lng, radiusKm, minLat, maxLat, minLng, maxLng);

        if (storeResults.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Projection에서 storeId와 distance 추출
        Map<Long, Double> storeDistanceMap = new HashMap<>();
        List<Long> storeIds = new ArrayList<>();
        for (StoreDistanceProjection row : storeResults) {
            storeDistanceMap.put(row.getStoreId(), row.getDistance());
            storeIds.add(row.getStoreId());
        }

        // 4. StoreEntity 일괄 조회
        List<StoreEntity> storeEntities = storeRepository.findAllById(storeIds);
        Map<Long, StoreEntity> storeMap = new HashMap<>();
        for (StoreEntity store : storeEntities) {
            storeMap.put(store.getId(), store);
        }

        // 5. 해당 가게들의 빵 일괄 조회
        List<BreadEntity> allBreads = breadRepository.findByStoreIdIn(storeIds);
        if (allBreads.isEmpty()) {
            return Collections.emptyList();
        }

        // 6. 가게별 빵 그룹핑 후 대표 빵 1개 선택 (할인가 최저)
        Map<Long, List<BreadEntity>> breadsByStore = allBreads.stream()
                .collect(Collectors.groupingBy(BreadEntity::getStoreId));

        List<BreadEntity> representativeBreads = new ArrayList<>();
        for (Map.Entry<Long, List<BreadEntity>> entry : breadsByStore.entrySet()) {
            entry.getValue().stream()
                    .min(Comparator.comparingInt(BreadEntity::getSalePrice))
                    .ifPresent(representativeBreads::add);
        }

        // 7. 이미지 일괄 조회 (N+1 방지)
        List<Long> breadIds = representativeBreads.stream()
                .map(BreadEntity::getId)
                .collect(Collectors.toList());
        Map<Long, String> imageUrlMap = breadImageService.getImageUrls(breadIds);

        // 8. NearbyBreadResponse 변환
        List<NearbyBreadResponse> responses = new ArrayList<>();
        for (BreadEntity bread : representativeBreads) {
            StoreEntity store = storeMap.get(bread.getStoreId());
            if (store == null) {
                continue;
            }
            double distance = storeDistanceMap.getOrDefault(store.getId(), 0.0);
            String imageUrl = imageUrlMap.get(bread.getId());
            responses.add(NearbyBreadResponse.of(bread, store, imageUrl, distance));
        }

        // 9. sort 파라미터에 따라 정렬
        switch (sort) {
            case "distance":
                responses.sort(Comparator.comparingDouble(NearbyBreadResponse::distance));
                break;
            case "price":
                responses.sort(Comparator.comparingInt(NearbyBreadResponse::salePrice));
                break;
            case "discount":
                responses.sort(Comparator.comparingDouble(
                        (NearbyBreadResponse r) -> {
                            if (r.originalPrice() == 0) return 0.0;
                            return (double) (r.originalPrice() - r.salePrice()) / r.originalPrice();
                        }).reversed());
                break;
            default:
                // none: 셔플하여 특정 가게 독점 방지
                Collections.shuffle(responses);
                break;
        }

        return responses;
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
        return storeRepository.getByUserIdAndIsActiveTrue(userId);
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
