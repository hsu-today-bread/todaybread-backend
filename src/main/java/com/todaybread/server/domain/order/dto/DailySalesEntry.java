package com.todaybread.server.domain.order.dto;

import java.time.LocalDate;

/**
 * 일별 매출 합산 항목 DTO (월별 조회 시 사용)
 *
 * @param date       매출 날짜
 * @param totalSales 해당 날짜의 총 매출액
 */
public record DailySalesEntry(
        LocalDate date,
        long totalSales
) {
}
