package com.todaybread.server.domain.store.dto;

/**
 * 이미지 업로드 응답 DTO입니다.
 *
 * @param id 이미지 ID
 * @param imageUrl 이미지 접근 URL
 * @param originalFilename 원본 파일명
 * @param displayOrder 표시 순서 (1 = 대표 이미지)
 */
public record StoreImageResponse(
        Long id,
        String imageUrl,
        String originalFilename,
        int displayOrder
) {
}
