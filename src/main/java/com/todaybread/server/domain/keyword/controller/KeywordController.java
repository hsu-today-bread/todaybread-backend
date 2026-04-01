package com.todaybread.server.domain.keyword.controller;

import com.todaybread.server.global.util.JwtRoleHelper;
import com.todaybread.server.domain.keyword.dto.KeywordCreateRequest;
import com.todaybread.server.domain.keyword.dto.KeywordCreateResponse;
import com.todaybread.server.domain.keyword.dto.KeywordDeleteResponse;
import com.todaybread.server.domain.keyword.dto.KeywordResponse;
import com.todaybread.server.domain.keyword.service.KeywordService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 키워드 도메인 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class KeywordController {

    private final KeywordService keywordService;

    /**
     * 키워드를 등록합니다.
     *
     * @param jwt 인증된 사용자의 JWT 토큰
     * @param request 키워드 등록 요청 DTO
     * @return 등록 결과 응답
     */
    @PostMapping("/add")
    public KeywordCreateResponse createKeyword(@AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid KeywordCreateRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return keywordService.createKeyword(userId, request);
    }

    /**
     * 인증된 사용자의 키워드 목록을 조회합니다.
     *
     * @param jwt 인증된 사용자의 JWT 토큰
     * @return 키워드 응답 DTO 목록
     */
    @GetMapping("/get-keyword")
    public List<KeywordResponse> getMyKeywords(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return keywordService.getMyKeywords(userId);
    }

    /**
     * 키워드를 삭제합니다.
     *
     * @param jwt 인증된 사용자의 JWT 토큰
     * @param userKeywordId 삭제할 사용자-키워드 관계 ID
     * @return 삭제 결과 응답
     */
    @DeleteMapping("/delete/{userKeywordId}")
    public KeywordDeleteResponse deleteKeyword(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long userKeywordId) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return keywordService.deleteKeyword(userId, userKeywordId);
    }
}
