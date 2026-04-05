package com.todaybread.server.domain.order.controller;

import com.todaybread.server.domain.order.dto.DirectOrderRequest;
import com.todaybread.server.domain.order.dto.OrderDetailResponse;
import com.todaybread.server.domain.order.dto.OrderResponse;
import com.todaybread.server.domain.order.service.OrderService;
import com.todaybread.server.global.util.JwtRoleHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 컨트롤러입니다.
 * 장바구니 기반 주문, 바로 구매, 주문 취소, 주문 목록/상세 조회 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    /**
     * 장바구니 기반 주문을 생성합니다.
     *
     * @param jwt            JWT 토큰
     * @param idempotencyKey 멱등성 키
     * @return 주문 상세 응답
     */
    @Operation(summary = "장바구니 기반 주문 생성")
    @PostMapping("/cart")
    public OrderDetailResponse createOrderFromCart(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return orderService.createOrderFromCart(userId, idempotencyKey);
    }

    /**
     * 바로 구매 주문을 생성합니다.
     *
     * @param jwt            JWT 토큰
     * @param idempotencyKey 멱등성 키
     * @param request        바로 구매 요청
     * @return 주문 상세 응답
     */
    @Operation(summary = "바로 구매")
    @PostMapping("/direct")
    public OrderDetailResponse createDirectOrder(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
                                                 @RequestBody @Valid DirectOrderRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return orderService.createDirectOrder(userId, request, idempotencyKey);
    }

    /**
     * 주문을 취소합니다.
     *
     * @param jwt     JWT 토큰
     * @param orderId 주문 ID
     */
    @Operation(summary = "주문 취소")
    @PostMapping("/{orderId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public void cancelOrder(@AuthenticationPrincipal Jwt jwt,
                            @PathVariable Long orderId) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        orderService.cancelOrder(userId, orderId);
    }

    /**
     * 주문 내역 목록을 조회합니다.
     *
     * @param jwt JWT 토큰
     * @return 주문 응답 목록
     */
    @Operation(summary = "주문 내역 목록 조회")
    @GetMapping
    public Page<OrderResponse> getOrders(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return orderService.getOrders(userId, pageable);
    }

    /**
     * 주문 상세를 조회합니다.
     *
     * @param jwt     JWT 토큰
     * @param orderId 주문 ID
     * @return 주문 상세 응답
     */
    @Operation(summary = "주문 상세 조회")
    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrderDetail(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable Long orderId) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return orderService.getOrderDetail(userId, orderId);
    }
}
