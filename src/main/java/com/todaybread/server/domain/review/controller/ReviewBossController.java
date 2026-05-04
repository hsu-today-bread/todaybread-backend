package com.todaybread.server.domain.review.controller;

import com.todaybread.server.domain.review.dto.BossReviewFilterType;
import com.todaybread.server.domain.review.dto.BossReviewResponse;
import com.todaybread.server.domain.review.dto.BossReviewSortType;
import com.todaybread.server.domain.review.service.ReviewService;
import com.todaybread.server.global.util.JwtRoleHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 사장님 전용 리뷰 관리 컨트롤러입니다.
 * 사장님이 자신의 가게에 달린 리뷰를 정렬/필터 옵션으로 조회할 수 있습니다.
 */
@RestController
@RequestMapping("/api/boss/review")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BOSS')")
public class ReviewBossController {

    private final ReviewService reviewService;

    /**
     * 사장님 가게 리뷰 관리 목록을 조회합니다.
     * <p>
     * 사장님이 소유한 가게의 리뷰를 정렬 및 필터 옵션에 따라 페이지네이션하여 반환합니다.
     * 각 리뷰에는 작성자의 해당 가게 구매 횟수(purchaseCount)가 함께 포함됩니다.
     *
     * @param jwt    인증된 JWT 토큰
     * @param sort   정렬 기준 (LATEST, OLDEST, RATING_HIGH, RATING_LOW; 기본값 LATEST)
     * @param filter 필터 기준 (ALL, WITH_IMAGE, TEXT_ONLY; 기본값 ALL)
     * @param page   페이지 번호 (기본값 0)
     * @param size   페이지 크기 (기본값 20)
     * @return 사장님 리뷰 응답 페이지
     */
    @Operation(summary = "사장님 가게 리뷰 관리 목록 조회")
    @GetMapping
    public Page<BossReviewResponse> getBossReviews(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return reviewService.getBossReviews(
                userId,
                BossReviewSortType.from(sort),
                BossReviewFilterType.from(filter),
                PageRequest.of(page, size));
    }
}
