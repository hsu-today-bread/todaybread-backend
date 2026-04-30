package com.todaybread.server.domain.order.dto;

import java.time.LocalDate;

/**
 * 일별 매출 합산 JPQL 결과를 매핑하는 인터페이스 프로젝션입니다.
 * orderDate별 매출 합계를 반환합니다.
 */
public interface DailySalesProjection {

    /**
     * 매출 날짜를 반환합니다.
     *
     * @return 매출 날짜
     */
    LocalDate getSalesDate();

    /**
     * 해당 날짜의 총 매출액을 반환합니다.
     *
     * @return 총 매출액
     */
    Long getTotalSales();
}
