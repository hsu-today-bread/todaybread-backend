package com.todaybread.server.domain.interestarea.dto;

/**
 * 매장 정보 DTO
 *
 * @param storeId 매장 ID
 * @param storeName 매장 이름
 */
public record StoreInfo(
        Long storeId,
        String storeName
) {
}
