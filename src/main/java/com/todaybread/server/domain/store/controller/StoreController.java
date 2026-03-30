package com.todaybread.server.domain.store.controller;

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

    // 향후 유저용 가게 조회 API 추가 예정
    // GET /api/store/nearby?lat=...&lng=...&radius=...
    // GET /api/store/{storeId}
}
