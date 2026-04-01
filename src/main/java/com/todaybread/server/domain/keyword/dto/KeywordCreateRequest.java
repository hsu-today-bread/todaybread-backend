package com.todaybread.server.domain.keyword.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 키워드 등록 시 요청 DTO 입니다.
 *
 * @param keyword 키워드
 */
public record KeywordCreateRequest(
        @NotBlank String keyword
) {
}
