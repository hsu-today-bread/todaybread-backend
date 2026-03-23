package com.todaybread.server.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.sql.Time;

/**
 * 가게 등록을 위한 DTO
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
        @NotBlank String phone,
        @NotBlank String description,
        @NotBlank String addressLine1,
        @NotBlank String addressLine2,
        @NotNull BigDecimal latitude,
        @NotNull BigDecimal longitude,
        @Schema(type = "string", format = "time", example = "22:00:00", description = "가게 종료 시간")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @NotNull Time endTime,
        @Schema(type = "string", format = "time", example = "21:30:00", description = "라스트 오더 시간")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @NotNull Time lastOrderTime,
        @NotBlank String orderTime
        ) {
}
