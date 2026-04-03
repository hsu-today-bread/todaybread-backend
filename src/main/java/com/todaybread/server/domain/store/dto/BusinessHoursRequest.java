package com.todaybread.server.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * 요일별 영업시간 등록/수정 요청 DTO
 *
 * @param dayOfWeek 요일 (1=월, 2=화, 3=수, 4=목, 5=금, 6=토, 7=일)
 * @param isClosed 휴무 여부
 * @param startTime 영업 시작 시간
 * @param endTime 영업 종료 시간
 * @param lastOrderTime 마지막 주문 시간
 */
public record BusinessHoursRequest(
        @NotNull @Min(1) @Max(7) Integer dayOfWeek,
        @NotNull Boolean isClosed,
        @Schema(type = "string", format = "time", example = "09:00:00", description = "영업 시작 시간")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        LocalTime startTime,
        @Schema(type = "string", format = "time", example = "22:00:00", description = "영업 종료 시간")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        LocalTime endTime,
        @Schema(type = "string", format = "time", example = "21:30:00", description = "마지막 주문 시간")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        LocalTime lastOrderTime
) {
}
