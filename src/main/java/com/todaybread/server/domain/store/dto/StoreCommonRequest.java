package com.todaybread.server.domain.store.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 가게 등록/수정을 위한 DTO
 *
 * @param name 가게 이름
 * @param phone 가게 전화번호
 * @param description 가게 설명
 * @param addressLine1 가게 주소1
 * @param addressLine2 가게 주소2
 * @param latitude 위도
 * @param longitude 경도
 * @param businessHours 요일별 영업시간 (7개, 월~일)
 */
public record StoreCommonRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^0\\d{1,2}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.") String phone,
        @NotBlank String description,
        @NotBlank String addressLine1,
        @NotBlank String addressLine2,
        @NotNull @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.") @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.") BigDecimal latitude,
        @NotNull @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.") @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.") BigDecimal longitude,
        @NotNull @Size(min = 7, max = 7) @Valid List<BusinessHoursRequest> businessHours
) {
}
