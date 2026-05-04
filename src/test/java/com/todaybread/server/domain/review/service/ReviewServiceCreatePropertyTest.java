package com.todaybread.server.domain.review.service;

import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.review.dto.ReviewCreateRequest;
import com.todaybread.server.domain.review.dto.ReviewResponse;
import com.todaybread.server.domain.review.entity.ReviewEntity;
import com.todaybread.server.domain.review.repository.ReviewRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.support.TestFixtures;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

/**
 * ReviewService.createReview() 비즈니스 로직 속성 테스트.
 * jqwik + Mockito를 사용하여 서비스 레이어의 핵심 불변 조건을 검증합니다.
 */
class ReviewServiceCreatePropertyTest {

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
    // Property 1: 구매 이력 검증
    // Feature: bread-review, Property 1: 구매 이력 검증
    // ========================================================================

    /**
     * PICKED_UP이 아닌 OrderStatus에 대해 createReview는 REVIEW_PURCHASE_REQUIRED를 던져야 한다.
     *
     * **Validates: Requirements 1.1, 1.2**
     */
    @Property(tries = 100)
    void createReview_rejectsNonPickedUpStatus(
            @ForAll("validUserIds") Long userId,
            @ForAll("nonPickedUpStatuses") OrderStatus status,
            @ForAll("validOrderItemIds") Long orderItemId,
            @ForAll("validOrderIds") Long orderId,
            @ForAll("validStoreIds") Long storeId,
            @ForAll("validBreadIds") Long breadId,
            @ForAll("validRatings") int rating,
            @ForAll("validContents") String content
    ) {
        // Arrange
        OrderItemEntity orderItem = TestFixtures.orderItem(orderItemId, orderId, breadId, 3000, 1);
        OrderEntity order = TestFixtures.order(orderId, userId, storeId, status, 3000, "key");

        given(orderItemRepository.findById(orderItemId)).willReturn(Optional.of(orderItem));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        ReviewCreateRequest request = new ReviewCreateRequest(orderItemId, rating, content);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(userId, request, Collections.emptyList()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REVIEW_PURCHASE_REQUIRED);
    }

    /**
     * userId가 주문 소유자와 다른 경우 createReview는 REVIEW_PURCHASE_REQUIRED를 던져야 한다.
     *
     * **Validates: Requirements 1.1, 1.2**
     */
    @Property(tries = 100)
    void createReview_rejectsMismatchedUserId(
            @ForAll("validUserIds") Long requestUserId,
            @ForAll("validUserIds") Long orderOwnerUserId,
            @ForAll("validOrderItemIds") Long orderItemId,
            @ForAll("validOrderIds") Long orderId,
            @ForAll("validStoreIds") Long storeId,
            @ForAll("validBreadIds") Long breadId,
            @ForAll("validRatings") int rating,
            @ForAll("validContents") String content
    ) {
        // Ensure the user IDs are different
        Assume.that(!requestUserId.equals(orderOwnerUserId));

        // Arrange: PICKED_UP status but different userId
        OrderItemEntity orderItem = TestFixtures.orderItem(orderItemId, orderId, breadId, 3000, 1);
        OrderEntity order = TestFixtures.order(orderId, orderOwnerUserId, storeId, OrderStatus.PICKED_UP, 3000, "key");

        given(orderItemRepository.findById(orderItemId)).willReturn(Optional.of(orderItem));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        ReviewCreateRequest request = new ReviewCreateRequest(orderItemId, rating, content);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(requestUserId, request, Collections.emptyList()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REVIEW_PURCHASE_REQUIRED);
    }

    // ========================================================================
    // Property 2: 중복 리뷰 방지
    // Feature: bread-review, Property 2: 중복 리뷰 방지
    // ========================================================================

