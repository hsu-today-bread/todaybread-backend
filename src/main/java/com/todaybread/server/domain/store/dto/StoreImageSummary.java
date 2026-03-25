package com.todaybread.server.domain.store.dto;

/**
 * 이미지 요약 DTO입니다.
 * StoreStatusResponse 내 이미지 목록에 사용됩니다.
 *
 * @param id 이미지 ID
 * @param imageUrl 이미지 접근 URL
 * @param displayOrder 표시 순서 (1 = 대표 이미지)
 */
public record StoreImageSummary(
        Long id,
        String imageUrl,
        int displayOrder
) {
}
