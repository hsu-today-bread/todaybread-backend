package com.todaybread.server.domain.cart.controller;

import com.todaybread.server.domain.cart.dto.CartAddRequest;
import com.todaybread.server.domain.cart.dto.CartResponse;
import com.todaybread.server.domain.cart.dto.CartUpdateRequest;
import com.todaybread.server.domain.cart.service.CartService;
import com.todaybread.server.global.util.JwtRoleHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 장바구니 컨트롤러입니다.
 * 장바구니 추가, 조회, 수량 변경, 항목 삭제, 비우기 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    /**
     * 장바구니에 빵을 추가합니다.
     *
     * @param jwt     JWT 토큰
     * @param request 장바구니 추가 요청
     */
    @Operation(summary = "장바구니에 빵 추가")
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void addToCart(@AuthenticationPrincipal Jwt jwt,
                          @RequestBody @Valid CartAddRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        cartService.addToCart(userId, request);
    }

    /**
     * 장바구니를 조회합니다.
     *
     * @param jwt JWT 토큰
     * @return 장바구니 응답
     */
    @Operation(summary = "장바구니 조회")
    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return cartService.getCart(userId);
    }

    /**
     * 장바구니 항목의 수량을 변경합니다.
     *
     * @param jwt        JWT 토큰
     * @param cartItemId 장바구니 항목 ID
     * @param request    수량 변경 요청
     */
    @Operation(summary = "장바구니 수량 변경")
    @PatchMapping("/items/{cartItemId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateQuantity(@AuthenticationPrincipal Jwt jwt,
                               @PathVariable Long cartItemId,
                               @RequestBody @Valid CartUpdateRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        cartService.updateQuantity(userId, cartItemId, request);
    }

    /**
     * 장바구니에서 항목을 삭제합니다.
     *
     * @param jwt        JWT 토큰
     * @param cartItemId 장바구니 항목 ID
     */
    @Operation(summary = "장바구니 항목 삭제")
    @DeleteMapping("/items/{cartItemId}")
    @ResponseStatus(HttpStatus.OK)
    public void removeItem(@AuthenticationPrincipal Jwt jwt,
                           @PathVariable Long cartItemId) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        cartService.removeItem(userId, cartItemId);
    }

    /**
     * 장바구니를 비웁니다.
     *
     * @param jwt JWT 토큰
     */
    @Operation(summary = "장바구니 비우기")
    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public void clearCart(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        cartService.clearCart(userId);
    }
}
