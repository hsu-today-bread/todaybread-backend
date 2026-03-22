package com.todaybread.server.domain.store.dto;

import jakarta.validation.constraints.NotBlank;
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
public record StoreAddRequest (
        @NotBlank String name,
        @NotBlank String phone,
        @NotBlank String description,
        @NotBlank String addressLine1,
        @NotBlank String addressLine2,
        @NotBlank double latitude,
        @NotBlank double longitude,
        @NotBlank Time endTime,
        @NotBlank Time lastOrderTime,
        @NotBlank String orderTime
        ) {
}
