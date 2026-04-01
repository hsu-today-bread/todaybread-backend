package com.todaybread.server.domain.wishlist.controller;

import com.todaybread.server.global.util.JwtRoleHelper;
import com.todaybread.server.domain.keyword.dto.KeywordResponse;
import com.todaybread.server.domain.keyword.service.KeywordService;
import com.todaybread.server.domain.store.dto.FavouriteStoreResponse;
import com.todaybread.server.domain.store.service.FavouriteStoreService;
import com.todaybread.server.domain.wishlist.dto.WishlistResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 찜목록 통합 조회 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final KeywordService keywordService;
    private final FavouriteStoreService favouriteStoreService;

    /**
     * 찜목록을 통합 조회합니다 (키워드 + 단골 가게).
     *
     * @param jwt 인증된 사용자의 JWT 토큰
     * @return 키워드 목록과 단골 가게 목록을 포함한 찜목록 응답
     */
    @Operation(summary = "찜목록 통합 조회 (키워드 + 단골 가게)")
    @GetMapping
    public WishlistResponse getWishlist(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        List<KeywordResponse> keywords = keywordService.getMyKeywords(userId);
        List<FavouriteStoreResponse> favouriteStores = favouriteStoreService.getMyFavouriteStores(userId);
        return new WishlistResponse(keywords, favouriteStores);
    }
}
