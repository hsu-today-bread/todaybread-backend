package com.todaybread.server.domain.keyword.controller;

import com.todaybread.server.domain.keyword.dto.*;
import com.todaybread.server.domain.keyword.service.KeywordService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
public class KeywordController {

    private final KeywordService keywordService;

    /**
     * 유저의 키워드 목록을 조회합니다.
     * @param jwt 유저 JWT 토큰
     * @return 유저의 키워드 목록 응답
     */
    @GetMapping
    public ResponseEntity<KeywordListResponse> getUserKeywords(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        List<KeywordResponse> keywords = keywordService.getUserKeywords(userId);
        return ResponseEntity.ok(KeywordListResponse.ok(keywords));
    }

    /**
     * 특정 유저가 등록한 키워드가 있는지 확인합니다.
     * @param jwt 유저 JWT 토큰
     * @return 존재 여부 응답
     */
    @GetMapping("/exist")
    public ResponseEntity<KeywordExistResponse> hasKeywords(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        boolean hasKeyword = keywordService.hasKeywords(userId);
        return ResponseEntity.ok(KeywordExistResponse.of(hasKeyword));
    }

    /**
     * 특정 키워드를 구독 중인 유저 목록을 조회합니다.
     * @param keywordId 키워드 ID
     * @return 구독 유저 목록 응답
     */
    @GetMapping("/{keywordId}/subscribers")
    public ResponseEntity<UserKeywordSubscribersResponse> getUserSubscribers(@PathVariable Long keywordId) {
        List<SubscriberResponse> subscribers = keywordService.getSubscriberUsers(keywordId);
        return ResponseEntity.ok(UserKeywordSubscribersResponse.ok(subscribers));
    }

    /**
     * 키워드를 추가합니다.
     * @param jwt 유저 JWT 토큰
     * @param keyword 추가할 키워드 텍스트
     * @return 키워드 추가 완료 응답 (키워드 ID 반환)
     */
    @PostMapping
    public ResponseEntity<KeywordAddResponse> addKeyword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("keyword") @NotBlank String keyword) {
        Long userId = Long.parseLong(jwt.getSubject());
        Long keywordId = keywordService.addKeyword(userId, keyword);
        return ResponseEntity.ok(KeywordAddResponse.ok(keywordId));
    }

    /**
     * 키워드를 삭제합니다.
     * @param jwt 유저 JWT 토큰
     * @param keywordId 삭제할 키워드 ID
     * @return 성공 여부 응답
     */
    @DeleteMapping("/{keywordId}")
    public ResponseEntity<KeywordDeleteResponse> deleteKeyword(@AuthenticationPrincipal Jwt jwt, @PathVariable Long keywordId) {
        Long userId = Long.parseLong(jwt.getSubject());
        keywordService.deleteKeyword(userId, keywordId);
        return ResponseEntity.ok(KeywordDeleteResponse.ok());
    }
}
