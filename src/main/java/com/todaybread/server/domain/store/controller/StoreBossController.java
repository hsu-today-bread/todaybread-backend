package com.todaybread.server.domain.store.controller;

import com.todaybread.server.config.jwt.JwtRoleHelper;
import com.todaybread.server.domain.store.dto.StoreCommonRequest;
import com.todaybread.server.domain.store.dto.StoreCommonResponse;
import com.todaybread.server.domain.store.dto.StoreImageResponse;
import com.todaybread.server.domain.store.dto.StoreInfoResponse;
import com.todaybread.server.domain.store.dto.StoreStatusResponse;
import com.todaybread.server.domain.store.service.StoreImageService;
import com.todaybread.server.domain.store.service.StoreService;
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
 * 사장님 전용 Store 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/boss/store")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BOSS')")
public class StoreBossController {

    private final StoreService storeService;
    private final StoreImageService storeImageService;

    /**
     * 사장님 탭 진입 상태를 조회합니다.
     * @param jwt JWT 토큰
     * @return 사장님 여부 및 가게 등록 여부
     */
    @Operation(summary = "사장님 가게 등록 상태 조회")
    @GetMapping("/status")
    public StoreStatusResponse getStoreStatus(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return storeService.getStoreStatus(userId);
    }

    /**
     * 사장님이 매장 정보와 이미지를 한번에 조회합니다.
     * @param jwt JWT 토큰
     * @return 매장 정보 + 이미지 목록
     */
    @Operation(summary = "내 가게 정보 + 이미지 조회")
    @GetMapping
    public StoreInfoResponse getStoreInfo(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return storeService.getStoreInfo(userId);
    }

    /**
     * 가게를 등록합니다 (정보 + 이미지 1~5장).
     * @param jwt JWT 토큰
     * @param request 가게 등록 요청
     * @param images 가게 이미지 (최소 1장, 최대 5장)
     * @return 가게 정보 + 이미지 응답
     */
    @Operation(summary = "가게 등록 (정보 + 이미지)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StoreInfoResponse addStore(@AuthenticationPrincipal Jwt jwt,
                                      @RequestPart("request") @Valid StoreCommonRequest request,
                                      @RequestPart("images") List<MultipartFile> images) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return storeService.addStore(userId, request, images);
    }
    
    /**
     * 가게 정보를 수정합니다 (이미지 제외).
     * @param jwt JWT 토큰
     * @param request 요청 DTO
     * @return 응답 DTO
     */
    @Operation(summary = "가게 정보 수정")
    @PutMapping
    public StoreCommonResponse updateStore(@AuthenticationPrincipal Jwt jwt,
                                           @RequestBody @Valid StoreCommonRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return storeService.updateStore(userId, request);
    }

    /**
     * 가게 이미지를 일괄 교체합니다 (Replace All 패턴, 1~5장).
     * @param jwt JWT 토큰
     * @param images 업로드할 이미지 파일 목록
     * @return 저장된 이미지 목록
     */
    @Operation(summary = "가게 이미지 교체")
    @PutMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<StoreImageResponse> updateImages(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestParam("images") List<MultipartFile> images) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return storeImageService.replaceImages(userId, images);
    }
}
