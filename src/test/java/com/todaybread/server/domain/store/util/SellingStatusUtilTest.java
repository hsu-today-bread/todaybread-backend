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

    // ===== 기존 isSelling() 테스트 =====

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

    // ===== getSellingStatus() 테스트 =====

    @Test
    void getSellingStatus_returnsClosed_whenInactive() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(false, hours, true, LocalTime.of(10, 0));

        assertThat(result).isEqualTo(SellingStatus.CLOSED);
    }

    @Test
    void getSellingStatus_returnsClosed_whenTodayHoursNull() {
        SellingStatus result = SellingStatusUtil.getSellingStatus(true, (StoreBusinessHoursEntity) null, true, LocalTime.of(10, 0));

        assertThat(result).isEqualTo(SellingStatus.CLOSED);
    }

    @Test
    void getSellingStatus_returnsClosed_whenIsClosed() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, true,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true, hours, true, LocalTime.of(10, 0));

        assertThat(result).isEqualTo(SellingStatus.CLOSED);
    }

    @Test
    void getSellingStatus_returnsClosed_whenStartTimeNull() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                null, LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true, hours, true, LocalTime.of(10, 0));

        assertThat(result).isEqualTo(SellingStatus.CLOSED);
    }

    @Test
    void getSellingStatus_returnsSelling_withinHoursWithStock() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true, hours, true, LocalTime.of(12, 0));

        assertThat(result).isEqualTo(SellingStatus.SELLING);
    }

    @Test
    void getSellingStatus_returnsOpenSoldOut_withinHoursNoStock() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true, hours, false, LocalTime.of(12, 0));

        assertThat(result).isEqualTo(SellingStatus.OPEN_SOLD_OUT);
    }

    @Test
    void getSellingStatus_returnsClosed_beforeBusinessHours() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true, hours, true, LocalTime.of(8, 0));

        assertThat(result).isEqualTo(SellingStatus.CLOSED);
    }

    @Test
    void getSellingStatus_returnsClosed_afterLastOrderTime() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true, hours, true, LocalTime.of(17, 30));

        assertThat(result).isEqualTo(SellingStatus.CLOSED);
    }

    @Test
    void getSellingStatus_returnsSelling_overnightWithinHours() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(22, 0), LocalTime.of(4, 0), LocalTime.of(3, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true, hours, true, LocalTime.of(1, 0));

        assertThat(result).isEqualTo(SellingStatus.SELLING);
    }

    @Test
    void getSellingStatus_returnsOpenSoldOut_overnightWithinHoursNoStock() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(22, 0), LocalTime.of(4, 0), LocalTime.of(3, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true, hours, false, LocalTime.of(23, 0));

        assertThat(result).isEqualTo(SellingStatus.OPEN_SOLD_OUT);
    }

    @Test
    void getSellingStatus_returnsClosed_overnightOutsideHours() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(22, 0), LocalTime.of(4, 0), LocalTime.of(3, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true, hours, true, LocalTime.of(10, 0));

        assertThat(result).isEqualTo(SellingStatus.CLOSED);
    }

    @Test
    void getSellingStatus_returnsSelling_24hours_withStock() {
        // 24시간 영업: startTime == cutoffTime (lastOrderTime이 없으면 endTime이 cutoff)
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(9, 0), null);

        SellingStatus result = SellingStatusUtil.getSellingStatus(true, hours, true, LocalTime.of(3, 0));

        assertThat(result).isEqualTo(SellingStatus.SELLING);
    }

    @Test
    void getSellingStatus_returnsOpenSoldOut_24hours_noStock() {
        // 24시간 영업: startTime == cutoffTime (lastOrderTime이 없으면 endTime이 cutoff)
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(9, 0), null);

        SellingStatus result = SellingStatusUtil.getSellingStatus(true, hours, false, LocalTime.of(15, 0));

        assertThat(result).isEqualTo(SellingStatus.OPEN_SOLD_OUT);
    }

    @Test
    void isSelling_delegatesToGetSellingStatus() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));
        LocalTime now = LocalTime.of(12, 0);

        SellingStatus status = SellingStatusUtil.getSellingStatus(true, hours, true, now);
        boolean isSelling = SellingStatusUtil.isSelling(true, hours, true, now);

        assertThat(isSelling).isEqualTo(status == SellingStatus.SELLING);
    }
}
