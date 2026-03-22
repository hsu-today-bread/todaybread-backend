package com.todaybread.server.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.todaybread.server.domain.store.entity.StoreEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.sql.Time;

public record StoreInfo(
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
    public static StoreInfo getStoreInfo(StoreEntity storeEntity) {
        return new StoreInfo(
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
