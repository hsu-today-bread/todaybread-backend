package com.todaybread.server.domain.order.dto;

import java.util.List;

/**
 * 월별 매출 응답 DTO (일별 합산 + 메뉴별 항목)
 *
 * @param totalSales    총 매출액
 * @param totalQuantity 총 판매 수량
 * @param dailySales    일별 매출 합산 목록
 * @param items         메뉴별 매출 항목 목록
 */
public record MonthlySalesResponse(
        long totalSales,
        long totalQuantity,
        List<DailySalesEntry> dailySales,
        List<SalesItemResponse> items
) {
}
