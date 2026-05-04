package com.todaybread.server.domain.review.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 가게 리뷰 목록 항목 응답 DTO입니다.
 *
 * @param reviewId 리뷰 ID
 * @param nickname 작성자 닉네임
 * @param rating 평점
 * @param content 리뷰 내용
 * @param breadName 빵 이름
 * @param imageUrls 이미지 URL 목록
 * @param createdAt 작성일시
 */
public record StoreReviewResponse(
        Long reviewId,
        String nickname,
        int rating,
        String content,
        String breadName,
        List<String> imageUrls,
        LocalDateTime createdAt
) {}
