package com.todaybread.server.domain.review.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.review.dto.BossReviewFilterType;
import com.todaybread.server.domain.review.dto.BossReviewResponse;
import com.todaybread.server.domain.review.dto.BossReviewSortType;
import com.todaybread.server.domain.review.entity.ReviewEntity;
import com.todaybread.server.domain.review.repository.ReviewRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.support.TestFixtures;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * ReviewService.getBossReviews() 비즈니스 로직 속성 테스트.
 * jqwik + Mockito를 사용하여 사장님 리뷰 관리 목록의 핵심 불변 조건을 검증합니다.
 *
 * - Property 9: 사장님 가게 소유권 검증
 * - Property 10: 사장님 리뷰 정렬
 * - Property 11: 사장님 리뷰 필터
 * - Property 12: 구매 횟수 정확성
 */
class ReviewServiceBossReviewsPropertyTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageService reviewImageService;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BreadRepository breadRepository;

    private ReviewService reviewService;

    @BeforeProperty
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reviewService = new ReviewService(reviewRepository, reviewImageService,
                orderItemRepository, orderRepository, storeRepository,
                userRepository, breadRepository);
    }

    // ========================================================================
    // Property 9: 사장님 가게 소유권 검증
    // Feature: bread-review, Property 9: 사장님 가게 소유권 검증
    // ========================================================================

    /**
     * 가게를 소유하지 않은 Boss(storeRepository가 empty 반환)에 대해
     * getBossReviews는 STORE_NOT_FOUND를 던져야 한다.
     *
     * **Validates: Requirements 4.1, 4.2**
     */
    @Property(tries = 100)
    void getBossReviews_rejectsNonOwner(
            @ForAll("validUserIds") Long userId
    ) {
        // Arrange: no active store for this userId
        given(storeRepository.findByUserIdAndIsActiveTrue(userId)).willReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 20);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.getBossReviews(userId, BossReviewSortType.LATEST,
                BossReviewFilterType.ALL, pageable))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    /**
     * 가게를 소유한 Boss(storeRepository가 store 반환)에 대해
     * getBossReviews는 정상적으로 결과를 반환해야 한다.
     *
     * **Validates: Requirements 4.1, 4.2**
     */
    @Property(tries = 100)
    void getBossReviews_allowsOwner(
            @ForAll("validUserIds") Long userId,
            @ForAll("validStoreIds") Long storeId
    ) {
        // Arrange: active store exists for this userId
        StoreEntity store = TestFixtures.store(storeId, userId);
        given(storeRepository.findByUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(store));

        // Return empty page (no reviews)
        Page<ReviewEntity> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        given(reviewRepository.findByStoreId(eq(storeId), any(Pageable.class))).willReturn(emptyPage);

        Pageable pageable = PageRequest.of(0, 20);

        // Act
        Page<BossReviewResponse> result = reviewService.getBossReviews(userId, BossReviewSortType.LATEST,
                BossReviewFilterType.ALL, pageable);

        // Assert: should succeed without exception
        assertThat(result).isNotNull();
    }

    // ========================================================================
    // Property 10: 사장님 리뷰 정렬
    // Feature: bread-review, Property 10: 사장님 리뷰 정렬
    // ========================================================================

    /**
     * 각 BossReviewSortType에 대해 올바른 Sort 파라미터가 리포지토리에 전달되는지 검증한다.
     * - LATEST → createdAt DESC
     * - OLDEST → createdAt ASC
     * - RATING_HIGH → rating DESC
     * - RATING_LOW → rating ASC
     *
     * **Validates: Requirements 4.3, 4.4, 4.5, 4.6**
     */
    @Property(tries = 100)
    void getBossReviews_appliesCorrectSortOrder(
            @ForAll("validUserIds") Long userId,
            @ForAll("validStoreIds") Long storeId,
            @ForAll("allBossSortTypes") BossReviewSortType sortType
    ) {
        // Arrange
        StoreEntity store = TestFixtures.store(storeId, userId);
        given(storeRepository.findByUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(store));

        List<ReviewEntity> reviews = buildReviewEntities(storeId, 3);
        Page<ReviewEntity> reviewPage = new PageImpl<>(reviews, PageRequest.of(0, 20), reviews.size());

        // Capture the Pageable to inspect the Sort
        List<Pageable> capturedPageables = new ArrayList<>();
        given(reviewRepository.findByStoreId(eq(storeId), any(Pageable.class))).willAnswer(invocation -> {
            capturedPageables.add(invocation.getArgument(1));
            return reviewPage;
        });
        mockRelatedData(reviews, storeId);

        Pageable pageable = PageRequest.of(0, 20);

        // Act
        reviewService.getBossReviews(userId, sortType, BossReviewFilterType.ALL, pageable);

        // Assert: verify the Sort passed to the repository
        assertThat(capturedPageables).isNotEmpty();
        Pageable capturedPageable = capturedPageables.get(capturedPageables.size() - 1);
        Sort capturedSort = capturedPageable.getSort();

        switch (sortType) {
            case LATEST -> {
                Sort.Order createdAtOrder = capturedSort.getOrderFor("createdAt");
                assertThat(createdAtOrder).isNotNull();
                assertThat(createdAtOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
            }
            case OLDEST -> {
                Sort.Order createdAtOrder = capturedSort.getOrderFor("createdAt");
                assertThat(createdAtOrder).isNotNull();
                assertThat(createdAtOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
            }
            case RATING_HIGH -> {
                Sort.Order ratingOrder = capturedSort.getOrderFor("rating");
                assertThat(ratingOrder).isNotNull();
                assertThat(ratingOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
            }
            case RATING_LOW -> {
                Sort.Order ratingOrder = capturedSort.getOrderFor("rating");
                assertThat(ratingOrder).isNotNull();
                assertThat(ratingOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
            }
        }
    }

    // ========================================================================
    // Property 11: 사장님 리뷰 필터
    // Feature: bread-review, Property 11: 사장님 리뷰 필터
    // ========================================================================

    /**
     * WITH_IMAGE 필터 시 findByStoreIdWithImages가 호출되고,
     * TEXT_ONLY 필터 시 findByStoreIdWithoutImages가 호출되고,
     * ALL 필터 시 findByStoreId가 호출되는지 검증한다.
     *
     * **Validates: Requirements 4.7, 4.8**
     */
    @Property(tries = 100)
    void getBossReviews_appliesCorrectFilter(
            @ForAll("validUserIds") Long userId,
            @ForAll("validStoreIds") Long storeId,
            @ForAll("allBossFilterTypes") BossReviewFilterType filterType
    ) {
        // Arrange — fresh mocks per try to avoid cross-try invocation counts
        MockitoAnnotations.openMocks(this);
        reviewService = new ReviewService(reviewRepository, reviewImageService,
                orderItemRepository, orderRepository, storeRepository,
                userRepository, breadRepository);

        StoreEntity store = TestFixtures.store(storeId, userId);
        given(storeRepository.findByUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(store));

        List<ReviewEntity> reviews = buildReviewEntities(storeId, 2);
        Page<ReviewEntity> reviewPage = new PageImpl<>(reviews, PageRequest.of(0, 20), reviews.size());

        // Stub all three repository methods so any can be called
        given(reviewRepository.findByStoreId(eq(storeId), any(Pageable.class))).willReturn(reviewPage);
        given(reviewRepository.findByStoreIdWithImages(eq(storeId), any(Pageable.class))).willReturn(reviewPage);
        given(reviewRepository.findByStoreIdWithoutImages(eq(storeId), any(Pageable.class))).willReturn(reviewPage);

        mockRelatedData(reviews, storeId);

        Pageable pageable = PageRequest.of(0, 20);

        // Act
        reviewService.getBossReviews(userId, BossReviewSortType.LATEST, filterType, pageable);

        // Assert: verify the correct repository method was called
        switch (filterType) {
            case WITH_IMAGE -> verify(reviewRepository).findByStoreIdWithImages(eq(storeId), any(Pageable.class));
            case TEXT_ONLY -> verify(reviewRepository).findByStoreIdWithoutImages(eq(storeId), any(Pageable.class));
            case ALL -> verify(reviewRepository).findByStoreId(eq(storeId), any(Pageable.class));
        }
    }

    // ========================================================================
    // Property 12: 구매 횟수 정확성
    // Feature: bread-review, Property 12: 구매 횟수 정확성
    // ========================================================================

    /**
     * orderRepository.countByStoreIdAndStatusAndUserIdIn이 반환하는 구매 횟수가
     * 각 BossReviewResponse.purchaseCount에 정확히 반영되는지 검증한다.
     *
     * **Validates: Requirements 4.10**
     */
    @Property(tries = 100)
    void getBossReviews_purchaseCountMatchesMockedCount(
            @ForAll("validUserIds") Long bossUserId,
            @ForAll("validStoreIds") Long storeId,
            @ForAll("reviewCounts") int reviewCount,
            @ForAll("purchaseCountValues") int basePurchaseCount
    ) {
        Assume.that(reviewCount > 0);

        // Arrange
        StoreEntity store = TestFixtures.store(storeId, bossUserId);
        given(storeRepository.findByUserIdAndIsActiveTrue(bossUserId)).willReturn(Optional.of(store));

        List<ReviewEntity> reviews = buildReviewEntities(storeId, reviewCount);
        Page<ReviewEntity> reviewPage = new PageImpl<>(reviews, PageRequest.of(0, 20), reviews.size());

        given(reviewRepository.findByStoreId(eq(storeId), any(Pageable.class))).willReturn(reviewPage);

        // Mock related data (users, breads, images)
        List<Long> userIds = reviews.stream().map(ReviewEntity::getUserId).distinct().toList();
        List<Long> breadIds = reviews.stream().map(ReviewEntity::getBreadId).distinct().toList();
        List<Long> reviewIds = reviews.stream().map(ReviewEntity::getId).toList();

        List<UserEntity> users = userIds.stream()
                .map(uid -> TestFixtures.user(uid, false))
                .toList();
        given(userRepository.findAllById(userIds)).willReturn(users);

        List<BreadEntity> breads = breadIds.stream()
                .map(bid -> TestFixtures.bread(bid, storeId, 10, 5000, 3000))
                .toList();
        given(breadRepository.findAllById(breadIds)).willReturn(breads);

        Map<Long, List<String>> imageMap = reviewIds.stream()
                .collect(Collectors.toMap(id -> id, id -> Collections.emptyList()));
        given(reviewImageService.getImageUrlsByReviewIds(reviewIds)).willReturn(imageMap);

        // Mock purchase counts: each user gets a distinct count based on their index
        List<Object[]> purchaseCounts = new ArrayList<>();
        Map<Long, Long> expectedPurchaseMap = new HashMap<>();
        for (int i = 0; i < userIds.size(); i++) {
            Long uid = userIds.get(i);
            long count = basePurchaseCount + i;
            purchaseCounts.add(new Object[]{uid, count});
            expectedPurchaseMap.put(uid, count);
        }
        given(orderRepository.countByStoreIdAndStatusAndUserIdIn(
                eq(storeId), eq(OrderStatus.PICKED_UP), eq(userIds)))
                .willReturn(purchaseCounts);

        Pageable pageable = PageRequest.of(0, 20);

        // Act
        Page<BossReviewResponse> result = reviewService.getBossReviews(bossUserId, BossReviewSortType.LATEST,
                BossReviewFilterType.ALL, pageable);

        // Assert: each response's purchaseCount matches the mocked count
        for (BossReviewResponse response : result.getContent()) {
            // Find the original review to get the userId
            ReviewEntity originalReview = reviews.stream()
                    .filter(r -> r.getId().equals(response.reviewId()))
                    .findFirst()
                    .orElseThrow();
            Long reviewUserId = originalReview.getUserId();
            int expectedCount = expectedPurchaseMap.getOrDefault(reviewUserId, 0L).intValue();
            assertThat(response.purchaseCount()).isEqualTo(expectedCount);
        }
    }

    // ========================================================================
    // Arbitrary Providers
    // ========================================================================

    @Provide
    Arbitrary<Long> validUserIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    @Provide
    Arbitrary<Long> validStoreIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    @Provide
    Arbitrary<BossReviewSortType> allBossSortTypes() {
        return Arbitraries.of(BossReviewSortType.LATEST, BossReviewSortType.OLDEST,
                BossReviewSortType.RATING_HIGH, BossReviewSortType.RATING_LOW);
    }

    @Provide
    Arbitrary<BossReviewFilterType> allBossFilterTypes() {
        return Arbitraries.of(BossReviewFilterType.ALL, BossReviewFilterType.WITH_IMAGE,
                BossReviewFilterType.TEXT_ONLY);
    }

    @Provide
    Arbitrary<Integer> reviewCounts() {
        return Arbitraries.integers().between(1, 10);
    }

    @Provide
    Arbitrary<Integer> purchaseCountValues() {
        return Arbitraries.integers().between(1, 50);
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private List<ReviewEntity> buildReviewEntities(Long storeId, int count) {
        List<ReviewEntity> reviews = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long reviewId = i + 1L;
            long userId = 100L + i;
            long breadId = 200L + i;
            int rating = (i % 5) + 1;

            ReviewEntity review = ReviewEntity.builder()
                    .userId(userId)
                    .storeId(storeId)
                    .breadId(breadId)
                    .orderItemId(300L + i)
                    .rating(rating)
                    .content("사장님 리뷰 테스트 내용입니다 " + i)
                    .build();
            ReflectionTestUtils.setField(review, "id", reviewId);
            ReflectionTestUtils.setField(review, "createdAt",
                    LocalDateTime.of(2026, 4, 5, 12, 0).minusHours(i));
            reviews.add(review);
        }
        return reviews;
    }

    private void mockRelatedData(List<ReviewEntity> reviews, Long storeId) {
        List<Long> userIds = reviews.stream().map(ReviewEntity::getUserId).distinct().toList();
        List<Long> breadIds = reviews.stream().map(ReviewEntity::getBreadId).distinct().toList();
        List<Long> reviewIds = reviews.stream().map(ReviewEntity::getId).toList();

        // Mock userRepository.findAllById
        List<UserEntity> users = userIds.stream()
                .map(uid -> TestFixtures.user(uid, false))
                .toList();
        given(userRepository.findAllById(userIds)).willReturn(users);

        // Mock breadRepository.findAllById
        List<BreadEntity> breads = breadIds.stream()
                .map(bid -> TestFixtures.bread(bid, storeId, 10, 5000, 3000))
                .toList();
        given(breadRepository.findAllById(breadIds)).willReturn(breads);

        // Mock reviewImageService.getImageUrlsByReviewIds
        Map<Long, List<String>> imageMap = reviewIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> List.of("https://example.com/review/" + id + "/image.jpg")
                ));
        given(reviewImageService.getImageUrlsByReviewIds(reviewIds)).willReturn(imageMap);

        // Mock orderRepository.countByStoreIdAndStatusAndUserIdIn (default: 0 purchases)
        given(orderRepository.countByStoreIdAndStatusAndUserIdIn(
                eq(storeId), eq(OrderStatus.PICKED_UP), eq(userIds)))
                .willReturn(Collections.emptyList());
    }
}
