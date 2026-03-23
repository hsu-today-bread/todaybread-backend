package com.todaybread.server.domain.store.controller;

import com.todaybread.server.config.jwt.JwtRoleHelper;
import com.todaybread.server.domain.store.dto.StoreCommonRequest;
import com.todaybread.server.domain.store.dto.StoreCommonResponse;
import com.todaybread.server.domain.store.dto.StoreStatusResponse;
import com.todaybread.server.domain.store.service.StoreService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * store 도메인을 처리합니다.
 */
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BOSS')")
public class StoreController {

    private final StoreService storeService;

    /**
     * 사장님 탭 진입 상태를 조회합니다.
     * @param jwt JWT 토큰
     * @return 사장님 여부 및 가게 등록 여부
     */
    @GetMapping("/status")
    public StoreStatusResponse getStoreStatus(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return storeService.getStoreStatus(userId);
    }

    /**
     * 가게 등록을 처리합니다.
     * @param jwt JWT 토큰
     * @param request 요청 DTO
     * @return 응답 DTO
     */
    @PostMapping("/add-store")
    public StoreCommonResponse addStore(@AuthenticationPrincipal Jwt jwt,
                                     @RequestBody @Valid StoreCommonRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return storeService.addStore(userId, request);
    }

    /**
     * 가게 정보를 업데이트합니다.
     * @param jwt JWT 토큰
     * @param request 요청 DTO
     * @return 응답 DTO
     */
    @PutMapping("/update-store")
    public StoreCommonResponse updateStore(@AuthenticationPrincipal Jwt jwt,
                                           @RequestBody @Valid StoreCommonRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return storeService.updateStore(userId, request);
    }
}
