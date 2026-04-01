package com.todaybread.server.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.sql.Time;

/**
 * 가게 등록을 위한 DTO
 *
 * @param name 가게 이름
 * @param phone 가게 전화번호
 * @param description 가게 설명
 * @param addressLine1 가게 주소1
 * @param addressLine2 가게 주소2
 * @param latitude 위도
 * @param longitude 경도
 * @param endTime 가게 종료 시간
 * @param lastOrderTime 라스트 오더 시간
 * @param orderTime 전체적인 영업 시간
 */
public record StoreCommonRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^0\\d{1,2}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.") String phone,
        @NotBlank String description,
        @NotBlank String addressLine1,
        @NotBlank String addressLine2,
        @NotNull @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.") @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.") BigDecimal latitude,
        @NotNull @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.") @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.") BigDecimal longitude,
        @Schema(type = "string", format = "time", example = "22:00:00", description = "가게 종료 시간")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @NotNull Time endTime,
        @Schema(type = "string", format = "time", example = "21:30:00", description = "라스트 오더 시간")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @NotNull Time lastOrderTime,
        @NotBlank String orderTime
        ) {
}
