package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 사업자 등록 번호를 받는 요청 DTO입니다.
 *
 * @param bossNumber 사업자 등록 번호
 */
public record UserBossRequest (
        @NotBlank String bossNumber
) {
}
