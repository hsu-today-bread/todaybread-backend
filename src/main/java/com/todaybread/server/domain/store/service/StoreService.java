package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.bread.dto.BreadCommonResponse;
import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.bread.service.BreadService;
import com.todaybread.server.domain.store.dto.BusinessHoursRequest;
import com.todaybread.server.domain.store.dto.NearbyStoreResponse;
import com.todaybread.server.domain.store.dto.StoreCommonRequest;
import com.todaybread.server.domain.store.dto.StoreCommonResponse;
import com.todaybread.server.domain.store.dto.StoreDetailResponse;
import com.todaybread.server.domain.store.dto.StoreImageResponse;
import com.todaybread.server.domain.store.dto.StoreInfoResponse;
import com.todaybread.server.domain.store.dto.StoreStatusResponse;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.entity.StoreImageEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreDistanceProjection;
import com.todaybread.server.domain.store.repository.StoreImageRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.store.util.SellingStatus;
import com.todaybread.server.domain.store.util.SellingStatusUtil;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.global.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * store 도메인 서비스 계층입니다.
 */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreBusinessHoursRepository storeBusinessHoursRepository;
    private final StoreImageRepository storeImageRepository;
    private final StoreImageService storeImageService;
    private final BreadRepository breadRepository;
    private final BreadService breadService;
    private final FileStorage fileStorage;
    private final Clock clock;

    /**
     * 사장님 탭 진입 상태를 조회합니다.
     *
     * @param userId 유저 ID
     * @return 가게 등록 여부
     */
    @Transactional(readOnly = true)
    public StoreStatusResponse getStoreStatus(Long userId) {
        if (storeRepository.existsByUserIdAndIsActiveTrue(userId)) {
            return StoreStatusResponse.registered();
        }
        return StoreStatusResponse.notRegistered();
    }

    /**
     * 매장 정보와 이미지를 한번에 조회합니다.
     *
     * @param userId 유저 ID
     * @return 매장 정보 + 이미지 목록
     */
    @Transactional(readOnly = true)
    public StoreInfoResponse getStoreInfo(Long userId) {
        StoreEntity storeEntity = storeRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        List<StoreBusinessHoursEntity> businessHours = storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(storeEntity.getId());
        StoreCommonResponse storeResponse = StoreCommonResponse.from(storeEntity, businessHours);
        var images = storeImageService.getImagesByStoreId(storeEntity.getId());

        return StoreInfoResponse.of(storeResponse, images);
    }

    /**
     * 가게 상세 정보를 조회합니다.
     * 가게 정보, 이미지 목록, 빵 목록, 판매 상태를 한번에 반환합니다.
     *
     * @param storeId 가게 ID
     * @return 가게 상세 응답
     */
    @Transactional(readOnly = true)
    public StoreDetailResponse getStoreDetail(Long storeId) {
        StoreEntity storeEntity = storeRepository.findByIdAndIsActiveTrue(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        List<StoreBusinessHoursEntity> businessHours = storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(storeId);
        StoreCommonResponse storeResponse = StoreCommonResponse.from(storeEntity, businessHours);
        List<StoreImageResponse> images = storeImageService.getImagesByStoreId(storeId);
        List<BreadCommonResponse> breads = breadService.getBreadsFromStore(storeId);

        // 재고 존재 여부 확인
        boolean hasStock = breads.stream().anyMatch(b -> b.remainingQuantity() > 0);

        SellingStatus sellingStatus = SellingStatusUtil.getSellingStatus(
                storeEntity.getIsActive(), businessHours, hasStock, clock);

        return StoreDetailResponse.of(storeResponse, images, breads, sellingStatus,
                storeEntity.getAverageRating(), storeEntity.getReviewCount());
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
                .build();

        try {
            storeRepository.save(storeEntity);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.STORE_ALREADY_EXISTS);
        }

        // 영업시간 검증 및 저장
        List<StoreBusinessHoursEntity> businessHours = validateAndSaveBusinessHours(
                storeEntity.getId(), request.businessHours());

        // 이미지 저장 (소유권 검증 생략 — 방금 생성한 가게)
        var savedImages = storeImageService.saveImages(storeEntity.getId(), images);

        StoreCommonResponse storeResponse = StoreCommonResponse.from(storeEntity, businessHours);
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
        StoreEntity storeEntity = storeRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
        String phone = request.phone();

        if (!storeEntity.getPhoneNumber().equals(phone)
                && storeRepository.existsByPhoneNumber(phone)) {
            throw new CustomException(ErrorCode.STORE_PHONE_EXISTS);
        }

        storeEntity.updateInfo(request.name(), request.phone(), request.description(),
                request.addressLine1(), request.addressLine2(), request.latitude(),
                request.longitude());

        try {
            storeRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.STORE_PHONE_EXISTS);
        }

        // 기존 영업시간 삭제 후 새로운 영업시간 저장
        storeBusinessHoursRepository.deleteByStoreId(storeEntity.getId());
        List<StoreBusinessHoursEntity> businessHours = validateAndSaveBusinessHours(
                storeEntity.getId(), request.businessHours());

        return StoreCommonResponse.from(storeEntity, businessHours);
    }

    /**
     * 영업시간 데이터를 검증하고 저장합니다.
     * addStore와 updateStore에서 공통으로 사용합니다.
     *
     * @param storeId       가게 ID
     * @param businessHours 요일별 영업시간 요청 리스트
     * @return 저장된 영업시간 엔티티 리스트
     */
    private List<StoreBusinessHoursEntity> validateAndSaveBusinessHours(
            Long storeId, List<BusinessHoursRequest> businessHours) {

        // dayOfWeek 중복 체크
        Set<Integer> dayOfWeekSet = new HashSet<>();
        for (BusinessHoursRequest bh : businessHours) {
            if (!dayOfWeekSet.add(bh.dayOfWeek())) {
                throw new CustomException(ErrorCode.STORE_DAY_OF_WEEK_DUPLICATE);
            }
        }

        List<StoreBusinessHoursEntity> entities = new ArrayList<>();
        for (BusinessHoursRequest bh : businessHours) {
            if (bh.isClosed()) {
                // 휴무일: 모든 시간값이 null이어야 함
                if (bh.startTime() != null || bh.endTime() != null || bh.lastOrderTime() != null) {
                    throw new CustomException(ErrorCode.STORE_BUSINESS_HOURS_INVALID);
                }
            } else {
                // 영업일: startTime, endTime, lastOrderTime 모두 필수
                if (bh.startTime() == null || bh.endTime() == null || bh.lastOrderTime() == null) {
                    throw new CustomException(ErrorCode.STORE_BUSINESS_HOURS_INVALID);
                }
                // startTime == endTime 금지
                if (bh.startTime().equals(bh.endTime())) {
                    throw new CustomException(ErrorCode.STORE_BUSINESS_HOURS_INVALID);
                }
                // lastOrderTime 범위 검증
                validateLastOrderTime(bh.startTime(), bh.endTime(), bh.lastOrderTime());
            }

            StoreBusinessHoursEntity entity = StoreBusinessHoursEntity.builder()
                    .storeId(storeId)
                    .dayOfWeek(bh.dayOfWeek())
                    .isClosed(bh.isClosed())
                    .startTime(bh.isClosed() ? null : bh.startTime())
                    .endTime(bh.isClosed() ? null : bh.endTime())
                    .lastOrderTime(bh.isClosed() ? null : bh.lastOrderTime())
                    .build();

            entities.add(entity);
        }

        return storeBusinessHoursRepository.saveAll(entities);
    }

    /**
     * lastOrderTime이 영업시간 범위 내에 있는지 검증합니다.
     * 자정 넘김 영업(startTime > endTime)도 지원합니다.
     *
     * @param startTime     영업 시작 시간
     * @param endTime       영업 종료 시간
     * @param lastOrderTime 마지막 주문 시간
     */
    private void validateLastOrderTime(LocalTime startTime, LocalTime endTime, LocalTime lastOrderTime) {
        if (startTime.isBefore(endTime)) {
            // 일반 영업: lastOrderTime은 startTime 이상, endTime 이하
            if (lastOrderTime.isBefore(startTime) || lastOrderTime.isAfter(endTime)) {
                throw new CustomException(ErrorCode.STORE_BUSINESS_HOURS_INVALID);
            }
        } else {
            // 자정 넘김 영업 (startTime > endTime): lastOrderTime이 startTime 이상이거나 endTime 이하
            boolean afterStart = !lastOrderTime.isBefore(startTime);
            boolean beforeEnd = !lastOrderTime.isAfter(endTime);
            if (!afterStart && !beforeEnd) {
                throw new CustomException(ErrorCode.STORE_BUSINESS_HOURS_INVALID);
            }
        }
    }

    /**
     * 유저 좌표 기준 반경 내 활성 가게 목록을 조회합니다.
     * 거리순 오름차순으로 정렬하여 반환합니다.
     *
     * @param lat      유저 위도
     * @param lng      유저 경도
     * @param radiusKm 검색 반경 (km)
     * @return 근처 가게 응답 리스트
     */
    @Transactional(readOnly = true)
    public List<NearbyStoreResponse> getNearbyStores(BigDecimal lat, BigDecimal lng, int radiusKm) {
        // 1. Bounding Box 계산 (BreadService.getNearbyBreads()와 동일한 공식)
        double latDouble = lat.doubleValue();
        double deltaLat = radiusKm / 111.0;
        double cosLat = Math.cos(Math.toRadians(latDouble));
        double deltaLng = (Math.abs(cosLat) < 1e-10) ? 180.0 : radiusKm / (111.0 * cosLat);

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

        // 4. 가게 엔티티 일괄 조회
        Map<Long, StoreEntity> storeMap = new HashMap<>();
        for (StoreEntity store : storeRepository.findByIdInAndIsActiveTrue(storeIds)) {
            storeMap.put(store.getId(), store);
        }

        // 5. 영업시간 일괄 조회 → 가게별 그룹핑
        Map<Long, List<StoreBusinessHoursEntity>> businessHoursMap =
                storeBusinessHoursRepository.findByStoreIdIn(storeIds).stream()
                        .collect(Collectors.groupingBy(StoreBusinessHoursEntity::getStoreId));

        // 6. 빵 재고 일괄 조회 → 가게별 그룹핑 (삭제된 빵 제외)
        Map<Long, List<BreadEntity>> breadsByStore =
                breadRepository.findByStoreIdInAndIsDeletedFalse(storeIds).stream()
                        .collect(Collectors.groupingBy(BreadEntity::getStoreId));

        // 7. 대표 이미지 일괄 조회
        Map<Long, String> primaryImageMap = buildPrimaryImageMap(storeIds);

        // 8. 오늘 요일
        int todayDayOfWeek = LocalDate.now(clock).getDayOfWeek().getValue();

        // 9. 응답 조립
        List<NearbyStoreResponse> responses = new ArrayList<>();
        for (StoreDistanceProjection projection : storeResults) {
            StoreEntity store = storeMap.get(projection.getStoreId());
            if (store == null) {
                continue;
            }

            // 가게별 영업시간 목록
            List<StoreBusinessHoursEntity> storeHours = businessHoursMap.getOrDefault(store.getId(), List.of());

            // 가게별 재고 존재 여부
            List<BreadEntity> breads = breadsByStore.getOrDefault(store.getId(), List.of());
            boolean hasStock = breads.stream().anyMatch(b -> b.getRemainingQuantity() > 0);

            // 판매 상태 판별 (전날 자정 넘김 영업도 고려)
            SellingStatus sellingStatus = SellingStatusUtil.getSellingStatus(
                    store.getIsActive(), storeHours, hasStock, clock);
            boolean isSelling = sellingStatus == SellingStatus.SELLING;

            // lastOrderTime 추출 (오늘 row 기준)
            StoreBusinessHoursEntity todayHours = storeHours.stream()
                    .filter(h -> h.getDayOfWeek().equals(todayDayOfWeek))
                    .findFirst()
                    .orElse(null);
            LocalTime lastOrderTime = (todayHours != null) ? todayHours.getLastOrderTime() : null;

            responses.add(new NearbyStoreResponse(
                    store.getId(),
                    store.getName(),
                    store.getAddressLine1(),
                    store.getAddressLine2(),
                    store.getLatitude(),
                    store.getLongitude(),
                    primaryImageMap.get(store.getId()),
                    isSelling,
                    sellingStatus,
                    projection.getDistance(),
                    lastOrderTime,
                    store.getAverageRating(),
                    store.getReviewCount()
            ));
        }

        // 10. 거리순 오름차순 정렬
        responses.sort(Comparator.comparingDouble(NearbyStoreResponse::distance));

        return responses;
    }

    /**
     * 가게 ID 목록에 대해 대표 이미지 URL 맵을 구성합니다.
     * displayOrder=0인 이미지의 URL을 반환하며, 없으면 해당 가게는 맵에 포함되지 않습니다.
     * 일괄 조회로 N+1 쿼리를 방지합니다.
     *
     * @param storeIds 가게 ID 목록
     * @return storeId → 대표 이미지 URL 매핑
     */
    private Map<Long, String> buildPrimaryImageMap(List<Long> storeIds) {
        List<StoreImageEntity> allImages = storeImageRepository
                .findByStoreIdInOrderByStoreIdAscDisplayOrderAsc(storeIds);
        Map<Long, String> result = new HashMap<>();
        for (StoreImageEntity image : allImages) {
            if (image.getDisplayOrder() == 0) {
                result.put(image.getStoreId(), fileStorage.getFileUrl(image.getStoredFilename()));
            }
        }
        return result;
    }
}
