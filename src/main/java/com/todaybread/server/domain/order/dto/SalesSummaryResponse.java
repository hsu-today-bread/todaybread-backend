package com.todaybread.server.domain.order.dto;

import java.util.List;

/**
 * 매출 요약 응답 DTO (총 매출 + 메뉴별 항목)
 *
 * @param totalSales    총 매출액
 * @param totalQuantity 총 판매 수량
 * @param items         메뉴별 매출 항목 목록
 */
public record SalesSummaryResponse(
        long totalSales,
        long totalQuantity,
        List<SalesItemResponse> items
) {
    /**
     * 매출 항목 목록으로부터 요약 응답을 생성합니다.
     *
     * @param items 매출 항목 목록
     * @return 매출 요약 응답
     */
    public static SalesSummaryResponse of(List<SalesItemResponse> items) {
        long totalSales = items.stream().mapToLong(SalesItemResponse::totalSales).sum();
        long totalQuantity = items.stream().mapToLong(SalesItemResponse::totalQuantity).sum();
        return new SalesSummaryResponse(totalSales, totalQuantity, items);
    }
}
