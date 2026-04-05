package com.todaybread.server.domain.store.util;

import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SellingStatusUtilTest {

    @Test
    void isSelling_returnsTrueWithinBusinessHours() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        boolean result = SellingStatusUtil.isSelling(true, hours, true, LocalTime.of(16, 30));

        assertThat(result).isTrue();
    }

    @Test
    void isSelling_handlesOvernightBusinessHours() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(22, 0), LocalTime.of(2, 0), LocalTime.of(1, 0));

        boolean result = SellingStatusUtil.isSelling(true, hours, true, LocalTime.of(0, 30));

        assertThat(result).isTrue();
    }

    @Test
    void isSelling_returnsFalseWhenNoStock() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        boolean result = SellingStatusUtil.isSelling(true, hours, false, LocalTime.of(10, 0));

        assertThat(result).isFalse();
    }

    @Test
    void isSelling_clockOverloadUsesTodayBusinessHours() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-05T03:30:00Z"), ZoneId.of("Asia/Seoul"));
        StoreBusinessHoursEntity sundayHours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(12, 0), LocalTime.of(20, 0), LocalTime.of(19, 0));

        boolean result = SellingStatusUtil.isSelling(true, List.of(sundayHours), true, clock);

        assertThat(result).isTrue();
    }
}
