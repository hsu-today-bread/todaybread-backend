package com.todaybread.server.domain.order.controller;

import com.todaybread.server.domain.order.dto.SalesItemResponse;
import com.todaybread.server.domain.order.service.OrderBossService;
import com.todaybread.server.global.util.JwtRoleHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 사장님 매출 조회 전용 컨트롤러입니다.
 * 일별/월별 매출 조회를 제공합니다.
 */
@RestController
@RequestMapping("/api/boss/sales")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BOSS')")
@Validated
public class SalesBossController {

    private final OrderBossService orderBossService;

    /**
     * 일별 매출을 조회합니다.
     *
     * @param jwt  JWT 토큰
     * @param date 조회 날짜 (ISO DATE 형식)
     * @return 매출 항목 응답 목록
     */
    @Operation(summary = "일별 매출 조회")
    @GetMapping("/daily")
    public List<SalesItemResponse> getDailySales(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return orderBossService.getDailySales(userId, date);
    }

    /**
     * 월별 매출을 조회합니다.
     *
     * @param jwt   JWT 토큰
     * @param year  조회 연도
     * @param month 조회 월
     * @return 매출 항목 응답 목록
     */
    @Operation(summary = "월별 매출 조회")
    @GetMapping("/monthly")
    public List<SalesItemResponse> getMonthlySales(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @Min(2000) int year,
            @RequestParam @Min(1) @Max(12) int month) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return orderBossService.getMonthlySales(userId, year, month);
    }
}
