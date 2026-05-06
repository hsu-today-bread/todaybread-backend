package com.todaybread.server.domain.review.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.bread.service.BreadImageService;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.review.dto.MyReviewResponse;
import com.todaybread.server.domain.review.dto.MyReviewSortType;
import com.todaybread.server.domain.review.entity.ReviewEntity;
import com.todaybread.server.domain.review.repository.ReviewRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
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
 * ReviewQueryService.getMyReviews() 비즈니스 로직 속성 테스트.
 * jqwik + Mockito를 사용하여 내 리뷰 조회의 핵심 불변 조건을 검증합니다.
 *
 * - Property 14: 내 리뷰 필터링 및 정렬
 * - Property 15: 내 리뷰 응답 필수 필드
 */
class ReviewServiceMyReviewsPropertyTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageService reviewImageService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BreadRepository breadRepository;

    @Mock
    private BreadImageService breadImageService;

    private ReviewQueryService reviewQueryService;

    @BeforeProperty
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reviewQueryService = new ReviewQueryService(reviewRepository, reviewImageService,
                storeRepository, userRepository, breadRepository, breadImageService, orderRepository, orderItemRepository);
    }

    // ========================================================================
    // Property 14: 내 리뷰 필터링 및 정렬
    // Feature: bread-review, Property 14: 내 리뷰 필터링 및 정렬
    // ========================================================================

    /**
     * For any userId, all reviews returned by getMyReviews should belong to
     * the requested userId. The repository is mocked to return only reviews
     * with the matching userId, and the service should faithfully return all
     * of them without mixing in reviews from other users.
     *
     * **Validates: Requirements 6.1, 6.2**
     */
    @Property(tries = 100)
    void getMyReviews_allReviewsBelongToRequestedUser(
            @ForAll("validUserIds") Long userId,
            @ForAll("reviewCounts") int reviewCount
    ) {
        // Arrange: create reviews all belonging to the requested userId
        List<ReviewEntity> reviews = buildReviewEntities(userId, reviewCount);
        Page<ReviewEntity> reviewPage = new PageImpl<>(reviews, PageRequest.of(0, 20), reviews.size());

        given(reviewRepository.findByUserId(eq(userId), any(Pageable.class))).willReturn(reviewPage);

        if (!reviews.isEmpty()) {
            mockRelatedData(reviews);
        }

        Pageable pageable = PageRequest.of(0, 20);

        // Act
        Page<MyReviewResponse> result = reviewQueryService.getMyReviews(userId, MyReviewSortType.LATEST, pageable);

        // Assert: the number of returned reviews matches exactly what the repository returned
        assertThat(result.getContent()).hasSize(reviewCount);

        // Assert: all source review entities had the correct userId
        for (ReviewEntity review : reviews) {
            assertThat(review.getUserId()).isEqualTo(userId);
        }
    }

    /**
     * For LATEST sort: the Sort parameter passed to the repository should order
     * by createdAt descending.
     * For OLDEST sort: the Sort parameter should order by createdAt ascending.
     *
     * **Validates: Requirements 6.1, 6.2**
     */
    @Property(tries = 100)
    void getMyReviews_appliesCorrectSortOrder(
            @ForAll("validUserIds") Long userId,
            @ForAll("allMyReviewSortTypes") MyReviewSortType sortType
    ) {
        // Arrange: create a few reviews
        List<ReviewEntity> reviews = buildReviewEntities(userId, 3);
        Page<ReviewEntity> reviewPage = new PageImpl<>(reviews, PageRequest.of(0, 20), reviews.size());

        // Capture the Pageable via Answer to inspect the Sort
        List<Pageable> capturedPageables = new ArrayList<>();
        given(reviewRepository.findByUserId(eq(userId), any(Pageable.class))).willAnswer(invocation -> {
            capturedPageables.add(invocation.getArgument(1));
            return reviewPage;
        });
        mockRelatedData(reviews);

        Pageable pageable = PageRequest.of(0, 20);

        // Act
        reviewQueryService.getMyReviews(userId, sortType, pageable);

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
        }
    }

    // ========================================================================
    // Property 15: 내 리뷰 응답 필수 필드
    // Feature: bread-review, Property 15: 내 리뷰 응답 필수 필드
    // ========================================================================

    /**
     * For any review in the response, breadName, storeName, storeId, rating,
     * content, createdAt, and imageUrls should all be non-null.
     *
     * **Validates: Requirements 6.3**
     */
    @Property(tries = 100)
    void getMyReviews_allResponseFieldsAreNonNull(
            @ForAll("validUserIds") Long userId,
            @ForAll("nonEmptyReviewCounts") int reviewCount
    ) {
        // Arrange
        List<ReviewEntity> reviews = buildReviewEntities(userId, reviewCount);
        Page<ReviewEntity> reviewPage = new PageImpl<>(reviews, PageRequest.of(0, 20), reviews.size());

        given(reviewRepository.findByUserId(eq(userId), any(Pageable.class))).willReturn(reviewPage);
        mockRelatedData(reviews);

        Pageable pageable = PageRequest.of(0, 20);

        // Act
        Page<MyReviewResponse> result = reviewQueryService.getMyReviews(userId, MyReviewSortType.LATEST, pageable);

        // Assert: every response field must be non-null
        for (MyReviewResponse response : result.getContent()) {
            assertThat(response.reviewId()).isNotNull();
            assertThat(response.breadName()).isNotNull();
            assertThat(response.storeName()).isNotNull();
            assertThat(response.storeId()).isNotNull();
            assertThat(response.content()).isNotNull();
            assertThat(response.imageUrls()).isNotNull();
            assertThat(response.createdAt()).isNotNull();
            // rating is a primitive int, always non-null
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
    Arbitrary<Integer> reviewCounts() {
        return Arbitraries.integers().between(0, 10);
    }

    @Provide
    Arbitrary<Integer> nonEmptyReviewCounts() {
        return Arbitraries.integers().between(1, 10);
    }

    @Provide
    Arbitrary<MyReviewSortType> allMyReviewSortTypes() {
        return Arbitraries.of(MyReviewSortType.LATEST, MyReviewSortType.OLDEST);
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private List<ReviewEntity> buildReviewEntities(Long userId, int count) {
        List<ReviewEntity> reviews = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long reviewId = i + 1L;
            long storeId = 100L + i;
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
        List<Long> breadIds = reviews.stream().map(ReviewEntity::getBreadId).distinct().toList();
        List<Long> storeIds = reviews.stream().map(ReviewEntity::getStoreId).distinct().toList();
        List<Long> reviewIds = reviews.stream().map(ReviewEntity::getId).toList();
        List<Long> orderItemIds = reviews.stream().map(ReviewEntity::getOrderItemId).distinct().toList();

        // Mock orderItemRepository.findAllById (breadName snapshot)
        List<OrderItemEntity> orderItems = orderItemIds.stream()
                .map(oiId -> TestFixtures.orderItem(oiId, 1000L, 200L + oiId, 3000, 1))
                .toList();
        given(orderItemRepository.findAllById(orderItemIds)).willReturn(orderItems);

        // Mock breadImageService.getImageUrls
        Map<Long, String> breadImageMap = breadIds.stream()
                .collect(Collectors.toMap(bid -> bid, bid -> "https://example.com/bread/" + bid + "/image.jpg"));
        given(breadImageService.getImageUrls(breadIds)).willReturn(breadImageMap);

        // Mock storeRepository.findAllById
        List<StoreEntity> stores = storeIds.stream()
                .map(sid -> TestFixtures.store(sid, 999L))
                .toList();
        given(storeRepository.findAllById(storeIds)).willReturn(stores);

        // Mock reviewImageService.getImageUrlsByReviewIds
        Map<Long, List<String>> imageMap = reviewIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> List.of("https://example.com/review/" + id + "/image.jpg")
                ));
        given(reviewImageService.getImageUrlsByReviewIds(reviewIds)).willReturn(imageMap);
    }
}
