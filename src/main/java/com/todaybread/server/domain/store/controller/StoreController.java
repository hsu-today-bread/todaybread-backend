package com.todaybread.server.domain.store.controller;

import com.todaybread.server.domain.store.dto.NearbyStoreResponse;
import com.todaybread.server.domain.store.dto.StoreDetailResponse;
import com.todaybread.server.domain.store.service.StoreService;
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
 * 일반 유저용 Store 조회 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class StoreController {

    private final StoreService storeService;

    /**
     * 유저 좌표 기준 반경 내 가게 목록을 조회합니다.
     *
     * @param lat    유저 위도
     * @param lng    유저 경도
     * @param radius 검색 반경 (km, 기본값 1)
     * @return 근처 가게 응답 리스트
     */
    @Operation(summary = "근처 가게 목록 조회")
    @GetMapping("/nearby")
    public List<NearbyStoreResponse> getNearbyStores(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") BigDecimal lng,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10) int radius) {
        return storeService.getNearbyStores(lat, lng, radius);
    }

    /**
     * 가게 상세 정보를 조회합니다.
     *
     * @param storeId 가게 ID
     * @return 가게 상세 응답
     */
    @Operation(summary = "가게 상세 조회")
    @GetMapping("/{storeId}")
    public StoreDetailResponse getStoreDetail(@PathVariable Long storeId) {
        return storeService.getStoreDetail(storeId);
    }

}
