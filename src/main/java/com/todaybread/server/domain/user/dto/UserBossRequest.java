package com.todaybread.server.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 사장님 등록을 위한 사업자 진위확인 요청 DTO입니다.
 *
 * @param bossNumber 사업자 등록 번호
 * @param businessStartDate 개업일자(yyyyMMdd)
 * @param representativeName 대표자명
 */
public record UserBossRequest (
        @NotBlank String bossNumber,

        @NotBlank
        @Pattern(regexp = "^\\d{8}$")
        String businessStartDate,

        @NotBlank
        @Size(max = 50)
        String representativeName
) {
}
