package com.todaybread.server.domain.interestarea.controller;

import com.todaybread.server.domain.interestarea.dto.InterestAreaDeleteResponse;
import com.todaybread.server.domain.interestarea.dto.InterestAreaRequest;
import com.todaybread.server.domain.interestarea.dto.InterestAreaResponse;
import com.todaybread.server.domain.interestarea.dto.InterestAreaWrapperResponse;
import com.todaybread.server.domain.interestarea.service.InterestAreaService;
import com.todaybread.server.global.util.JwtRoleHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 관심지역 도메인 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/interest-area")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class InterestAreaController {

    private final InterestAreaService interestAreaService;

    /**
     * 유저의 관심지역을 조회합니다.
     *
     * @param jwt 인증된 사용자의 JWT 토큰
     * @return 관심지역 래퍼 응답 (interestArea가 null일 수 있음)
     */
    @Operation(summary = "관심지역 조회")
    @GetMapping
    public InterestAreaWrapperResponse getInterestArea(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return interestAreaService.getInterestArea(userId);
    }

    /**
     * 관심지역을 등록합니다.
     *
     * @param jwt     인증된 사용자의 JWT 토큰
     * @param request 관심지역 등록 요청 DTO
     * @return 등록된 관심지역 응답
     */
    @Operation(summary = "관심지역 등록")
    @PostMapping
    public InterestAreaResponse createInterestArea(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid InterestAreaRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return interestAreaService.createInterestArea(userId, request);
    }

    /**
     * 관심지역을 수정합니다.
     *
     * @param jwt     인증된 사용자의 JWT 토큰
     * @param request 관심지역 수정 요청 DTO
     * @return 수정된 관심지역 응답
     */
    @Operation(summary = "관심지역 수정")
    @PutMapping
    public InterestAreaResponse updateInterestArea(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid InterestAreaRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return interestAreaService.updateInterestArea(userId, request);
    }

    /**
     * 관심지역을 삭제합니다.
     *
     * @param jwt 인증된 사용자의 JWT 토큰
     * @return 삭제 응답 (keywordNotificationDisabled 포함)
     */
    @Operation(summary = "관심지역 삭제")
    @DeleteMapping
    public InterestAreaDeleteResponse deleteInterestArea(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return interestAreaService.deleteInterestArea(userId);
    }
}
