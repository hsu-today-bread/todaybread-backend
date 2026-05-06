package com.todaybread.server.domain.review.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 리뷰 작성 응답 DTO입니다.
 *
 * @param reviewId 리뷰 ID
 * @param orderItemId 주문 항목 ID
 * @param rating 평점
 * @param content 리뷰 내용
 * @param imageUrls 이미지 URL 목록
 * @param createdAt 작성일시
 */
public record ReviewResponse(
        Long reviewId,
        Long orderItemId,
        int rating,
        String content,
        List<String> imageUrls,
        LocalDateTime createdAt
) {}
