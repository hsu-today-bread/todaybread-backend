package com.todaybread.server.domain.order.dto;

/**
 * 매출 집계 JPQL 결과를 매핑하는 인터페이스 프로젝션입니다.
 * breadId + breadName별 판매 수량과 매출 합계를 반환합니다.
 */
public interface SalesAggregateProjection {

    /**
     * 빵 ID를 반환합니다. 삭제된 메뉴는 null입니다.
     *
     * @return 빵 ID (nullable)
     */
    Long getBreadId();

    /**
     * 빵 이름을 반환합니다.
     *
     * @return 빵 이름
     */
    String getBreadName();

    /**
     * 총 판매 수량을 반환합니다.
     *
     * @return 총 판매 수량
     */
    Long getTotalQuantity();

    /**
     * 총 매출액을 반환합니다.
     *
     * @return 총 매출액
     */
    Long getTotalSales();
}
