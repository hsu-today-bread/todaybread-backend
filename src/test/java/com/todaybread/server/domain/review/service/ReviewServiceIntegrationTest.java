package com.todaybread.server.domain.review.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.bread.service.BreadImageService;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.review.dto.MyReviewResponse;
import com.todaybread.server.domain.review.dto.MyReviewSortType;
import com.todaybread.server.domain.review.dto.ReviewCreateRequest;
import com.todaybread.server.domain.review.dto.ReviewResponse;
import com.todaybread.server.domain.review.dto.ReviewSortType;
import com.todaybread.server.domain.review.dto.StoreReviewResponse;
import com.todaybread.server.domain.review.entity.ReviewEntity;
import com.todaybread.server.domain.review.repository.ReviewRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

/**
 * ReviewService 통합 테스트입니다.
 * Mockito를 사용하여 리뷰 작성 → 평점 갱신 → 조회 E2E 흐름을 검증합니다.
 *
 * _Requirements: 5.1, 5.2, 4.11_
 */
class ReviewServiceIntegrationTest {

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

    @Mock
    private BreadImageService breadImageService;

    private ReviewService reviewService;
    private ReviewQueryService reviewQueryService;

    // Shared test data
    private static final Long USER_ID = 1L;
    private static final Long STORE_ID = 10L;
    private static final Long BREAD_ID = 100L;
    private static final Long ORDER_ID = 1000L;
    private static final Long ORDER_ITEM_ID = 2000L;

    private StoreEntity store;
    private UserEntity user;
    private BreadEntity bread;
    private OrderEntity order;
    private OrderItemEntity orderItem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reviewService = new ReviewService(reviewRepository, reviewImageService,
                orderItemRepository, orderRepository, storeRepository);
        reviewQueryService = new ReviewQueryService(reviewRepository, reviewImageService,
                storeRepository, userRepository, breadRepository, breadImageService, orderRepository);

