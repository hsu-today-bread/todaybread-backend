package com.todaybread.server.domain.review.dto;

/**
 * 사장님 리뷰 관리 목록 조회 시 필터 기준을 정의하는 enum입니다.
 */
public enum BossReviewFilterType {
    ALL,
    WITH_IMAGE,
    TEXT_ONLY;

    /**
     * 문자열을 BossReviewFilterType으로 변환합니다.
     * 매칭되지 않으면 ALL을 반환합니다.
     *
     * @param value 필터 기준 문자열
     * @return BossReviewFilterType
     */
    public static BossReviewFilterType from(String value) {
        if (value == null) {
            return ALL;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }
}
