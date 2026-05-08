package com.todaybread.server.domain.interestarea.util;

/**
 * 두 좌표 간의 Haversine 거리를 계산하는 유틸리티 클래스입니다.
 */
public final class HaversineDistanceUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private HaversineDistanceUtil() {
        // 인스턴스화 방지
    }

    /**
     * 두 좌표 간의 Haversine 거리를 km 단위로 계산한다.
     *
     * @param lat1 첫 번째 지점의 위도 (도 단위)
     * @param lon1 첫 번째 지점의 경도 (도 단위)
     * @param lat2 두 번째 지점의 위도 (도 단위)
     * @param lon2 두 번째 지점의 경도 (도 단위)
     * @return 두 좌표 간의 거리 (km)
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
