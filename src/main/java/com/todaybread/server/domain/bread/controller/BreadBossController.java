package com.todaybread.server.domain.bread.controller;

import com.todaybread.server.config.jwt.JwtRoleHelper;
import com.todaybread.server.domain.bread.dto.BreadAddRequest;
import com.todaybread.server.domain.bread.dto.BreadCommonResponse;
import com.todaybread.server.domain.bread.dto.BreadUpdateRequest;
import com.todaybread.server.domain.bread.service.BreadImageService;
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
    @GetMapping("/my-breads")
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
    @PostMapping(path = "/add-bread",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BreadCommonResponse addBread(@AuthenticationPrincipal Jwt jwt,
                                        @RequestPart @Valid BreadAddRequest request,
                                        @RequestPart(required = false) MultipartFile image) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return breadService.addBread(userId, request, image);
    }

    /**
     * 정보를 업데이트 합니다 (이미지 포함 가능).
     * @param jwt JWT 토큰
     * @param request 업데이트 요청
     * @param image 이미지 (선택사항)
     * @return 빵 공통 응답
     */
    @Operation(summary = "메뉴 수정/삭제/품절")
    @PutMapping(path = "/update-bread", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BreadCommonResponse updateBread(@AuthenticationPrincipal Jwt jwt,
                                           @RequestPart @Valid BreadUpdateRequest request,
                                           @RequestPart(required = false) MultipartFile image) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return breadService.updateBread(userId, request, image);
    }
}
