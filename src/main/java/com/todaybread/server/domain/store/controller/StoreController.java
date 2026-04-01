package com.todaybread.server.domain.store.controller;

import com.todaybread.server.domain.store.dto.StoreDetailResponse;
import com.todaybread.server.domain.store.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