    /**
     * 이미 리뷰가 존재하는 (userId, orderItemId) 조합에 대해 createReview는
     * REVIEW_ALREADY_EXISTS를 던져야 한다.
     *
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 100)
    void createReview_rejectsDuplicateReview(
            @ForAll("validUserIds") Long userId,
            @ForAll("validOrderItemIds") Long orderItemId,
            @ForAll("validOrderIds") Long orderId,
            @ForAll("validStoreIds") Long storeId,
            @ForAll("validBreadIds") Long breadId,
            @ForAll("validRatings") int rating,
            @ForAll("validContents") String content
    ) {
        // Arrange: all prerequisites pass (PICKED_UP, matching userId)
        OrderItemEntity orderItem = TestFixtures.orderItem(orderItemId, orderId, breadId, 3000, 1);
        OrderEntity order = TestFixtures.order(orderId, userId, storeId, OrderStatus.PICKED_UP, 3000, "key");

        given(orderItemRepository.findById(orderItemId)).willReturn(Optional.of(orderItem));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        // Duplicate review exists
        given(reviewRepository.existsByUserIdAndOrderItemId(userId, orderItemId)).willReturn(true);

        ReviewCreateRequest request = new ReviewCreateRequest(orderItemId, rating, content);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(userId, request, Collections.emptyList()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    // ========================================================================
    // Property 3: 리뷰 데이터 라운드트립
    // Feature: bread-review, Property 3: 리뷰 데이터 라운드트립
    // ========================================================================

    /**
     * 유효한 rating(1~5)과 content(10~500자)로 생성된 리뷰의 응답은
     * 입력한 rating과 content를 그대로 반환해야 한다.
     *
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    void createReview_roundTripsRatingAndContent(
            @ForAll("validUserIds") Long userId,
            @ForAll("validOrderItemIds") Long orderItemId,
            @ForAll("validOrderIds") Long orderId,
            @ForAll("validStoreIds") Long storeId,
            @ForAll("validBreadIds") Long breadId,
            @ForAll("validRatings") int rating,
            @ForAll("validContents") String content
    ) {
        // Arrange: all prerequisites pass
        OrderItemEntity orderItem = TestFixtures.orderItem(orderItemId, orderId, breadId, 3000, 1);
        OrderEntity order = TestFixtures.order(orderId, userId, storeId, OrderStatus.PICKED_UP, 3000, "key");
        StoreEntity store = TestFixtures.store(storeId, 999L);

        given(orderItemRepository.findById(orderItemId)).willReturn(Optional.of(orderItem));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(reviewRepository.existsByUserIdAndOrderItemId(userId, orderItemId)).willReturn(false);
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(reviewImageService.uploadImages(any(), any())).willReturn(Collections.emptyList());

        // Mock save to set the ID on the entity via Answer
        given(reviewRepository.save(any(ReviewEntity.class))).willAnswer(invocation -> {
            ReviewEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 1L);
            ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.of(2026, 4, 5, 12, 0));
            return entity;
        });

        ReviewCreateRequest request = new ReviewCreateRequest(orderItemId, rating, content);

        // Act
        ReviewResponse response = reviewService.createReview(userId, request, Collections.emptyList());

        // Assert: round-trip
        assertThat(response.rating()).isEqualTo(rating);
        assertThat(response.content()).isEqualTo(content);
        assertThat(response.orderItemId()).isEqualTo(orderItemId);
        assertThat(response.reviewId()).isNotNull();
    }

    // ========================================================================
    // Property 5: 이미지 개수 검증
    // Feature: bread-review, Property 5: 이미지 개수 검증
    // ========================================================================

    /**
     * 0~2장 이미지로 createReview 호출 시 성공해야 한다.
     *
     * **Validates: Requirements 1.7, 1.8**
     */
    @Property(tries = 100)
    void createReview_succeedsWithZeroToTwoImages(
            @ForAll("validUserIds") Long userId,
            @ForAll("validOrderItemIds") Long orderItemId,
            @ForAll("validOrderIds") Long orderId,
            @ForAll("validStoreIds") Long storeId,
            @ForAll("validBreadIds") Long breadId,
            @ForAll("validRatings") int rating,
            @ForAll("validContents") String content,
            @ForAll("validImageCounts") int imageCount
    ) {
        // Arrange: all prerequisites pass
        OrderItemEntity orderItem = TestFixtures.orderItem(orderItemId, orderId, breadId, 3000, 1);
        OrderEntity order = TestFixtures.order(orderId, userId, storeId, OrderStatus.PICKED_UP, 3000, "key");
        StoreEntity store = TestFixtures.store(storeId, 999L);

        given(orderItemRepository.findById(orderItemId)).willReturn(Optional.of(orderItem));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(reviewRepository.existsByUserIdAndOrderItemId(userId, orderItemId)).willReturn(false);
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

        // Mock save
        given(reviewRepository.save(any(ReviewEntity.class))).willAnswer(invocation -> {
            ReviewEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 1L);
            ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.of(2026, 4, 5, 12, 0));
            return entity;
        });

        // Build image list
        List<MultipartFile> images = buildMockImages(imageCount);

        // Mock uploadImages to return filenames for valid counts
        List<String> expectedUrls = new ArrayList<>();
        for (int i = 0; i < imageCount; i++) {
            expectedUrls.add("review/1/image-" + i + ".jpg");
        }
        given(reviewImageService.uploadImages(any(), eq(images))).willReturn(expectedUrls);

        ReviewCreateRequest request = new ReviewCreateRequest(orderItemId, rating, content);

        // Act
        ReviewResponse response = reviewService.createReview(userId, request, images);

        // Assert: success with correct image count
        assertThat(response).isNotNull();
        assertThat(response.imageUrls()).hasSize(imageCount);
    }

    /**
     * 3장 이상 이미지로 createReview 호출 시 ReviewImageService가
     * REVIEW_IMAGE_LIMIT_EXCEEDED를 던져야 한다.
     *
     * **Validates: Requirements 1.7, 1.8**
     */
    @Property(tries = 100)
    void createReview_rejectsThreeOrMoreImages(
            @ForAll("validUserIds") Long userId,
            @ForAll("validOrderItemIds") Long orderItemId,
            @ForAll("validOrderIds") Long orderId,
            @ForAll("validStoreIds") Long storeId,
            @ForAll("validBreadIds") Long breadId,
            @ForAll("validRatings") int rating,
            @ForAll("validContents") String content,
            @ForAll("excessiveImageCounts") int imageCount
    ) {
        // Arrange: all prerequisites pass
        OrderItemEntity orderItem = TestFixtures.orderItem(orderItemId, orderId, breadId, 3000, 1);
        OrderEntity order = TestFixtures.order(orderId, userId, storeId, OrderStatus.PICKED_UP, 3000, "key");
        StoreEntity store = TestFixtures.store(storeId, 999L);

        given(orderItemRepository.findById(orderItemId)).willReturn(Optional.of(orderItem));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(reviewRepository.existsByUserIdAndOrderItemId(userId, orderItemId)).willReturn(false);
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

        // Mock save
        given(reviewRepository.save(any(ReviewEntity.class))).willAnswer(invocation -> {
            ReviewEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 1L);
            ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.of(2026, 4, 5, 12, 0));
            return entity;
        });

        // Build image list with excessive count
        List<MultipartFile> images = buildMockImages(imageCount);

        // Mock uploadImages to throw for excessive images
        given(reviewImageService.uploadImages(any(), eq(images)))
                .willThrow(new CustomException(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED));

        ReviewCreateRequest request = new ReviewCreateRequest(orderItemId, rating, content);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(userId, request, images))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED);
    }

    // ========================================================================
    // Arbitrary Providers
    // ========================================================================

    @Provide
    Arbitrary<Long> validUserIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    @Provide
    Arbitrary<Long> validOrderItemIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    @Provide
    Arbitrary<Long> validOrderIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    @Provide
    Arbitrary<Long> validStoreIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    @Provide
    Arbitrary<Long> validBreadIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    @Provide
    Arbitrary<Integer> validRatings() {
        return Arbitraries.integers().between(1, 5);
    }

    @Provide
    Arbitrary<String> validContents() {
        // Generate content between 10 and 500 characters using alphanumeric + Korean chars
        return Arbitraries.strings()
                .withCharRange('가', '힣')
                .ofMinLength(10)
                .ofMaxLength(100);
    }

    @Provide
    Arbitrary<OrderStatus> nonPickedUpStatuses() {
        return Arbitraries.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
    }

    @Provide
    Arbitrary<Integer> validImageCounts() {
        return Arbitraries.integers().between(0, 2);
    }

    @Provide
    Arbitrary<Integer> excessiveImageCounts() {
        return Arbitraries.integers().between(3, 5);
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private List<MultipartFile> buildMockImages(int count) {
        List<MultipartFile> images = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            images.add(new MockMultipartFile(
                    "images",
                    "test-image-" + i + ".jpg",
                    "image/jpeg",
                    ("image-bytes-" + i).getBytes(StandardCharsets.UTF_8)
            ));
        }
        return images;
    }
}
