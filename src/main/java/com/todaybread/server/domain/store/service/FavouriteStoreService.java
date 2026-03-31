package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.store.dto.FavouriteStoreResponse;
import com.todaybread.server.domain.store.dto.FavouriteStoreToggleRequest;
import com.todaybread.server.domain.store.dto.FavouriteStoreToggleResponse;
import com.todaybread.server.domain.store.dto.StoreImageResponse;
import com.todaybread.server.domain.store.entity.FavouriteStoreEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.FavouriteStoreRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final StoreImageService storeImageService;

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

        storeRepository.findByIdAndIsActiveTrue(request.storeId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        if (favouriteStoreRepository.countByUserId(userId) >= MAX_FAVOURITE_STORES) {
            throw new CustomException(ErrorCode.FAVOURITE_STORE_LIMIT_EXCEEDED);
        }

        FavouriteStoreEntity entity = FavouriteStoreEntity.builder()
                .userId(userId)
                .storeId(request.storeId())
                .build();
        favouriteStoreRepository.save(entity);

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
        List<FavouriteStoreResponse> responses = new ArrayList<>();

        for (FavouriteStoreEntity favourite : favourites) {
            Optional<StoreEntity> storeOptional = storeRepository.findById(favourite.getStoreId());
            if (storeOptional.isEmpty()) {
                continue;
            }
            StoreEntity store = storeOptional.get();

            List<BreadEntity> breads = breadRepository.findByStoreId(store.getId());
            boolean isSelling = calculateIsSelling(store, breads);

            String address = store.getAddressLine1() + " " + store.getAddressLine2();
            String imageUrl = getPrimaryImageUrl(store.getId());

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
     * 판매중 여부를 판별합니다.
     * isActive가 true이고, 재고가 있는 빵이 1개 이상이며, 현재 시간이 라스트 오더 시간 이전이면 판매중입니다.
     *
     * @param store  가게 엔티티
     * @param breads 해당 가게의 빵 목록
     * @return 판매중이면 true, 아니면 false
     */
    private boolean calculateIsSelling(StoreEntity store, List<BreadEntity> breads) {
        if (!store.getIsActive()) {
            return false;
        }

        boolean hasStock = breads.stream()
                .anyMatch(b -> b.getRemainingQuantity() > 0);
        if (!hasStock) {
            return false;
        }

        Time now = Time.valueOf(LocalTime.now());
        return now.before(store.getLastOrderTime());
    }

    /**
     * 가게의 대표 이미지 URL을 조회합니다.
     * displayOrder=0인 이미지의 URL을 반환하며, 없으면 null을 반환합니다.
     *
     * @param storeId 가게 ID
     * @return 대표 이미지 URL (없으면 null)
     */
    private String getPrimaryImageUrl(Long storeId) {
        List<StoreImageResponse> images = storeImageService.getImagesByStoreId(storeId);
        for (StoreImageResponse image : images) {
            if (image.displayOrder() == 0) {
                return image.imageUrl();
            }
        }
        return null;
    }
}
