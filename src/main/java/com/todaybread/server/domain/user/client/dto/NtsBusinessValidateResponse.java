package com.todaybread.server.domain.user.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 국세청 사업자등록정보 진위확인 응답입니다.
 *
 * @param statusCode 응답 상태 코드
 * @param requestCnt 요청 건수
 * @param matchCnt   매칭 건수
 * @param data       검증 결과 목록
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NtsBusinessValidateResponse(
        @JsonProperty("status_code") String statusCode,
        @JsonProperty("request_cnt") Integer requestCnt,
        @JsonProperty("match_cnt") Integer matchCnt,
        List<Result> data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            String valid,
            @JsonProperty("valid_msg") String validMessage,
            NtsBusinessStatus status
    ) {
    }
}
