package com.todaybread.server.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.todaybread.server.domain.store.entity.StoreEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.sql.Time;

/**
 * 가게 관련 공통 응답을 위해 사용됩니다.
 * @param name 이름
 * @param phone 전화번호
 * @param description 설명
 * @param addressLine1 가게주소1
 * @param addressLine2 가게주소2
 * @param latitude 위도
 * @param longitude 경도
 * @param endTime 마감 시간
 * @param lastOrderTime 라스트 오더 시간
 * @param orderTime 일반적인 영업 시간
 */
public record StoreCommonResponse(
        String name,
        String phone,
        String description,
        String addressLine1,
        String addressLine2,
        BigDecimal latitude,
        BigDecimal longitude,
        @Schema(type = "string", format = "time", example = "22:00:00", description = "가게 종료 시간")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        Time endTime,
        @Schema(type = "string", format = "time", example = "21:30:00", description = "라스트 오더 시간")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        Time lastOrderTime,
        String orderTime
) {
    public static StoreCommonResponse from(StoreEntity storeEntity) {
        return new StoreCommonResponse(
                storeEntity.getName(),
                storeEntity.getPhoneNumber(),
                storeEntity.getDescription(),
                storeEntity.getAddressLine1(),
                storeEntity.getAddressLine2(),
                storeEntity.getLatitude(),
                storeEntity.getLongitude(),
                storeEntity.getEndTime(),
                storeEntity.getLastOrderTime(),
                storeEntity.getOrderTime()
        );
    }
}
