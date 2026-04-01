package com.todaybread.server.domain.wishlist.dto;

import com.todaybread.server.domain.keyword.dto.KeywordResponse;
import com.todaybread.server.domain.store.dto.FavouriteStoreResponse;

import java.util.List;

/**
 * 찜목록 통합 조회 응답 DTO
 *
 * @param keywords        사용자의 키워드 목록
 * @param favouriteStores 사용자의 단골 가게 목록
 */
public record WishlistResponse(
        List<KeywordResponse> keywords,
        List<FavouriteStoreResponse> favouriteStores
) {
}
