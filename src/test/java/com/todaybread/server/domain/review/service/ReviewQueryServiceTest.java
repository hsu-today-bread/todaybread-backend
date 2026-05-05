package com.todaybread.server.domain.review.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.bread.service.BreadImageService;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.review.dto.ReviewSortType;
import com.todaybread.server.domain.review.dto.StoreReviewResponse;
import com.todaybread.server.domain.review.entity.ReviewEntity;
import com.todaybread.server.domain.review.repository.ReviewRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * ReviewQueryService 단위 테스트.
 * 삭제된 빵의 과거 데이터 경로 접근 보장을 검증합니다.
 */
class ReviewQueryServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageService reviewImageService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BreadRepository breadRepository;

    @Mock
    private BreadImageService breadImageService;

    @Mock
    private OrderRepository orderRepository;

    private ReviewQueryService reviewQueryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reviewQueryService = new ReviewQueryService(reviewRepository, reviewImageService,
                storeRepository, userRepository, breadRepository, breadImageService, orderRepository);
    }

    /**
     * 삭제된 빵(isDeleted=true)이 포함된 리뷰 조회 시 빵 이름이 정상적으로 반환되는지 검증합니다.
     * ReviewQueryService는 breadRepository.findAllById()를 사용하므로 삭제된 빵도 조회됩니다.
     *
     * Validates: Requirements 6.2
     */
    @Test
    void getStoreReviews_삭제된빵_이름정상반환() {
        // Arrange
        Long storeId = 1L;
        Long breadId = 10L;
        Long userId = 100L;

        // 삭제된 빵 생성
        BreadEntity deletedBread = TestFixtures.bread(breadId, storeId, 5, 4_000, 2_000);
        deletedBread.softDelete(LocalDateTime.of(2026, 4, 1, 10, 0));

        // 리뷰 생성 (삭제된 빵에 대한 리뷰)
        ReviewEntity review = ReviewEntity.builder()
                .userId(userId)
                .storeId(storeId)
                .breadId(breadId)
                .orderItemId(300L)
                .rating(4)
                .content("맛있었어요 정말 좋았습니다")
                .build();
        ReflectionTestUtils.setField(review, "id", 1L);
        ReflectionTestUtils.setField(review, "createdAt", LocalDateTime.of(2026, 4, 5, 12, 0));

        Page<ReviewEntity> reviewPage = new PageImpl<>(List.of(review), PageRequest.of(0, 20), 1);

        given(storeRepository.existsByIdAndIsActiveTrue(storeId)).willReturn(true);
        given(reviewRepository.findByStoreId(eq(storeId), any(Pageable.class))).willReturn(reviewPage);
        given(userRepository.findAllById(List.of(userId)))
                .willReturn(List.of(TestFixtures.user(userId, false)));
        // findAllById는 삭제된 빵도 반환 (필터 없음)
        given(breadRepository.findAllById(List.of(breadId))).willReturn(List.of(deletedBread));
        given(breadImageService.getImageUrls(List.of(breadId))).willReturn(Collections.emptyMap());
        given(reviewImageService.getImageUrlsByReviewIds(List.of(1L))).willReturn(Collections.emptyMap());

        // Act
        Page<StoreReviewResponse> result = reviewQueryService.getStoreReviews(storeId, ReviewSortType.LATEST, PageRequest.of(0, 20));

        // Assert: 삭제된 빵의 이름이 정상적으로 반환됨
        assertThat(result.getContent()).hasSize(1);
        StoreReviewResponse response = result.getContent().get(0);
        assertThat(response.breadName()).isEqualTo(deletedBread.getName());
    }

    /**
     * 삭제된 빵(isDeleted=true)이 포함된 리뷰 조회 시 breadImageUrl이 정상적으로 반환되는지 검증합니다.
     * ReviewQueryService는 breadImageService.getImageUrls()를 사용하며,
     * BreadImageService는 is_deleted 여부와 무관하게 breadId 기반으로 이미지를 조회합니다.
     *
     * Validates: Requirements 6.6
     */
    @Test
    void getStoreReviews_삭제된빵_breadImageUrl정상반환() {
        // Arrange
        Long storeId = 1L;
        Long breadId = 10L;
        Long userId = 100L;
        String expectedImageUrl = "https://example.com/bread/10/image.jpg";

        // 삭제된 빵 생성
        BreadEntity deletedBread = TestFixtures.bread(breadId, storeId, 5, 4_000, 2_000);
        deletedBread.softDelete(LocalDateTime.of(2026, 4, 1, 10, 0));

        // 리뷰 생성 (삭제된 빵에 대한 리뷰)
        ReviewEntity review = ReviewEntity.builder()
                .userId(userId)
                .storeId(storeId)
                .breadId(breadId)
                .orderItemId(300L)
                .rating(5)
                .content("빵이 정말 맛있었습니다 추천합니다")
                .build();
        ReflectionTestUtils.setField(review, "id", 1L);
        ReflectionTestUtils.setField(review, "createdAt", LocalDateTime.of(2026, 4, 5, 12, 0));

        Page<ReviewEntity> reviewPage = new PageImpl<>(List.of(review), PageRequest.of(0, 20), 1);

        given(storeRepository.existsByIdAndIsActiveTrue(storeId)).willReturn(true);
        given(reviewRepository.findByStoreId(eq(storeId), any(Pageable.class))).willReturn(reviewPage);
        given(userRepository.findAllById(List.of(userId)))
                .willReturn(List.of(TestFixtures.user(userId, false)));
        given(breadRepository.findAllById(List.of(breadId))).willReturn(List.of(deletedBread));
        // BreadImageService는 삭제된 빵의 이미지도 정상 반환
        given(breadImageService.getImageUrls(List.of(breadId)))
                .willReturn(Map.of(breadId, expectedImageUrl));
        given(reviewImageService.getImageUrlsByReviewIds(List.of(1L))).willReturn(Collections.emptyMap());

        // Act
        Page<StoreReviewResponse> result = reviewQueryService.getStoreReviews(storeId, ReviewSortType.LATEST, PageRequest.of(0, 20));

        // Assert: 삭제된 빵의 대표 이미지 URL이 정상적으로 반환됨
        assertThat(result.getContent()).hasSize(1);
        StoreReviewResponse response = result.getContent().get(0);
        assertThat(response.breadImageUrl()).isEqualTo(expectedImageUrl);
    }
}
