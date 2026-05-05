package com.todaybread.server.domain.review.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 사장님 리뷰 관리 목록 조회 시 정렬 기준을 정의하는 enum입니다.
 */
public enum BossReviewSortType {
    LATEST,
    OLDEST,
    RATING_HIGH,
    RATING_LOW;

    private static final Logger log = LoggerFactory.getLogger(BossReviewSortType.class);

    /**
     * 문자열을 BossReviewSortType으로 변환합니다.
     * 매칭되지 않으면 LATEST를 반환합니다.
     *
     * @param value 정렬 기준 문자열
     * @return BossReviewSortType
     */
    public static BossReviewSortType from(String value) {
        if (value == null) {
            return LATEST;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 BossReviewSortType 값: {}, LATEST로 대체", value);
            return LATEST;
        }
    }
}
