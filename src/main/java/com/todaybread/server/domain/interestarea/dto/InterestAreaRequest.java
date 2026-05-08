package com.todaybread.server.domain.interestarea.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 관심지역 등록/수정 요청 DTO
 *
 * @param name 관심지역 이름 (1~50자)
 * @param address 관심지역 주소 (1~200자)
 * @param latitude 위도 (-90~90)
 * @param longitude 경도 (-180~180)
 */
public record InterestAreaRequest(
        @NotBlank @Size(min = 1, max = 50) String name,
        @NotBlank @Size(min = 1, max = 200) String address,
        @NotNull @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.") @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.") BigDecimal latitude,
        @NotNull @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.") @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.") BigDecimal longitude
) {
}
