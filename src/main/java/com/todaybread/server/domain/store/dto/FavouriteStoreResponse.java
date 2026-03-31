package com.todaybread.server.domain.store.dto;

/**
 * 단골 가게 목록 조회 응답 DTO
 *
 * @param storeId   가게 ID
 * @param name      가게 이름
 * @param address   가게 주소 (addressLine1 + " " + addressLine2)
 * @param imageUrl  가게 대표 이미지 URL (없으면 null)
 * @param isSelling 판매중 여부
 */
public record FavouriteStoreResponse(
        Long storeId,
        String name,
        String address,
        String imageUrl,
        boolean isSelling
) {
}