        store = TestFixtures.store(STORE_ID, 999L);
        user = TestFixtures.user(USER_ID, false);
        bread = TestFixtures.bread(BREAD_ID, STORE_ID, 10, 5000, 3000);
        order = TestFixtures.order(ORDER_ID, USER_ID, STORE_ID, OrderStatus.PICKED_UP, 3000, "key-1");
        orderItem = TestFixtures.orderItem(ORDER_ITEM_ID, ORDER_ID, BREAD_ID, 3000, 1);
    }

    @Test
    @DisplayName("리뷰 작성 시 ReviewResponse가 올바른 데이터로 반환된다")
    void createReview_returnsCorrectResponse() {
        // Arrange
        given(orderItemRepository.findById(ORDER_ITEM_ID)).willReturn(Optional.of(orderItem));
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
        given(reviewRepository.existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID)).willReturn(false);
        doNothing().when(storeRepository).addReviewRating(anyLong(), anyInt());
        given(reviewImageService.uploadImages(any(), any())).willReturn(Collections.emptyList());
        given(reviewRepository.save(any(ReviewEntity.class))).willAnswer(invocation -> {
            ReviewEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 1L);
            ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.of(2026, 4, 5, 12, 0));
            return entity;
        });

        ReviewCreateRequest request = new ReviewCreateRequest(ORDER_ITEM_ID, 4, "정말 맛있는 빵이었습니다! 추천합니다.");

        // Act
        ReviewResponse response = reviewService.createReview(USER_ID, request, Collections.emptyList());

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.reviewId()).isEqualTo(1L);
        assertThat(response.orderItemId()).isEqualTo(ORDER_ITEM_ID);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.content()).isEqualTo("정말 맛있는 빵이었습니다! 추천합니다.");
        assertThat(response.imageUrls()).isEmpty();
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("리뷰 작성 후 가게의 addReviewRating이 호출된다")
    void createReview_callsAtomicRatingUpdate() {
        // Arrange
        given(orderItemRepository.findById(ORDER_ITEM_ID)).willReturn(Optional.of(orderItem));
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
        given(reviewRepository.existsByUserIdAndOrderItemId(USER_ID, ORDER_ITEM_ID)).willReturn(false);
        doNothing().when(storeRepository).addReviewRating(anyLong(), anyInt());
        given(reviewImageService.uploadImages(any(), any())).willReturn(Collections.emptyList());
        given(reviewRepository.save(any(ReviewEntity.class))).willAnswer(invocation -> {
            ReviewEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 1L);
            ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.of(2026, 4, 5, 12, 0));
            return entity;
        });

        // Act: create review with rating 4
        ReviewCreateRequest request1 = new ReviewCreateRequest(ORDER_ITEM_ID, 4, "정말 맛있는 빵이었습니다! 추천합니다.");
        reviewService.createReview(USER_ID, request1, Collections.emptyList());

        // Assert: atomic update was called
        org.mockito.Mockito.verify(storeRepository).addReviewRating(STORE_ID, 4);
    }

    @Test
    @DisplayName("getStoreReviews로 작성된 리뷰가 목록에 나타난다")
    void getStoreReviews_returnsCreatedReview() {
        // Arrange: store exists and is active
        given(storeRepository.existsByIdAndIsActiveTrue(STORE_ID)).willReturn(true);

        // Arrange: create a review entity as if it was saved
        ReviewEntity review = ReviewEntity.builder()
                .userId(USER_ID)
                .storeId(STORE_ID)
                .breadId(BREAD_ID)
                .orderItemId(ORDER_ITEM_ID)
                .rating(4)
                .content("정말 맛있는 빵이었습니다! 추천합니다.")
                .build();
        ReflectionTestUtils.setField(review, "id", 1L);
        ReflectionTestUtils.setField(review, "createdAt", LocalDateTime.of(2026, 4, 5, 12, 0));

        Pageable pageable = PageRequest.of(0, 10);
        Page<ReviewEntity> reviewPage = new PageImpl<>(List.of(review), pageable, 1);

        given(reviewRepository.findByStoreId(eq(STORE_ID), any(PageRequest.class))).willReturn(reviewPage);
        given(userRepository.findAllById(List.of(USER_ID))).willReturn(List.of(user));
        given(breadRepository.findAllById(List.of(BREAD_ID))).willReturn(List.of(bread));
        given(breadImageService.getImageUrls(List.of(BREAD_ID))).willReturn(Collections.emptyMap());
        given(reviewImageService.getImageUrlsByReviewIds(List.of(1L))).willReturn(Collections.emptyMap());

        // Act
        Page<StoreReviewResponse> result = reviewQueryService.getStoreReviews(STORE_ID, ReviewSortType.LATEST, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        StoreReviewResponse storeReview = result.getContent().get(0);
        assertThat(storeReview.reviewId()).isEqualTo(1L);
        assertThat(storeReview.nickname()).isEqualTo("nick-1");
        assertThat(storeReview.rating()).isEqualTo(4);
        assertThat(storeReview.content()).isEqualTo("정말 맛있는 빵이었습니다! 추천합니다.");
        assertThat(storeReview.breadName()).isEqualTo("bread-100");
        assertThat(storeReview.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("getMyReviews로 작성된 리뷰가 storeId와 함께 목록에 나타난다")
    void getMyReviews_returnsCreatedReviewWithStoreId() {
        // Arrange
        ReviewEntity review = ReviewEntity.builder()
                .userId(USER_ID)
                .storeId(STORE_ID)
                .breadId(BREAD_ID)
                .orderItemId(ORDER_ITEM_ID)
                .rating(5)
                .content("최고의 빵집입니다! 매일 가고 싶어요.")
                .build();
        ReflectionTestUtils.setField(review, "id", 1L);
        ReflectionTestUtils.setField(review, "createdAt", LocalDateTime.of(2026, 4, 5, 12, 0));

        Pageable pageable = PageRequest.of(0, 10);
        Page<ReviewEntity> reviewPage = new PageImpl<>(List.of(review), pageable, 1);

        given(reviewRepository.findByUserId(eq(USER_ID), any(PageRequest.class))).willReturn(reviewPage);
        given(breadRepository.findAllById(List.of(BREAD_ID))).willReturn(List.of(bread));
        given(breadImageService.getImageUrls(List.of(BREAD_ID))).willReturn(Collections.emptyMap());
        given(storeRepository.findAllById(List.of(STORE_ID))).willReturn(List.of(store));
        given(reviewImageService.getImageUrlsByReviewIds(List.of(1L))).willReturn(Collections.emptyMap());

        // Act
        Page<MyReviewResponse> result = reviewQueryService.getMyReviews(USER_ID, MyReviewSortType.LATEST, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        MyReviewResponse myReview = result.getContent().get(0);
        assertThat(myReview.reviewId()).isEqualTo(1L);
        assertThat(myReview.storeId()).isEqualTo(STORE_ID);
        assertThat(myReview.storeName()).isEqualTo("store-10");
        assertThat(myReview.breadName()).isEqualTo("bread-100");
        assertThat(myReview.rating()).isEqualTo(5);
        assertThat(myReview.content()).isEqualTo("최고의 빵집입니다! 매일 가고 싶어요.");
    }

    @Test
    @DisplayName("페이지네이션: 여러 리뷰 생성 후 페이지 크기가 올바르게 동작한다")
    void pagination_worksCorrectlyWithMultipleReviews() {
        // Arrange: store exists and is active
        given(storeRepository.existsByIdAndIsActiveTrue(STORE_ID)).willReturn(true);

        // Arrange: 5 reviews, page size 2
        List<ReviewEntity> allReviews = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ReviewEntity review = ReviewEntity.builder()
                    .userId(USER_ID)
                    .storeId(STORE_ID)
                    .breadId(BREAD_ID)
                    .orderItemId(ORDER_ITEM_ID + i)
                    .rating((i % 5) + 1)
                    .content("리뷰 내용입니다 번호 " + i + " 입니다.")
                    .build();
            ReflectionTestUtils.setField(review, "id", (long) i);
            ReflectionTestUtils.setField(review, "createdAt", LocalDateTime.of(2026, 4, 5, 12, i));
            allReviews.add(review);
        }

        // Page 0: first 2 reviews
        Pageable page0 = PageRequest.of(0, 2);
        Page<ReviewEntity> reviewPage0 = new PageImpl<>(allReviews.subList(0, 2), page0, 5);

        given(reviewRepository.findByStoreId(eq(STORE_ID), any(PageRequest.class))).willReturn(reviewPage0);
        given(userRepository.findAllById(anyList())).willReturn(List.of(user));
        given(breadRepository.findAllById(anyList())).willReturn(List.of(bread));
        given(breadImageService.getImageUrls(anyList())).willReturn(Collections.emptyMap());
        given(reviewImageService.getImageUrlsByReviewIds(anyList())).willReturn(Collections.emptyMap());

        // Act
        Page<StoreReviewResponse> result0 = reviewQueryService.getStoreReviews(STORE_ID, ReviewSortType.LATEST, page0);

        // Assert: page 0
        assertThat(result0.getContent()).hasSize(2);
        assertThat(result0.getTotalElements()).isEqualTo(5);
        assertThat(result0.getTotalPages()).isEqualTo(3);
        assertThat(result0.getNumber()).isZero();

        // Page 2 (last page): 1 review
        Pageable page2 = PageRequest.of(2, 2);
        Page<ReviewEntity> reviewPage2 = new PageImpl<>(allReviews.subList(4, 5), page2, 5);

        given(reviewRepository.findByStoreId(eq(STORE_ID), any(PageRequest.class))).willReturn(reviewPage2);

        // Act
        Page<StoreReviewResponse> result2 = reviewQueryService.getStoreReviews(STORE_ID, ReviewSortType.LATEST, page2);

        // Assert: last page
        assertThat(result2.getContent()).hasSize(1);
        assertThat(result2.getTotalElements()).isEqualTo(5);
        assertThat(result2.isLast()).isTrue();
    }
}
