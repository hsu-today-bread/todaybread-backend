package com.todaybread.server.domain.cart.dto;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.cart.entity.CartItemEntity;

/**
 * 장바구니 항목 응답 DTO
 *
 * @param cartItemId 장바구니 항목 ID
 * @param breadId 빵 ID
 * @param breadName 빵 이름
 * @param description 빵 설명
 * @param quantity 수량
 * @param imageUrl 이미지 URL
 * @param salePrice 할인가
 */
public record CartItemResponse(
        Long cartItemId,
        Long breadId,
        String breadName,
        String description,
        int quantity,
        String imageUrl,
        int salePrice
) {
    /**
     * CartItemEntity와 BreadEntity로부터 응답을 생성합니다.
     *
     * @param cartItem 장바구니 항목 엔티티
     * @param bread    빵 엔티티
     * @param imageUrl 이미지 URL
     * @return 장바구니 항목 응답
     */
    public static CartItemResponse of(CartItemEntity cartItem, BreadEntity bread, String imageUrl) {
        return new CartItemResponse(
                cartItem.getId(),
                bread.getId(),
                bread.getName(),
                bread.getDescription(),
                cartItem.getQuantity(),
                imageUrl,
                bread.getSalePrice()
        );
    }
}
