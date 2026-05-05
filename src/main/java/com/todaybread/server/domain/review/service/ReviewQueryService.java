package com.todaybread.server.domain.review.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.bread.service.BreadImageService;
import com.todaybread.server.domain.order.dto.PurchaseCountProjection;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.review.dto.BossReviewFilterType;
import com.todaybread.server.domain.review.dto.BossReviewResponse;
import com.todaybread.server.domain.review.dto.BossReviewSortType;
import com.todaybread.server.domain.review.dto.MyReviewResponse;
import com.todaybread.server.domain.review.dto.MyReviewSortType;
import com.todaybread.server.domain.review.dto.ReviewSortType;
import com.todaybread.server.domain.review.dto.StoreRatingInfo;
import com.todaybread.server.domain.review.dto.StoreReviewResponse;
import com.todaybread.server.domain.review.entity.ReviewEntity;
import com.todaybread.server.domain.review.repository.ReviewRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 리뷰 조회 전용 서비스 계층입니다.
 * 가게 리뷰 목록, 사장님 리뷰 관리, 내 리뷰 목록, 평점 조회 등의 읽기 전용 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class ReviewQueryService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageService reviewImageService;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final BreadRepository breadRepository;
    private final BreadImageService breadImageService;
    private final OrderRepository orderRepository;

    /**
     * 가게 리뷰 목록을 조회합니다.
     * <p>
     * 정렬 옵션에 따라 최신순, 평점 높은 순, 평점 낮은 순으로 정렬하며,
     * N+1 문제를 방지하기 위해 관련 데이터를 일괄 조회합니다.
     *
     * @param storeId  가게 ID
     * @param sort     정렬 기준 (LATEST, RATING_HIGH, RATING_LOW)
     * @param pageable 페이지네이션 정보
     * @return 가게 리뷰 응답 페이지
     * @throws CustomException STORE_NOT_FOUND — 가게가 존재하지 않거나 비활성인 경우
     */
    @Transactional(readOnly = true)
    public Page<StoreReviewResponse> getStoreReviews(Long storeId, ReviewSortType sort, Pageable pageable) {
        // 0. 가게 존재 및 활성 여부 확인
        if (!storeRepository.existsByIdAndIsActiveTrue(storeId)) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }

        // 1. 정렬 기준 생성
        Sort sortOrder = createSort(sort);

        // 2. 정렬이 적용된 PageRequest 생성
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOrder);

        // 3. 리뷰 조회
        Page<ReviewEntity> reviewPage = reviewRepository.findByStoreId(storeId, pageRequest);

        if (reviewPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 4. 관련 데이터 일괄 조회 (N+1 방지)
        List<ReviewEntity> reviews = reviewPage.getContent();
        List<Long> reviewIds = reviews.stream().map(ReviewEntity::getId).toList();
        List<Long> userIds = reviews.stream().map(ReviewEntity::getUserId).distinct().toList();
        List<Long> breadIds = reviews.stream().map(ReviewEntity::getBreadId).distinct().toList();

        // 닉네임 매핑
        Map<Long, String> nicknameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));

        // 빵 이름 매핑
        Map<Long, String> breadNameMap = breadRepository.findAllById(breadIds).stream()
                .collect(Collectors.toMap(BreadEntity::getId, BreadEntity::getName));

        // 빵 대표 이미지 URL 매핑
        Map<Long, String> breadImageUrlMap = breadImageService.getImageUrls(breadIds);

        // 이미지 URL 매핑
        Map<Long, List<String>> imageUrlsMap = reviewImageService.getImageUrlsByReviewIds(reviewIds);

        // 5. 응답 매핑
        return reviewPage.map(review -> new StoreReviewResponse(
                review.getId(),
                nicknameMap.getOrDefault(review.getUserId(), "알 수 없음"),
                review.getRating(),
                review.getContent(),
                breadNameMap.getOrDefault(review.getBreadId(), "알 수 없음"),
                breadImageUrlMap.get(review.getBreadId()),
                imageUrlsMap.getOrDefault(review.getId(), Collections.emptyList()),
                review.getCreatedAt()
        ));
    }

    /**
     * ReviewSortType에 따른 Sort 객체를 생성합니다.
     *
     * @param sort 정렬 기준
     * @return Sort 객체
     */
    private Sort createSort(ReviewSortType sort) {
        return switch (sort) {
            case RATING_HIGH -> Sort.by("rating").descending().and(Sort.by("createdAt").descending());
            case RATING_LOW -> Sort.by("rating").ascending().and(Sort.by("createdAt").descending());
            default -> Sort.by("createdAt").descending();
        };
    }

    /**
     * 사장님 가게 리뷰 관리 목록을 조회합니다.
     * <p>
     * Boss의 userId로 가게를 조회하고, 정렬/필터 옵션에 따라 리뷰를 반환합니다.
     * 각 리뷰 작성자의 해당 가게 구매 횟수(purchaseCount)를 함께 포함합니다.
     *
     * @param userId   사장님 유저 ID
     * @param sort     정렬 기준 (LATEST, OLDEST, RATING_HIGH, RATING_LOW)
     * @param filter   필터 기준 (ALL, WITH_IMAGE, TEXT_ONLY)
     * @param pageable 페이지네이션 정보
     * @return 사장님 리뷰 응답 페이지
     * @throws CustomException STORE_NOT_FOUND — 사장님의 활성 가게를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public Page<BossReviewResponse> getBossReviews(Long userId, BossReviewSortType sort,
                                                    BossReviewFilterType filter, Pageable pageable) {
        // 1. Boss의 가게 조회
        StoreEntity store = storeRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        Long storeId = store.getId();

        // 2. 정렬 기준 생성
        Sort sortOrder = createBossSort(sort);

        // 3. 정렬이 적용된 PageRequest 생성
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOrder);

        // 4. 필터에 따른 리뷰 조회
        Page<ReviewEntity> reviewPage = switch (filter) {
            case WITH_IMAGE -> reviewRepository.findByStoreIdWithImages(storeId, pageRequest);
            case TEXT_ONLY -> reviewRepository.findByStoreIdWithoutImages(storeId, pageRequest);
            default -> reviewRepository.findByStoreId(storeId, pageRequest);
        };

        if (reviewPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 5. 관련 데이터 일괄 조회 (N+1 방지)
        List<ReviewEntity> reviews = reviewPage.getContent();
        List<Long> reviewIds = reviews.stream().map(ReviewEntity::getId).toList();
        List<Long> userIds = reviews.stream().map(ReviewEntity::getUserId).distinct().toList();
        List<Long> breadIds = reviews.stream().map(ReviewEntity::getBreadId).distinct().toList();

        // 닉네임 매핑
        Map<Long, String> nicknameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));

        // 빵 이름 매핑
        Map<Long, String> breadNameMap = breadRepository.findAllById(breadIds).stream()
                .collect(Collectors.toMap(BreadEntity::getId, BreadEntity::getName));

        // 빵 대표 이미지 URL 매핑
        Map<Long, String> breadImageUrlMap = breadImageService.getImageUrls(breadIds);

        // 이미지 URL 매핑
        Map<Long, List<String>> imageUrlsMap = reviewImageService.getImageUrlsByReviewIds(reviewIds);

        // 6. 구매 횟수 일괄 조회
        Map<Long, Long> purchaseCountMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            List<PurchaseCountProjection> purchaseCounts = orderRepository.countByStoreIdAndStatusAndUserIdIn(
                    storeId, OrderStatus.PICKED_UP, userIds);
            for (PurchaseCountProjection pc : purchaseCounts) {
                purchaseCountMap.put(pc.getUserId(), pc.getPurchaseCount());
            }
        }

        // 7. 응답 매핑
        return reviewPage.map(review -> new BossReviewResponse(
                review.getId(),
                nicknameMap.getOrDefault(review.getUserId(), "알 수 없음"),
                review.getRating(),
                review.getContent(),
                breadNameMap.getOrDefault(review.getBreadId(), "알 수 없음"),
                breadImageUrlMap.get(review.getBreadId()),
                imageUrlsMap.getOrDefault(review.getId(), Collections.emptyList()),
                review.getCreatedAt(),
                purchaseCountMap.getOrDefault(review.getUserId(), 0L).intValue()
        ));
    }

    /**
     * BossReviewSortType에 따른 Sort 객체를 생성합니다.
     *
     * @param sort 정렬 기준
     * @return Sort 객체
     */
    private Sort createBossSort(BossReviewSortType sort) {
        return switch (sort) {
            case OLDEST -> Sort.by("createdAt").ascending();
            case RATING_HIGH -> Sort.by("rating").descending().and(Sort.by("createdAt").descending());
            case RATING_LOW -> Sort.by("rating").ascending().and(Sort.by("createdAt").descending());
            default -> Sort.by("createdAt").descending();
        };
    }

    /**
     * 내 리뷰 목록을 조회합니다.
     * <p>
     * 사용자가 작성한 모든 리뷰를 정렬 옵션에 따라 페이지네이션하여 반환합니다.
     * N+1 문제를 방지하기 위해 빵 이름, 가게 이름/ID, 이미지 URL을 일괄 조회합니다.
     *
     * @param userId   사용자 ID
     * @param sort     정렬 기준 (LATEST, OLDEST)
     * @param pageable 페이지네이션 정보
     * @return 내 리뷰 응답 페이지
     */
    @Transactional(readOnly = true)
    public Page<MyReviewResponse> getMyReviews(Long userId, MyReviewSortType sort, Pageable pageable) {
        // 1. 정렬 기준 생성
        Sort sortOrder = createMyReviewSort(sort);

        // 2. 정렬이 적용된 PageRequest 생성
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOrder);

        // 3. 리뷰 조회
        Page<ReviewEntity> reviewPage = reviewRepository.findByUserId(userId, pageRequest);

        if (reviewPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 4. 관련 데이터 일괄 조회 (N+1 방지)
        List<ReviewEntity> reviews = reviewPage.getContent();
        List<Long> reviewIds = reviews.stream().map(ReviewEntity::getId).toList();
        List<Long> breadIds = reviews.stream().map(ReviewEntity::getBreadId).distinct().toList();
        List<Long> storeIds = reviews.stream().map(ReviewEntity::getStoreId).distinct().toList();

        // 빵 이름 매핑
        Map<Long, String> breadNameMap = breadRepository.findAllById(breadIds).stream()
                .collect(Collectors.toMap(BreadEntity::getId, BreadEntity::getName));

        // 빵 대표 이미지 URL 매핑
        Map<Long, String> breadImageUrlMap = breadImageService.getImageUrls(breadIds);

        // 가게 이름 및 ID 매핑
        Map<Long, StoreEntity> storeMap = storeRepository.findAllById(storeIds).stream()
                .collect(Collectors.toMap(StoreEntity::getId, store -> store));

        // 이미지 URL 매핑
        Map<Long, List<String>> imageUrlsMap = reviewImageService.getImageUrlsByReviewIds(reviewIds);

        // 5. 응답 매핑
        return reviewPage.map(review -> new MyReviewResponse(
                review.getId(),
                breadNameMap.getOrDefault(review.getBreadId(), "알 수 없음"),
                breadImageUrlMap.get(review.getBreadId()),
                storeMap.containsKey(review.getStoreId())
                        ? storeMap.get(review.getStoreId()).getName()
                        : "알 수 없음",
                review.getStoreId(),
                review.getRating(),
                review.getContent(),
                imageUrlsMap.getOrDefault(review.getId(), Collections.emptyList()),
                review.getCreatedAt()
        ));
    }

    /**
     * MyReviewSortType에 따른 Sort 객체를 생성합니다.
     *
     * @param sort 정렬 기준
     * @return Sort 객체
     */
    private Sort createMyReviewSort(MyReviewSortType sort) {
        return switch (sort) {
            case OLDEST -> Sort.by("createdAt").ascending();
            default -> Sort.by("createdAt").descending();
        };
    }

    /**
     * 단일 가게의 평점 정보를 조회합니다.
     * <p>
     * StoreEntity에 비정규화된 ratingSum과 reviewCount를 기반으로
     * 평균 평점과 리뷰 총 개수를 반환합니다.
     *
     * @param storeId 가게 ID
     * @return 가게 평점 정보 (평균 평점, 리뷰 총 개수)
     * @throws CustomException STORE_NOT_FOUND — 가게를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public StoreRatingInfo getStoreRating(Long storeId) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
        return new StoreRatingInfo(store.getAverageRating(), store.getReviewCount());
    }

    /**
     * 여러 가게의 평점 정보를 일괄 조회합니다. (N+1 방지)
     * <p>
     * 근처 빵/가게 목록 등 다수의 가게 평점을 한 번에 조회해야 하는 경우에 사용합니다.
     * StoreEntity에 비정규화된 ratingSum과 reviewCount를 기반으로 계산합니다.
     *
     * @param storeIds 가게 ID 목록
     * @return 가게 ID를 키로, 평점 정보를 값으로 하는 맵 (존재하지 않는 가게 ID는 포함되지 않음)
     */
    @Transactional(readOnly = true)
    public Map<Long, StoreRatingInfo> getStoreRatings(List<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return storeRepository.findAllById(storeIds).stream()
                .collect(Collectors.toMap(
                        StoreEntity::getId,
                        store -> new StoreRatingInfo(store.getAverageRating(), store.getReviewCount())
                ));
    }
}
