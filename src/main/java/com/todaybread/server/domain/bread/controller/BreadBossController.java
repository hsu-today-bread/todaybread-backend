package com.todaybread.server.domain.bread.controller;

import com.todaybread.server.config.jwt.JwtRoleHelper;
import com.todaybread.server.domain.bread.dto.BreadCommonRequest;
import com.todaybread.server.domain.bread.dto.BreadCommonResponse;
import com.todaybread.server.domain.bread.dto.BreadStockUpdateRequest;
import com.todaybread.server.domain.bread.dto.BreadSuccessResponse;
import com.todaybread.server.domain.bread.service.BreadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 사장님 전용 Bread 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/boss/bread")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BOSS')")
public class BreadBossController {

    private final BreadService breadService;

    /**
     * 내 가게의 목록을 조회합니다.
     * @param jwt JWT 토큰
     * @return 빵 공통 응답 리스트
     */
    @Operation(summary = "내 가게 메뉴 목록 조회")
    @GetMapping
    public List<BreadCommonResponse> getMyBreads(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return breadService.getMyBreads(userId);
    }

    /**
     * 메뉴를 등록합니다.
     * @param jwt JWT 토큰
     * @param request 빵 추가 요청
     * @param image 빵 이미지
     * @return 빵 공통 응답
     */
    @Operation(summary = "메뉴 등록")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BreadCommonResponse addBread(@AuthenticationPrincipal Jwt jwt,
                                        @RequestPart("request") @Valid BreadCommonRequest request,
                                        @RequestPart(value = "image", required = false) MultipartFile image) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return breadService.addBread(userId, request, image);
    }

    /**
     * 메뉴 정보를 업데이트합니다 (이미지 포함 가능).
     * @param jwt JWT 토큰
     * @param breadId 빵 ID
     * @param request 업데이트 요청
     * @param image 이미지 (선택사항)
     * @return 빵 공통 응답
     */
    @Operation(summary = "메뉴 수정")
    @PutMapping(path = "/{breadId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BreadCommonResponse updateBread(@AuthenticationPrincipal Jwt jwt,
                                           @PathVariable Long breadId,
                                           @RequestPart("request") @Valid BreadCommonRequest request,
                                           @RequestPart(value = "image", required = false) MultipartFile image) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return breadService.updateBread(userId, breadId, request, image);
    }

    /**
     * 메뉴를 품절 처리 및 품절 해제 처리, 메뉴의 재고를 설정합니다.
     * @param jwt JWT 토큰
     * @param breadId 빵 ID
     * @param request 재고 수정 요청
     * @return 성공 응답
     */
    @Operation(summary = "메뉴 품절 및 해제 처리 / 메뉴 재고 설정 포함")
    @PatchMapping("/{breadId}/stock")
    public BreadSuccessResponse changeStock(@AuthenticationPrincipal Jwt jwt,
                                            @PathVariable Long breadId,
                                            @RequestBody @Valid BreadStockUpdateRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return breadService.changeQuantity(userId, breadId, request);
    }

    /**
     * 메뉴를 삭제합니다.
     * @param jwt JWT 토큰
     * @param breadId 빵 ID
     * @return 성공 응답
     */
    @Operation(summary = "메뉴 삭제")
    @DeleteMapping("/{breadId}")
    public BreadSuccessResponse deleteBread(@AuthenticationPrincipal Jwt jwt,
                                           @PathVariable Long breadId) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return breadService.deleteBread(userId, breadId);
    }
}
