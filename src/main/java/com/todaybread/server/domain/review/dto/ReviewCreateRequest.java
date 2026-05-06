package com.todaybread.server.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 리뷰 작성 요청 DTO입니다.
 *
 * @param orderItemId 주문 항목 ID
 * @param rating 평점 (1~5)
 * @param content 리뷰 내용 (10~500자)
 */
public record ReviewCreateRequest(
        @NotNull Long orderItemId,
        @Min(1) @Max(5) int rating,
        @NotBlank @Size(min = 10, max = 500) String content
) {}
