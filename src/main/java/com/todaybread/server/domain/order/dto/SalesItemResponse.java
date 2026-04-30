package com.todaybread.server.domain.order.dto;

/**
 * 매출 항목 응답 DTO (메뉴별 집계)
 *
 * @param breadId       빵 ID (삭제된 메뉴는 null)
 * @param breadName     빵 이름
 * @param breadPrice    빵 단가 (스냅샷 기준)
 * @param totalQuantity 총 판매 수량
 * @param totalSales    총 매출액
 */
public record SalesItemResponse(
        Long breadId,
        String breadName,
        int breadPrice,
        long totalQuantity,
        long totalSales
) {
    /**
     * SalesAggregateProjection으로부터 응답을 생성합니다.
     *
     * @param projection 매출 집계 프로젝션
     * @return 매출 항목 응답
     */
    public static SalesItemResponse of(SalesAggregateProjection projection) {
        return new SalesItemResponse(
                projection.getBreadId(),
                projection.getBreadName(),
                projection.getBreadPrice() != null ? projection.getBreadPrice() : 0,
                projection.getTotalQuantity(),
                projection.getTotalSales()
        );
    }
}
