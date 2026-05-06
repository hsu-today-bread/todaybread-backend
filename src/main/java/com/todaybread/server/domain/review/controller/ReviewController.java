package com.todaybread.server.domain.review.controller;

import com.todaybread.server.domain.review.dto.MyReviewResponse;
import com.todaybread.server.domain.review.dto.MyReviewSortType;
import com.todaybread.server.domain.review.dto.ReviewCreateRequest;
import com.todaybread.server.domain.review.dto.ReviewResponse;
import com.todaybread.server.domain.review.dto.ReviewSortType;
import com.todaybread.server.domain.review.dto.StoreReviewResponse;
import com.todaybread.server.domain.review.service.ReviewQueryService;
import com.todaybread.server.domain.review.service.ReviewService;
import com.todaybread.server.global.util.JwtRoleHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 일반 유저용 리뷰 컨트롤러입니다.
 * 리뷰 작성, 가게 리뷰 목록 조회, 내 리뷰 목록 조회 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewQueryService reviewQueryService;

    /**
     * 리뷰를 작성합니다.
     * <p>
     * 수령 완료(PICKED_UP) 상태의 주문 항목에 대해 평점, 리뷰 내용, 선택적 이미지를 포함하여
     * 리뷰를 작성합니다. 동일 주문 항목에 대해 중복 리뷰는 허용되지 않습니다.
     *
     * @param jwt     인증된 JWT 토큰
     * @param request 리뷰 작성 요청 (orderItemId, rating, content)
     * @param images  첨부 이미지 목록 (선택사항, 최대 2장)
     * @return 리뷰 작성 응답
     */
    @Operation(summary = "리뷰 작성")
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("request") @Valid ReviewCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return reviewService.createReview(userId, request, images);
    }

    /**
     * 가게 리뷰 목록을 조회합니다.
     * <p>
     * 특정 가게에 속한 모든 빵에 대한 리뷰를 정렬 옵션에 따라 페이지네이션하여 반환합니다.
     *
     * @param storeId 가게 ID
     * @param sort    정렬 기준 (LATEST, RATING_HIGH, RATING_LOW; 기본값 LATEST)
     * @param page    페이지 번호 (기본값 0)
     * @param size    페이지 크기 (기본값 20)
     * @return 가게 리뷰 응답 페이지
     */
    @Operation(summary = "가게 리뷰 목록 조회")
    @GetMapping("/store/{storeId}")
    public Page<StoreReviewResponse> getStoreReviews(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return reviewQueryService.getStoreReviews(storeId, ReviewSortType.from(sort),
                PageRequest.of(page, Math.min(size, 100)));
    }

    /**
     * 내 리뷰 목록을 조회합니다.
     * <p>
     * 인증된 사용자가 작성한 모든 리뷰를 정렬 옵션에 따라 페이지네이션하여 반환합니다.
     *
     * @param jwt  인증된 JWT 토큰
     * @param sort 정렬 기준 (LATEST, OLDEST; 기본값 LATEST)
     * @param page 페이지 번호 (기본값 0)
     * @param size 페이지 크기 (기본값 20)
     * @return 내 리뷰 응답 페이지
     */
    @Operation(summary = "내 리뷰 목록 조회")
    @GetMapping("/my")
    public Page<MyReviewResponse> getMyReviews(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return reviewQueryService.getMyReviews(userId, MyReviewSortType.from(sort),
                PageRequest.of(page, Math.min(size, 100)));
    }
}
