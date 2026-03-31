package com.todaybread.server.domain.store.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 단골 가게 토글 요청 DTO
 *
 * @param storeId 가게 ID
 */
public record FavouriteStoreToggleRequest(
        @NotNull Long storeId
) {
}
