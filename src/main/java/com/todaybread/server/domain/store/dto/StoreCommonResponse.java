package com.todaybread.server.domain.store.dto;

import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;

import java.math.BigDecimal;
import java.util.List;

/**
 * 가게 관련 공통 응답을 위해 사용됩니다.
 *
 * @param id 가게 ID
 * @param name 이름
 * @param phone 전화번호
 * @param description 설명
 * @param addressLine1 가게주소1
 * @param addressLine2 가게주소2
 * @param latitude 위도
 * @param longitude 경도
 * @param businessHours 요일별 영업시간 목록
 */
public record StoreCommonResponse(
        Long id,
        String name,
        String phone,
        String description,
        String addressLine1,
        String addressLine2,
        BigDecimal latitude,
        BigDecimal longitude,
        List<BusinessHoursResponse> businessHours
) {
    public static StoreCommonResponse from(StoreEntity storeEntity, List<StoreBusinessHoursEntity> businessHoursList) {
        List<BusinessHoursResponse> hours = businessHoursList.stream()
                .map(BusinessHoursResponse::from)
                .toList();

        return new StoreCommonResponse(
                storeEntity.getId(),
                storeEntity.getName(),
                storeEntity.getPhoneNumber(),
                storeEntity.getDescription(),
                storeEntity.getAddressLine1(),
                storeEntity.getAddressLine2(),
                storeEntity.getLatitude(),
                storeEntity.getLongitude(),
                hours
        );
    }
}
