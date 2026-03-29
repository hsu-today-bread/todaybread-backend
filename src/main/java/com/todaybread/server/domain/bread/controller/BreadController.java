package com.todaybread.server.domain.bread.controller;

import com.todaybread.server.domain.bread.dto.BreadCommonResponse;
import com.todaybread.server.domain.bread.service.BreadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
     * 특정 가게의 메뉴 목록을 조회합니다.
     */
    @Operation(summary = "가게 메뉴 목록 조회")
    @GetMapping("/{storeId}")
    public List<BreadCommonResponse> getBreadsFromStore(@PathVariable Long storeId) {
        return breadService.getBreadsFromStore(storeId);
    }
}
