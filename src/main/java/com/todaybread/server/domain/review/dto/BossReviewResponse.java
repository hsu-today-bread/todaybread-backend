package com.todaybread.server.domain.review.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사장님 리뷰 관리 목록 항목 응답 DTO입니다.
 *
 * @param reviewId 리뷰 ID
 * @param nickname 작성자 닉네임
 * @param rating 평점
 * @param content 리뷰 내용
 * @param breadName 빵 이름
 * @param imageUrls 이미지 URL 목록
 * @param createdAt 작성일시
 * @param purchaseCount 작성자의 해당 가게 구매 횟수
 */
public record BossReviewResponse(
        Long reviewId,
        String nickname,
        int rating,
        String content,
        String breadName,
        List<String> imageUrls,
        LocalDateTime createdAt,
        int purchaseCount
) {}
