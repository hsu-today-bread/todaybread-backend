package com.todaybread.server.domain.store.dto;

/**
 * 단골 가게 토글 응답 DTO
 *
 * @param added 추가 여부 (true: 추가됨, false: 해제됨)
 */
public record FavouriteStoreToggleResponse(
        boolean added
) {
}
