package com.todaybread.server.domain.bread.dto;

/**
 * 근처 빵 목록 조회 시 정렬 기준을 정의하는 enum입니다.
 */
public enum BreadSortType {
    NONE,
    DISTANCE,
    PRICE,
    DISCOUNT;

    /**
     * 문자열을 BreadSortType으로 변환합니다.
     * 매칭되지 않으면 NONE을 반환합니다.
     *
     * @param value 정렬 기준 문자열
     * @return BreadSortType
     */
    public static BreadSortType from(String value) {
        if (value == null) {
            return NONE;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
