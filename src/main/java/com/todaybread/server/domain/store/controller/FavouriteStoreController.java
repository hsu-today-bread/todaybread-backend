package com.todaybread.server.domain.store.controller;

import com.todaybread.server.global.util.JwtRoleHelper;
import com.todaybread.server.domain.store.dto.FavouriteStoreResponse;
import com.todaybread.server.domain.store.dto.FavouriteStoreToggleRequest;
import com.todaybread.server.domain.store.dto.FavouriteStoreToggleResponse;
import com.todaybread.server.domain.store.service.FavouriteStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 단골 가게 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/favourite-stores")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class FavouriteStoreController {

    private final FavouriteStoreService favouriteStoreService;

    /**
     * 단골 가게를 토글합니다 (추가/해제).
     * @param jwt 인증된 사용자의 JWT 토큰
     * @param request 단골 가게 토글 요청 DTO
     * @return 토글 결과 응답
     */
    @Operation(summary = "단골 가게 토글 (추가/해제)")
    @PostMapping
    public FavouriteStoreToggleResponse toggleFavouriteStore(@AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid FavouriteStoreToggleRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return favouriteStoreService.toggleFavouriteStore(userId, request);
    }

    /**
     * 인증된 사용자의 단골 가게 목록을 조회합니다.
     * @param jwt 인증된 사용자의 JWT 토큰
     * @return 단골 가게 응답 DTO 목록
     */
    @Operation(summary = "단골 가게 목록 조회")
    @GetMapping
    public List<FavouriteStoreResponse> getMyFavouriteStores(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return favouriteStoreService.getMyFavouriteStores(userId);
    }
}
