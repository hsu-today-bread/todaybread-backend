package com.todaybread.server.domain.user.client.dto;

/**
 * 국세청 사업자 진위확인 성공 결과입니다.
 *
 * @param businessStatusCode 사업자 상태 코드
 * @param businessStatusName 사업자 상태명
 */
public record NtsBusinessValidationResult(
        String businessStatusCode,
        String businessStatusName
) {
}
