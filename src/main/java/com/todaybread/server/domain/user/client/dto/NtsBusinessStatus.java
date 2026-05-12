package com.todaybread.server.domain.user.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 국세청 사업자 상태 정보입니다.
 *
 * @param businessStatus     사업자 상태명
 * @param businessStatusCode 사업자 상태 코드
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NtsBusinessStatus(
        @JsonProperty("b_stt") String businessStatus,
        @JsonProperty("b_stt_cd") String businessStatusCode
) {
}
