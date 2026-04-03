package com.todaybread.server.domain.store.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 근처 가게 목록 조회 응답 DTO입니다.
 * 지도 보기에서 가게 마커를 표시하기 위한 정보를 포함합니다.
 *
 * @param storeId           가게 ID
 * @param name              가게 이름
 * @param storeAddressLine1 주소1
 * @param storeAddressLine2 주소2
 * @param latitude          위도
 * @param longitude         경도
 * @param primaryImageUrl   대표 이미지 URL (없으면 null)
 * @param isSelling         판매중 여부
 * @param distance          유저~가게 거리 (km)
 * @param lastOrderTime     라스트오더 시간 (null이면 영업 종료 시간이 마감)
 */
public record NearbyStoreResponse(
        Long storeId,
        String name,
        String storeAddressLine1,
        String storeAddressLine2,
        BigDecimal latitude,
        BigDecimal longitude,
        String primaryImageUrl,
        boolean isSelling,
        double distance,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        LocalTime lastOrderTime
) {
}
