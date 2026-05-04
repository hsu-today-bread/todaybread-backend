package com.todaybread.server.domain.review.dto;

/**
 * 가게 평점 정보 DTO입니다. (내부 전달용)
 *
 * @param averageRating 평균 평점
 * @param reviewCount 리뷰 총 개수
 */
public record StoreRatingInfo(
        double averageRating,
        int reviewCount
) {}
