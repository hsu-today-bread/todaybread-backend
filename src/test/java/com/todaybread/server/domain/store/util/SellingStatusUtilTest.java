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
    void isSelling_delegatesToGetSellingStatus() {
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));
        LocalTime now = LocalTime.of(12, 0);

        SellingStatus status = SellingStatusUtil.getSellingStatus(true, hours, true, now);
        boolean isSelling = SellingStatusUtil.isSelling(true, hours, true, now);

        assertThat(isSelling).isEqualTo(status == SellingStatus.SELLING);
    }

    // ===== 자정 넘김 영업 — List 기반 전날 row 고려 테스트 =====

    @Test
    void getSellingStatus_list_returnsSellingFromYesterdayOvernight() {
        // 토요일(6) 22:00~03:00 영업, 일요일(7) 01:00에 조회 → 토요일 row의 연장 구간
        // FIXED_CLOCK = 2026-04-05 12:00 KST (일요일, dayOfWeek=7)
        // 일요일 01:00 KST = 2026-04-04T16:00:00Z
        Clock sundayAt0100 = Clock.fixed(Instant.parse("2026-04-04T16:00:00Z"), ZoneId.of("Asia/Seoul"));
        StoreBusinessHoursEntity saturdayHours = TestFixtures.businessHours(1L, 6, false,
                LocalTime.of(22, 0), LocalTime.of(4, 0), LocalTime.of(3, 0));
        // 일요일 row는 없거나 다른 시간대
        StoreBusinessHoursEntity sundayHours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true,
                List.of(saturdayHours, sundayHours), true, sundayAt0100);

        assertThat(result).isEqualTo(SellingStatus.SELLING);
    }

    @Test
    void getSellingStatus_list_returnsOpenSoldOutFromYesterdayOvernight() {
        // 토요일(6) 22:00~03:00 영업, 일요일(7) 01:00에 조회, 재고 없음
        Clock sundayAt0100 = Clock.fixed(Instant.parse("2026-04-04T16:00:00Z"), ZoneId.of("Asia/Seoul"));
        StoreBusinessHoursEntity saturdayHours = TestFixtures.businessHours(1L, 6, false,
                LocalTime.of(22, 0), LocalTime.of(4, 0), LocalTime.of(3, 0));
        StoreBusinessHoursEntity sundayHours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true,
                List.of(saturdayHours, sundayHours), false, sundayAt0100);

        assertThat(result).isEqualTo(SellingStatus.OPEN_SOLD_OUT);
    }

    @Test
    void getSellingStatus_list_returnsClosedWhenPastYesterdayCutoff() {
        // 토요일(6) 22:00~03:00 (lastOrder 03:00), 일요일(7) 04:00에 조회 → cutoff 지남 → CLOSED
        // 일요일 04:00 KST = 2026-04-04T19:00:00Z
        Clock sundayAt0400 = Clock.fixed(Instant.parse("2026-04-04T19:00:00Z"), ZoneId.of("Asia/Seoul"));
        StoreBusinessHoursEntity saturdayHours = TestFixtures.businessHours(1L, 6, false,
                LocalTime.of(22, 0), LocalTime.of(4, 0), LocalTime.of(3, 0));
        StoreBusinessHoursEntity sundayHours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true,
                List.of(saturdayHours, sundayHours), true, sundayAt0400);

        assertThat(result).isEqualTo(SellingStatus.CLOSED);
    }

    @Test
    void getSellingStatus_list_prefersToday_whenWithinTodayHours() {
        // 토요일(6) 22:00~03:00 영업, 일요일(7) 12:00에 조회 → 오늘 row 영업시간 안
        Clock sundayAt1200 = Clock.fixed(Instant.parse("2026-04-05T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        StoreBusinessHoursEntity saturdayHours = TestFixtures.businessHours(1L, 6, false,
                LocalTime.of(22, 0), LocalTime.of(4, 0), LocalTime.of(3, 0));
        StoreBusinessHoursEntity sundayHours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true,
                List.of(saturdayHours, sundayHours), true, sundayAt1200);

        assertThat(result).isEqualTo(SellingStatus.SELLING);
    }

    @Test
    void getSellingStatus_list_returnsClosedWhenYesterdayNotOvernight() {
        // 토요일(6) 일반 영업 09:00~18:00, 일요일(7) 01:00에 조회 → 전날이 자정 넘김이 아님 → CLOSED
        Clock sundayAt0100 = Clock.fixed(Instant.parse("2026-04-04T16:00:00Z"), ZoneId.of("Asia/Seoul"));
        StoreBusinessHoursEntity saturdayHours = TestFixtures.businessHours(1L, 6, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));
        StoreBusinessHoursEntity sundayHours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true,
                List.of(saturdayHours, sundayHours), true, sundayAt0100);

        assertThat(result).isEqualTo(SellingStatus.CLOSED);
    }

    @Test
    void getSellingStatus_list_mondayOvernightIntoTuesday() {
        // 월요일(1) 22:00~03:00 영업, 화요일(2) 01:00에 조회 → 월요일 row의 연장 구간
        // 화요일 01:00 KST = 2026-03-30T16:00:00Z (2026-03-31 is Tuesday)
        Clock tuesdayAt0100 = Clock.fixed(Instant.parse("2026-03-30T16:00:00Z"), ZoneId.of("Asia/Seoul"));
        StoreBusinessHoursEntity mondayHours = TestFixtures.businessHours(1L, 1, false,
                LocalTime.of(22, 0), LocalTime.of(4, 0), LocalTime.of(3, 0));
        StoreBusinessHoursEntity tuesdayHours = TestFixtures.businessHours(1L, 2, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true,
                List.of(mondayHours, tuesdayHours), true, tuesdayAt0100);

        assertThat(result).isEqualTo(SellingStatus.SELLING);
    }

    @Test
    void getSellingStatus_list_sundayOvernightIntoMonday() {
        // 일요일(7) 22:00~03:00 영업, 월요일(1) 02:00에 조회 → 일요일 row의 연장 구간
        // 월요일 02:00 KST = 2026-04-05T17:00:00Z (2026-04-06 is Monday)
        Clock mondayAt0200 = Clock.fixed(Instant.parse("2026-04-05T17:00:00Z"), ZoneId.of("Asia/Seoul"));
        StoreBusinessHoursEntity sundayHours = TestFixtures.businessHours(1L, 7, false,
                LocalTime.of(22, 0), LocalTime.of(4, 0), LocalTime.of(3, 0));
        StoreBusinessHoursEntity mondayHours = TestFixtures.businessHours(1L, 1, false,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(17, 0));

        SellingStatus result = SellingStatusUtil.getSellingStatus(true,
                List.of(sundayHours, mondayHours), true, mondayAt0200);

        assertThat(result).isEqualTo(SellingStatus.SELLING);
    }
}
