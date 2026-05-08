package com.todaybread.server.domain.interestarea.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineDistanceUtilTest {

    @Test
    void calculateDistance_samePoint_returnsZero() {
        double distance = HaversineDistanceUtil.calculateDistance(37.5665, 126.9780, 37.5665, 126.9780);

        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    void calculateDistance_knownDistance_seoulToIncheon() {
        // 서울시청 (37.5665, 126.9780) → 인천시청 (37.4563, 126.7052)
        // 실제 직선 거리 약 26~27km
        double distance = HaversineDistanceUtil.calculateDistance(
                37.5665, 126.9780, 37.4563, 126.7052);

        assertThat(distance).isCloseTo(27.0, within(2.0));
    }

    @Test
    void calculateDistance_shortDistance_withinRadius() {
        // 서울역 (37.5547, 126.9707) → 남산타워 (37.5512, 126.9882)
        // 약 1.5km 이내
        double distance = HaversineDistanceUtil.calculateDistance(
                37.5547, 126.9707, 37.5512, 126.9882);

        assertThat(distance).isLessThan(3.0);
    }

    @Test
    void calculateDistance_isSymmetric() {
        double lat1 = 37.5665, lon1 = 126.9780;
        double lat2 = 35.1796, lon2 = 129.0756;

        double distanceAB = HaversineDistanceUtil.calculateDistance(lat1, lon1, lat2, lon2);
        double distanceBA = HaversineDistanceUtil.calculateDistance(lat2, lon2, lat1, lon1);

        assertThat(distanceAB).isEqualTo(distanceBA);
    }

    @Test
    void calculateDistance_alwaysNonNegative() {
        double distance = HaversineDistanceUtil.calculateDistance(
                -33.8688, 151.2093, 51.5074, -0.1278);

        assertThat(distance).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void calculateDistance_antipodal_returnsApproxHalfEarthCircumference() {
        // 대척점 (0,0) → (0,180) 약 20015km (지구 반둘레)
        double distance = HaversineDistanceUtil.calculateDistance(0.0, 0.0, 0.0, 180.0);

        assertThat(distance).isCloseTo(20015.0, within(100.0));
    }
}
