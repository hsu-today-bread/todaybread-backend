package com.todaybread.server.domain.store.controller;

import com.todaybread.server.config.jwt.JwtRoleHelper;
import com.todaybread.server.domain.store.dto.StoreAddRequest;
import com.todaybread.server.domain.store.dto.StoreAddResponse;
import com.todaybread.server.domain.store.dto.StoreStatusResponse;
import com.todaybread.server.domain.store.service.StoreService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * store 도메인을 처리합니다.
 */
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
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
        boolean isBoss = JwtRoleHelper.isBoss(jwt);
        return storeService.getStoreStatus(userId, isBoss);
    }

    /**
     * 가게 등록을 처리합니다.
     * @param jwt JWT 토큰
     * @param request 요청 DTO
     * @return 응답 DTO
     */
    @PostMapping("/add-store")
    public StoreAddResponse addStore(@AuthenticationPrincipal Jwt jwt,
                                     @RequestBody @Valid StoreAddRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        boolean isBoss = JwtRoleHelper.isBoss(jwt);
        return storeService.addStore(userId, isBoss, request);
    }

}
