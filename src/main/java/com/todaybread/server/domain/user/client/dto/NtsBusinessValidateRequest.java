package com.todaybread.server.domain.user.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 국세청 사업자등록정보 진위확인 요청입니다.
 *
 * @param businesses 검증할 사업자 정보 목록
 */
public record NtsBusinessValidateRequest(
        List<Business> businesses
) {
    public static NtsBusinessValidateRequest of(String bossNumber, String businessStartDate,
                                                String representativeName) {
        return new NtsBusinessValidateRequest(
                List.of(new Business(bossNumber, businessStartDate, representativeName))
        );
    }

    /**
     * 국세청 API의 businesses 배열 원소입니다.
     *
     * @param bossNumber         사업자등록번호
     * @param businessStartDate  개업일자(yyyyMMdd)
     * @param representativeName 대표자명
     */
    public record Business(
            @JsonProperty("b_no") String bossNumber,
            @JsonProperty("start_dt") String businessStartDate,
            @JsonProperty("p_nm") String representativeName
    ) {
    }
}
