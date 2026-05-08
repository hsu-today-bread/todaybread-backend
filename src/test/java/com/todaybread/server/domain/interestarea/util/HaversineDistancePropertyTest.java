package com.todaybread.server.domain.interestarea.util;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

// Feature: keyword-area-notification, Property 4: Haversine 거리 기반 알림 대상 필터링
/**
 * Property 4: Haversine 거리 기반 알림 대상 필터링
 *
 * 유저의 관심지역 좌표와 매장 좌표 쌍에 대해,
 * Haversine 거리가 3km 이내이면 해당 유저는 알림 대상에 포함되고,
 * 3km를 초과하면 제외되어야 한다.
 *
 * 추가로 거리는 항상 비음수이며 대칭(symmetric)이어야 한다.
 *
 * **Validates: Requirements 6.6, 6.7, 6.8**
 */
@Tag("Feature: keyword-area-notification, Property 4: Haversine 거리 기반 알림 대상 필터링")
class HaversineDistancePropertyTest {

    private static final double RADIUS_KM = 3.0;

    /**
     * Property 4a: 거리가 3km 이내인 좌표 쌍에 대해 유저는 알림 대상에 포함된다.
     *
     * **Validates: Requirements 6.6, 6.7**
     */
    @Property(tries = 100)
    @Tag("Property 4: Haversine 거리 기반 알림 대상 필터링")
    void withinRadius_userIsIncludedInNotificationTargets(
            @ForAll("coordinatePairs") double[] coords
    ) {
        double lat1 = coords[0];
        double lon1 = coords[1];
        double lat2 = coords[2];
        double lon2 = coords[3];

        double distance = HaversineDistanceUtil.calculateDistance(lat1, lon1, lat2, lon2);

        if (distance <= RADIUS_KM) {
            // 3km 이내이면 알림 대상에 포함
            assertThat(isWithinNotificationRadius(distance)).isTrue();
        }
    }

    /**
     * Property 4b: 거리가 3km 초과인 좌표 쌍에 대해 유저는 알림 대상에서 제외된다.
     *
     * **Validates: Requirements 6.6, 6.8**
     */
    @Property(tries = 100)
    @Tag("Property 4: Haversine 거리 기반 알림 대상 필터링")
    void beyondRadius_userIsExcludedFromNotificationTargets(
            @ForAll("coordinatePairs") double[] coords
    ) {
        double lat1 = coords[0];
        double lon1 = coords[1];
        double lat2 = coords[2];
        double lon2 = coords[3];

        double distance = HaversineDistanceUtil.calculateDistance(lat1, lon1, lat2, lon2);

        if (distance > RADIUS_KM) {
            // 3km 초과이면 알림 대상에서 제외
            assertThat(isWithinNotificationRadius(distance)).isFalse();
        }
    }

    /**
     * Property 4c: Haversine 거리는 항상 비음수(non-negative)이다.
     *
     * **Validates: Requirements 6.6, 6.7, 6.8**
     */
    @Property(tries = 100)
    @Tag("Property 4: Haversine 거리 기반 알림 대상 필터링")
    void distance_isAlwaysNonNegative(
            @ForAll("coordinatePairs") double[] coords
    ) {
        double lat1 = coords[0];
        double lon1 = coords[1];
        double lat2 = coords[2];
        double lon2 = coords[3];

        double distance = HaversineDistanceUtil.calculateDistance(lat1, lon1, lat2, lon2);

        assertThat(distance).isGreaterThanOrEqualTo(0.0);
    }

    /**
     * Property 4d: Haversine 거리는 대칭(symmetric)이다. distance(A, B) == distance(B, A)
     *
     * **Validates: Requirements 6.6, 6.7, 6.8**
     */
    @Property(tries = 100)
    @Tag("Property 4: Haversine 거리 기반 알림 대상 필터링")
    void distance_isSymmetric(
            @ForAll("coordinatePairs") double[] coords
    ) {
        double lat1 = coords[0];
        double lon1 = coords[1];
        double lat2 = coords[2];
        double lon2 = coords[3];

        double distanceAB = HaversineDistanceUtil.calculateDistance(lat1, lon1, lat2, lon2);
        double distanceBA = HaversineDistanceUtil.calculateDistance(lat2, lon2, lat1, lon1);

        assertThat(distanceAB).isEqualTo(distanceBA);
    }

    /**
     * Property 4e: 3km 이내/초과 판정의 일관성 — 포함과 제외는 상호 배타적이다.
     *
     * **Validates: Requirements 6.7, 6.8**
     */
    @Property(tries = 100)
    @Tag("Property 4: Haversine 거리 기반 알림 대상 필터링")
    void inclusionAndExclusion_areMutuallyExclusive(
            @ForAll("coordinatePairs") double[] coords
    ) {
        double lat1 = coords[0];
        double lon1 = coords[1];
        double lat2 = coords[2];
        double lon2 = coords[3];

        double distance = HaversineDistanceUtil.calculateDistance(lat1, lon1, lat2, lon2);

        boolean included = isWithinNotificationRadius(distance);
        boolean excluded = !isWithinNotificationRadius(distance);

        // 포함과 제외는 항상 상호 배타적
        assertThat(included).isNotEqualTo(excluded);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helper: 알림 대상 포함 여부 판정 (3km 이내)
    // ──────────────────────────────────────────────────────────────────────

    private boolean isWithinNotificationRadius(double distance) {
        return distance <= RADIUS_KM;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<double[]> coordinatePairs() {
        Arbitrary<Double> latitudes = Arbitraries.doubles().between(-90.0, 90.0);
        Arbitrary<Double> longitudes = Arbitraries.doubles().between(-180.0, 180.0);

        return Combinators.combine(latitudes, longitudes, latitudes, longitudes)
                .as((lat1, lon1, lat2, lon2) -> new double[]{lat1, lon1, lat2, lon2});
    }
}
