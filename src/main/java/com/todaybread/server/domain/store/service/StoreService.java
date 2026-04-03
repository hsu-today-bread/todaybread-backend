package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.bread.dto.BreadCommonResponse;
import com.todaybread.server.domain.bread.service.BreadService;
import com.todaybread.server.domain.store.dto.BusinessHoursRequest;
import com.todaybread.server.domain.store.dto.StoreCommonRequest;
import com.todaybread.server.domain.store.dto.StoreCommonResponse;
import com.todaybread.server.domain.store.dto.StoreDetailResponse;
import com.todaybread.server.domain.store.dto.StoreImageResponse;
import com.todaybread.server.domain.store.dto.StoreInfoResponse;
import com.todaybread.server.domain.store.dto.StoreStatusResponse;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.store.util.SellingStatusUtil;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * store 도메인 서비스 계층입니다.
 */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreBusinessHoursRepository storeBusinessHoursRepository;
    private final StoreImageService storeImageService;
    private final BreadService breadService;
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
        Optional<StoreEntity> storeOpt = storeRepository.findByUserIdAndIsActiveTrue(userId);
        if (storeOpt.isEmpty()) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }
        StoreEntity storeEntity = storeOpt.get();

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
        Optional<StoreEntity> storeOpt = storeRepository.findByIdAndIsActiveTrue(storeId);
        if (storeOpt.isEmpty()) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }
        StoreEntity storeEntity = storeOpt.get();

        List<StoreBusinessHoursEntity> businessHours = storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(storeId);
        StoreCommonResponse storeResponse = StoreCommonResponse.from(storeEntity, businessHours);
        List<StoreImageResponse> images = storeImageService.getImagesByStoreId(storeId);
        List<BreadCommonResponse> breads = breadService.getBreadsFromStore(storeId);

        // 재고 존재 여부 확인
        boolean hasStock = breads.stream().anyMatch(b -> b.remainingQuantity() > 0);

        boolean isSelling = SellingStatusUtil.isSelling(
                storeEntity.getIsActive(), businessHours, hasStock, clock);

        return StoreDetailResponse.of(storeResponse, images, breads, isSelling);
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

        storeRepository.save(storeEntity);

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
        Optional<StoreEntity> storeOpt = storeRepository.findByUserIdAndIsActiveTrue(userId);
        if (storeOpt.isEmpty()) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }
        StoreEntity storeEntity = storeOpt.get();
        String phone = request.phone();

        if (!storeEntity.getPhoneNumber().equals(phone)
                && storeRepository.existsByPhoneNumber(phone)) {
            throw new CustomException(ErrorCode.STORE_PHONE_EXISTS);
        }

        storeEntity.updateInfo(request.name(), request.phone(), request.description(),
                request.addressLine1(), request.addressLine2(), request.latitude(),
                request.longitude());

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
            if (!bh.isClosed()) {
                // 영업일인데 startTime 또는 endTime이 null이면 에러
                if (bh.startTime() == null || bh.endTime() == null) {
                    throw new CustomException(ErrorCode.STORE_BUSINESS_HOURS_INVALID);
                }
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
}
