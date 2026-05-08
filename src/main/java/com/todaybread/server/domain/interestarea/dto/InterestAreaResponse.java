package com.todaybread.server.domain.interestarea.dto;

import java.math.BigDecimal;

/**
 * 관심지역 상세 응답 DTO
 *
 * @param id 관심지역 ID
 * @param name 관심지역 이름
 * @param address 관심지역 주소
 * @param latitude 위도
 * @param longitude 경도
 * @param radiusKm 반경 (km)
 */
public record InterestAreaResponse(
        Long id,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        double radiusKm
) {
}
