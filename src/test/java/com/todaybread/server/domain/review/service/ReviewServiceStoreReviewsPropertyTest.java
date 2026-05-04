package com.todaybread.server.domain.review.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.review.dto.ReviewSortType;
import com.todaybread.server.domain.review.dto.StoreReviewResponse;
import com.todaybread.server.domain.review.entity.ReviewEntity;
import com.todaybread.server.domain.review.repository.ReviewRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * ReviewService.getStoreReviews() 비즈니스 로직 속성 테스트.
 * jqwik + Mockito를 사용하여 가게 리뷰 조회의 핵심 불변 조건을 검증합니다.
 *
 * - Property 6: 가게 리뷰 필터링
 * - Property 7: 가게 리뷰 정렬
 * - Property 8: 리뷰 응답 필수 필드
 */
class ReviewServiceStoreReviewsPropertyTest {

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
    // Property 6: 가게 리뷰 필터링
    // Feature: bread-review, Property 6: 가게 리뷰 필터링
    // ========================================================================

    /**
     * For any storeId, all reviews returned by getStoreReviews should have
     * storeId matching the requested storeId. The repository is mocked to return
     * only reviews with the matching storeId, and the service should faithfully
     * return all of them without mixing in reviews from other stores.
     *
     * **Validates: Requirements 3.1**
     */
    @Property(tries = 100)
    void getStoreReviews_allReviewsBelongToRequestedStore(
            @ForAll("validStoreIds") Long storeId,
            @ForAll("reviewCounts") int reviewCount
    ) {
        // Arrange: create reviews all belonging to the requested storeId
        List<ReviewEntity> reviews = buildReviewEntities(storeId, reviewCount);
        Page<ReviewEntity> reviewPage = new PageImpl<>(reviews, PageRequest.of(0, 20), reviews.size());

        given(reviewRepository.findByStoreId(eq(storeId), any(Pageable.class))).willReturn(reviewPage);

        if (!reviews.isEmpty()) {
            mockRelatedData(reviews);
        }

        Pageable pageable = PageRequest.of(0, 20);

        // Act
        Page<StoreReviewResponse> result = reviewService.getStoreReviews(storeId, ReviewSortType.LATEST, pageable);

        // Assert: the number of returned reviews matches exactly what the repository returned
        assertThat(result.getContent()).hasSize(reviewCount);

        // Assert: all source review entities had the correct storeId
        for (ReviewEntity review : reviews) {
            assertThat(review.getStoreId()).isEqualTo(storeId);
        }
    }

    // ========================================================================
    // Property 7: 가게 리뷰 정렬
    // Feature: bread-review, Property 7: 가게 리뷰 정렬
    // ========================================================================

    /**
     * For LATEST sort: the Sort parameter passed to the repository should order by createdAt descending.
     * For RATING_HIGH sort: the Sort parameter should order by rating descending.
     * For RATING_LOW sort: the Sort parameter should order by rating ascending.
     *
     * **Validates: Requirements 3.3, 3.4, 3.5**
     */
    @Property(tries = 100)
    void getStoreReviews_appliesCorrectSortOrder(
            @ForAll("validStoreIds") Long storeId,
            @ForAll("allSortTypes") ReviewSortType sortType
    ) {
        // Arrange: create a few reviews
        List<ReviewEntity> reviews = buildReviewEntities(storeId, 3);
        Page<ReviewEntity> reviewPage = new PageImpl<>(reviews, PageRequest.of(0, 20), reviews.size());

        // Capture the Pageable via Answer to inspect the Sort
        List<Pageable> capturedPageables = new ArrayList<>();
        given(reviewRepository.findByStoreId(eq(storeId), any(Pageable.class))).willAnswer(invocation -> {
            capturedPageables.add(invocation.getArgument(1));
            return reviewPage;
        });
        mockRelatedData(reviews);

        Pageable pageable = PageRequest.of(0, 20);

        // Act
        reviewService.getStoreReviews(storeId, sortType, pageable);

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
    // Property 8: 리뷰 응답 필수 필드
    // Feature: bread-review, Property 8: 리뷰 응답 필수 필드
    // ========================================================================

    /**
     * For any review in the response, nickname, rating, content, breadName,
     * imageUrls, and createdAt should all be non-null.
     *
     * **Validates: Requirements 3.2, 4.9**
     */
    @Property(tries = 100)
    void getStoreReviews_allResponseFieldsAreNonNull(
            @ForAll("validStoreIds") Long storeId,
            @ForAll("nonEmptyReviewCounts") int reviewCount
    ) {
        // Arrange
        List<ReviewEntity> reviews = buildReviewEntities(storeId, reviewCount);
        Page<ReviewEntity> reviewPage = new PageImpl<>(reviews, PageRequest.of(0, 20), reviews.size());

        given(reviewRepository.findByStoreId(eq(storeId), any(Pageable.class))).willReturn(reviewPage);
        mockRelatedData(reviews);

        Pageable pageable = PageRequest.of(0, 20);

        // Act
        Page<StoreReviewResponse> result = reviewService.getStoreReviews(storeId, ReviewSortType.LATEST, pageable);

        // Assert: every response field must be non-null
        for (StoreReviewResponse response : result.getContent()) {
            assertThat(response.reviewId()).isNotNull();
            assertThat(response.nickname()).isNotNull();
            assertThat(response.content()).isNotNull();
            assertThat(response.breadName()).isNotNull();
            assertThat(response.imageUrls()).isNotNull();
            assertThat(response.createdAt()).isNotNull();
            // rating is a primitive int, always non-null
        }
    }

    // ========================================================================
    // Arbitrary Providers
    // ========================================================================

    @Provide
    Arbitrary<Long> validStoreIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    @Provide
    Arbitrary<Integer> reviewCounts() {
        return Arbitraries.integers().between(0, 10);
    }

    @Provide
    Arbitrary<Integer> nonEmptyReviewCounts() {
        return Arbitraries.integers().between(1, 10);
    }

    @Provide
    Arbitrary<ReviewSortType> allSortTypes() {
        return Arbitraries.of(ReviewSortType.LATEST, ReviewSortType.RATING_HIGH, ReviewSortType.RATING_LOW);
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
                    .content("리뷰 내용입니다 테스트 " + i)
                    .build();
            ReflectionTestUtils.setField(review, "id", reviewId);
            ReflectionTestUtils.setField(review, "createdAt",
                    LocalDateTime.of(2026, 4, 5, 12, 0).minusHours(i));
            reviews.add(review);
        }
        return reviews;
    }

    private void mockRelatedData(List<ReviewEntity> reviews) {
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
                .map(bid -> TestFixtures.bread(bid, 1L, 10, 5000, 3000))
                .toList();
        given(breadRepository.findAllById(breadIds)).willReturn(breads);

        // Mock reviewImageService.getImageUrlsByReviewIds
        Map<Long, List<String>> imageMap = reviewIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> List.of("https://example.com/review/" + id + "/image.jpg")
                ));
        given(reviewImageService.getImageUrlsByReviewIds(reviewIds)).willReturn(imageMap);
    }
}
