package com.todaybread.server.domain.order.controller;

import com.todaybread.server.domain.order.dto.BossOrderResponse;
import com.todaybread.server.domain.order.service.OrderBossService;
import com.todaybread.server.global.util.JwtRoleHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 사장님 주문 관리 전용 컨트롤러입니다.
 * 픽업 대기 주문 조회 및 픽업 완료 처리를 제공합니다.
 */
@RestController
@RequestMapping("/api/boss/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BOSS')")
@Validated
public class OrderBossController {

    private final OrderBossService orderBossService;

    /**
     * 픽업 대기 주문 목록을 페이지네이션으로 조회합니다.
     *
     * @param jwt  JWT 토큰
     * @param page 페이지 번호 (기본값 0)
     * @param size 페이지 크기 (기본값 20)
     * @return 사장님 주문내역 응답 페이지
     */
    @Operation(summary = "픽업 대기 주문 목록 조회")
    @GetMapping
    public Page<BossOrderResponse> getConfirmedOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return orderBossService.getConfirmedOrders(userId, PageRequest.of(page, Math.min(size, 100)));
    }

    /**
     * 픽업 완료 처리를 수행합니다.
     *
     * @param jwt     JWT 토큰
     * @param orderId 주문 ID
     */
    @Operation(summary = "픽업 완료 처리")
    @PostMapping("/{orderId}/pickup")
    public void pickupOrder(@AuthenticationPrincipal Jwt jwt,
                            @PathVariable Long orderId) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        orderBossService.pickupOrder(userId, orderId);
    }
}
