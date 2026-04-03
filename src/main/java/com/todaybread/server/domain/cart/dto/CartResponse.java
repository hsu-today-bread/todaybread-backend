package com.todaybread.server.domain.cart.dto;

import java.time.LocalTime;
import java.util.List;

/**
 * 장바구니 조회 응답 DTO
 *
 * @param storeName 매장 이름
 * @param lastOrderTime 오늘 라스트오더 시간
 * @param items 장바구니 항목 목록
 */
public record CartResponse(
        String storeName,
        LocalTime lastOrderTime,
        List<CartItemResponse> items
) {
}
