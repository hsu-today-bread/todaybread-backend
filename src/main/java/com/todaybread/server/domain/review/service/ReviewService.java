package com.todaybread.server.domain.review.service;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.review.dto.ReviewCreateRequest;
import com.todaybread.server.domain.review.dto.ReviewResponse;
import com.todaybread.server.domain.review.entity.ReviewEntity;
import com.todaybread.server.domain.review.repository.ReviewRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

/**
 * 리뷰 도메인 서비스 계층입니다.
 * 리뷰 작성 비즈니스 로직을 처리합니다.
 * 조회 로직은 {@link ReviewQueryService}에서 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageService reviewImageService;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;

    /**
     * 리뷰를 작성합니다.
     * <p>
     * 주문 항목 존재 여부, 주문 상태(PICKED_UP), 주문 소유자 검증, 중복 리뷰 검사를 수행한 뒤
     * 리뷰를 저장하고 가게 평점을 원자적으로 갱신합니다. 이미지가 있으면 함께 저장합니다.
     *
     * @param userId  리뷰 작성자 ID
     * @param request 리뷰 작성 요청 (orderItemId, rating, content)
     * @param images  첨부 이미지 목록 (선택사항, 최대 2장)
     * @return 리뷰 작성 응답
     * @throws CustomException ORDER_NOT_FOUND — 주문 항목 또는 주문을 찾을 수 없는 경우
     * @throws CustomException REVIEW_PURCHASE_REQUIRED — PICKED_UP 상태가 아니거나 주문 소유자가 아닌 경우
     * @throws CustomException REVIEW_ALREADY_EXISTS — 동일 주문 항목에 대한 중복 리뷰인 경우
     */
    @Transactional
    public ReviewResponse createReview(Long userId, ReviewCreateRequest request, List<MultipartFile> images) {
        // 1. 주문 항목 조회
        OrderItemEntity orderItem = orderItemRepository.findById(request.orderItemId())
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 주문 조회
        OrderEntity order = orderRepository.findById(orderItem.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 3. 주문 상태 검증 (PICKED_UP만 리뷰 가능)
        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new CustomException(ErrorCode.REVIEW_PURCHASE_REQUIRED);
        }

        // 4. 주문 소유자 검증
        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.REVIEW_PURCHASE_REQUIRED);
        }

        // 5. 중복 리뷰 검사
        if (reviewRepository.existsByUserIdAndOrderItemId(userId, request.orderItemId())) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // 6. 리뷰 엔티티 생성
        ReviewEntity review = ReviewEntity.builder()
                .userId(userId)
                .storeId(order.getStoreId())
                .breadId(orderItem.getBreadId())
                .orderItemId(request.orderItemId())
                .rating(request.rating())
                .content(request.content())
                .build();

        // 7. 리뷰 저장
        reviewRepository.save(review);

        // 8. 가게 평점 원자적 갱신
        storeRepository.addReviewRating(order.getStoreId(), request.rating());

        // 9. 이미지 업로드
        List<String> imageUrls = reviewImageService.uploadImages(review.getId(), images);

        // 10. 응답 반환
        return new ReviewResponse(
                review.getId(),
                review.getOrderItemId(),
                review.getRating(),
                review.getContent(),
                imageUrls,
                review.getCreatedAt()
        );
    }
}
