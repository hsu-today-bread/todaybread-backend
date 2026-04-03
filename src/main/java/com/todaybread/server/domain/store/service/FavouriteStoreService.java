package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.store.dto.FavouriteStoreResponse;
import com.todaybread.server.domain.store.dto.FavouriteStoreToggleRequest;
import com.todaybread.server.domain.store.dto.FavouriteStoreToggleResponse;
import com.todaybread.server.domain.store.entity.FavouriteStoreEntity;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.entity.StoreImageEntity;
import com.todaybread.server.domain.store.repository.FavouriteStoreRepository;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreImageRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.store.util.SellingStatusUtil;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.global.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 단골 가게 서비스 계층입니다.
 * 단골 가게 토글(추가/해제) 및 목록 조회를 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class FavouriteStoreService {

    private static final int MAX_FAVOURITE_STORES = 5;

    private final FavouriteStoreRepository favouriteStoreRepository;
    private final StoreRepository storeRepository;
    private final BreadRepository breadRepository;
    private final StoreImageRepository storeImageRepository;
    private final StoreBusinessHoursRepository storeBusinessHoursRepository;
    private final FileStorage fileStorage;
    private final Clock clock;

    /**
     * 단골 가게를 토글합니다 (추가/해제).
     * 이미 등록된 가게면 삭제하고, 등록되지 않은 가게면 추가합니다.
     *
     * @param userId  유저 ID
     * @param request 토글 요청 DTO
     * @return 토글 결과 (added: true/false)
     */
    @Transactional
    public FavouriteStoreToggleResponse toggleFavouriteStore(Long userId, FavouriteStoreToggleRequest request) {
        Optional<FavouriteStoreEntity> existing = favouriteStoreRepository.findByUserIdAndStoreId(userId, request.storeId());

        if (existing.isPresent()) {
            favouriteStoreRepository.delete(existing.get());
            return new FavouriteStoreToggleResponse(false);
        }

        if (storeRepository.findByIdAndIsActiveTrue(request.storeId()).isEmpty()) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }

        if (favouriteStoreRepository.countByUserIdWithLock(userId) >= MAX_FAVOURITE_STORES) {
            throw new CustomException(ErrorCode.FAVOURITE_STORE_LIMIT_EXCEEDED);
        }

        FavouriteStoreEntity entity = FavouriteStoreEntity.builder()
                .userId(userId)
                .storeId(request.storeId())
                .build();

        try {
            favouriteStoreRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 유니크 제약조건 위반 시 이미 등록된 것으로 간주
            return new FavouriteStoreToggleResponse(true);
        }

        return new FavouriteStoreToggleResponse(true);
    }

    /**
     * 사용자의 단골 가게 목록을 조회합니다.
     *
     * @param userId 유저 ID
     * @return 단골 가게 목록
     */
    @Transactional(readOnly = true)
    public List<FavouriteStoreResponse> getMyFavouriteStores(Long userId) {
        List<FavouriteStoreEntity> favourites = favouriteStoreRepository.findByUserId(userId);
        if (favourites.isEmpty()) {
            return List.of();
        }

        // storeId 목록 추출
        List<Long> storeIds = favourites.stream()
                .map(FavouriteStoreEntity::getStoreId)
                .toList();

        // 가게 일괄 조회 (활성 가게만)
        List<StoreEntity> activeStores = storeRepository.findByIdInAndIsActiveTrue(storeIds);
        Map<Long, StoreEntity> storeMap = activeStores.stream()
                .collect(Collectors.toMap(StoreEntity::getId, Function.identity()));

        // 활성 가게 ID만 추출하여 후속 쿼리에 사용
        List<Long> activeStoreIds = activeStores.stream()
                .map(StoreEntity::getId)
                .toList();

        // 빵 일괄 조회 → 가게별 그룹핑
        Map<Long, List<BreadEntity>> breadsByStore = breadRepository.findByStoreIdIn(activeStoreIds).stream()
                .collect(Collectors.groupingBy(BreadEntity::getStoreId));

        // 영업시간 일괄 조회 → 가게별 그룹핑
        Map<Long, List<StoreBusinessHoursEntity>> businessHoursMap = storeBusinessHoursRepository.findByStoreIdIn(activeStoreIds).stream()
                .collect(Collectors.groupingBy(StoreBusinessHoursEntity::getStoreId));

        // 이미지 일괄 조회 → 가게별 대표 이미지 추출
        Map<Long, String> primaryImageMap = buildPrimaryImageMap(activeStoreIds);

        // 응답 조립
        List<FavouriteStoreResponse> responses = new ArrayList<>();
        for (FavouriteStoreEntity favourite : favourites) {
            StoreEntity store = storeMap.get(favourite.getStoreId());
            if (store == null) {
                continue;
            }

            List<BreadEntity> breads = breadsByStore.getOrDefault(store.getId(), List.of());
            boolean hasStock = breads.stream().anyMatch(b -> b.getRemainingQuantity() > 0);
            boolean isSelling = SellingStatusUtil.isSelling(store.getIsActive(),
                    businessHoursMap.getOrDefault(store.getId(), List.of()), hasStock, clock);
            String address = store.getAddressLine1() + " " + store.getAddressLine2();
            String imageUrl = primaryImageMap.get(store.getId());

            responses.add(new FavouriteStoreResponse(
                    store.getId(),
                    store.getName(),
                    address,
                    imageUrl,
                    isSelling
            ));
        }

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
        Map<Long, String> result = new java.util.HashMap<>();
        for (StoreImageEntity image : allImages) {
            if (image.getDisplayOrder() == 0) {
                result.put(image.getStoreId(), fileStorage.getFileUrl(image.getStoredFilename()));
            }
        }
        return result;
    }

}
