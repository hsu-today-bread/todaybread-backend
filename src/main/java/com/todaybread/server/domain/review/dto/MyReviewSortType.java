package com.todaybread.server.domain.review.dto;

/**
 * 내 리뷰 목록 조회 시 정렬 기준을 정의하는 enum입니다.
 */
public enum MyReviewSortType {
    LATEST,
    OLDEST;

    /**
     * 문자열을 MyReviewSortType으로 변환합니다.
     * 매칭되지 않으면 LATEST를 반환합니다.
     *
     * @param value 정렬 기준 문자열
     * @return MyReviewSortType
     */
    public static MyReviewSortType from(String value) {
        if (value == null) {
            return LATEST;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LATEST;
        }
    }
}
