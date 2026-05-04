package com.todaybread.server.domain.review.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 내 리뷰 목록 항목 응답 DTO입니다.
 *
 * @param reviewId 리뷰 ID
 * @param breadName 빵 이름
 * @param storeName 가게 이름
 * @param storeId 가게 ID
 * @param rating 평점
 * @param content 리뷰 내용
 * @param imageUrls 이미지 URL 목록
 * @param createdAt 작성일시
 */
public record MyReviewResponse(
        Long reviewId,
        String breadName,
        String storeName,
        Long storeId,
        int rating,
        String content,
        List<String> imageUrls,
        LocalDateTime createdAt
) {}
