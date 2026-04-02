package com.todaybread.server.domain.bread.controller;

import com.todaybread.server.domain.bread.dto.BreadCommonResponse;
import com.todaybread.server.domain.bread.dto.BreadDetailResponse;
import com.todaybread.server.domain.bread.dto.BreadSortType;
import com.todaybread.server.domain.bread.dto.NearbyBreadResponse;
import com.todaybread.server.domain.bread.service.BreadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 일반 유저용 Bread 조회 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/bread")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class BreadController {

    private final BreadService breadService;

    /**
     * 유저 좌표 기준 반경 내 가게들의 빵 목록을 조회합니다.
     *
     * @param lat    유저 위도
     * @param lng    유저 경도
     * @param radius 검색 반경 (km, 기본값 1)
     * @param sort   정렬 기준 (none, distance, price, discount)
     * @return 근처 빵 응답 리스트
     */
    @Operation(summary = "근처 빵 목록 조회")
    @GetMapping("/nearby")
    public List<NearbyBreadResponse> getNearbyBreads(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") BigDecimal lng,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10) int radius,
            @RequestParam(defaultValue = "none") String sort) {
        return breadService.getNearbyBreads(lat, lng, radius, BreadSortType.from(sort));
    }

    /**
     * 빵 상세 정보를 조회합니다.
     *
     * @param breadId 빵 ID
     * @return 빵 상세 응답
     */
    @Operation(summary = "빵 상세 조회")
    @GetMapping("/detail/{breadId}")
    public BreadDetailResponse getBreadDetail(@PathVariable Long breadId) {
        return breadService.getBreadDetail(breadId);
    }

    /**
     * 특정 가게의 메뉴 목록을 조회합니다.
     *
     * @param storeId 가게 ID
     * @return 빵 공통 응답 리스트
     */
    @Operation(summary = "가게 메뉴 목록 조회")
    @GetMapping("/{storeId}")
    public List<BreadCommonResponse> getBreadsFromStore(@PathVariable Long storeId) {
        return breadService.getBreadsFromStore(storeId);
    }
}
