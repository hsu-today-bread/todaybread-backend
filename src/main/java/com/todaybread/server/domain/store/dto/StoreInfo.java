package com.todaybread.server.domain.store.dto;

import com.todaybread.server.domain.store.entity.StoreEntity;

import java.math.BigDecimal;
import java.time.LocalTime;

public record StoreInfo(
        String name,
        String phone,
        String description,
        String addressLine1,
        String addressLine2,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalTime endTime,
        LocalTime lastOrderTime,
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
