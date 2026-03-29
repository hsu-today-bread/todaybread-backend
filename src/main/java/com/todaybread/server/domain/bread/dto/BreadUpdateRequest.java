package com.todaybread.server.domain.bread.dto;

/**
 * 빵 정보 업데이트 시 사용됩니다.
 *
 * @param delete 메뉴 삭제 여부
 * @param soldOut 품절 여부
 * @param id 빵 ID
 * @param breadAddRequest 빵 업데이트 정보
 */
public record BreadUpdateRequest(
        boolean delete,
        boolean soldOut,
        Long id,
        BreadAddRequest breadAddRequest
) {
}
