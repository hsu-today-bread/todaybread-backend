package com.todaybread.server.domain.interestarea.dto;

/**
 * 관심지역 조회 래퍼 응답 DTO
 * 관심지역이 존재하면 interestArea에 상세 정보를, 존재하지 않으면 null을 반환한다.
 *
 * @param interestArea 관심지역 상세 정보 (없으면 null)
 */
public record InterestAreaWrapperResponse(
        InterestAreaResponse interestArea
) {
}
