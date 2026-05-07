package com.todaybread.server.domain.store.util;

import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 가게 판매 상태를 판별하는 유틸리티 클래스입니다.
 */
public final class SellingStatusUtil {

    private SellingStatusUtil() {
        // 인스턴스화 방지
    }

    /**
     * 가게의 현재 판매 상태를 3단계로 판별합니다.
     *
     * @param isActive   가게 활성화 여부
     * @param todayHours 오늘 요일의 영업시간 정보 (null이면 CLOSED)
     * @param hasStock   재고 존재 여부
     * @param now        현재 시간
     * @return 판매 상태 (SELLING, OPEN_SOLD_OUT, CLOSED)
     */
    public static SellingStatus getSellingStatus(boolean isActive,
            StoreBusinessHoursEntity todayHours, boolean hasStock, LocalTime now) {
        // 1. CLOSED 조건 체크
        if (!isActive) return SellingStatus.CLOSED;
        if (todayHours == null || todayHours.getIsClosed()) return SellingStatus.CLOSED;
        if (todayHours.getStartTime() == null || todayHours.getEndTime() == null) return SellingStatus.CLOSED;

        LocalTime startTime = todayHours.getStartTime();
        LocalTime endTime = todayHours.getEndTime();

        // 2. cutoffTime 결정 (lastOrderTime은 등록 시 필수이므로 항상 non-null이지만 방어적으로 처리)
        LocalTime cutoffTime = todayHours.getLastOrderTime() != null
                ? todayHours.getLastOrderTime() : endTime;

        // 3. withinBusinessHours 계산
        boolean withinBusinessHours;
        if (!startTime.isAfter(cutoffTime)) {
            // 일반 영업: startTime <= now < cutoffTime
            withinBusinessHours = !now.isBefore(startTime) && now.isBefore(cutoffTime);
        } else {
            // 자정 넘김: now >= startTime OR now < cutoffTime
            withinBusinessHours = !now.isBefore(startTime) || now.isBefore(cutoffTime);
        }

        // 4. 결과
        if (!withinBusinessHours) return SellingStatus.CLOSED;
        return hasStock ? SellingStatus.SELLING : SellingStatus.OPEN_SOLD_OUT;
    }

    /**
     * 영업시간 목록에서 오늘 요일과 전날 요일을 고려하여 판매 상태를 판별합니다.
     * <p>
     * 자정 넘김 영업(예: 월요일 22:00~03:00)인 경우, 화요일 01:00에 조회하면
     * 오늘(화요일) row가 아닌 전날(월요일) row의 연장 구간으로 판별해야 합니다.
     * <p>
     * 우선순위:
     * 1. 오늘 row 영업시간 안이면 → 오늘 row 기준
     * 2. 전날 row가 자정 넘김이고 현재 시간이 전날 cutoffTime 이전이면 → 전날 row 기준
     * 3. 그 외 → CLOSED
     *
     * @param isActive      가게 활성화 여부
     * @param businessHours 가게의 전체 영업시간 목록
     * @param hasStock      재고 존재 여부
     * @param clock         시간 소스
     * @return 판매 상태 (SELLING, OPEN_SOLD_OUT, CLOSED)
     */
    public static SellingStatus getSellingStatus(boolean isActive,
            List<StoreBusinessHoursEntity> businessHours, boolean hasStock, Clock clock) {
        if (!isActive) return SellingStatus.CLOSED;

        int todayDayOfWeek = LocalDate.now(clock).getDayOfWeek().getValue();
        LocalTime now = LocalTime.now(clock);

        // 오늘 row 확인
        StoreBusinessHoursEntity todayHours = findByDayOfWeek(businessHours, todayDayOfWeek);
        SellingStatus todayResult = getSellingStatus(isActive, todayHours, hasStock, now);
        if (todayResult != SellingStatus.CLOSED) {
            return todayResult;
        }

        // 전날 row 확인 (자정 넘김 영업의 연장 구간)
        int yesterdayDayOfWeek = (todayDayOfWeek == 1) ? 7 : todayDayOfWeek - 1;
        StoreBusinessHoursEntity yesterdayHours = findByDayOfWeek(businessHours, yesterdayDayOfWeek);
        if (yesterdayHours != null && isOvernightAndWithinExtension(yesterdayHours, now)) {
            return hasStock ? SellingStatus.SELLING : SellingStatus.OPEN_SOLD_OUT;
        }

        return SellingStatus.CLOSED;
    }

    /**
     * 영업시간 목록에서 특정 요일의 row를 찾습니다.
     */
    private static StoreBusinessHoursEntity findByDayOfWeek(
            List<StoreBusinessHoursEntity> businessHours, int dayOfWeek) {
        return businessHours.stream()
                .filter(h -> h.getDayOfWeek().equals(dayOfWeek))
                .findFirst()
                .orElse(null);
    }

    /**
     * 전날 row가 자정 넘김 영업이고, 현재 시간이 전날의 cutoffTime 이전인지 확인합니다.
     * 자정 넘김 영업: startTime > cutoffTime (endTime 또는 lastOrderTime)
     */
    private static boolean isOvernightAndWithinExtension(
            StoreBusinessHoursEntity hours, LocalTime now) {
        if (hours.getIsClosed()) return false;
        if (hours.getStartTime() == null || hours.getEndTime() == null) return false;

        LocalTime startTime = hours.getStartTime();
        LocalTime cutoffTime = hours.getLastOrderTime() != null
                ? hours.getLastOrderTime() : hours.getEndTime();

        // 자정 넘김 여부: startTime > cutoffTime
        if (!startTime.isAfter(cutoffTime)) return false;

        // 현재 시간이 자정 이후 ~ cutoffTime 이전 구간에 있는지
        return now.isBefore(cutoffTime);
    }

    /**
     * 가게의 현재 판매 상태를 판별합니다.
     *
     * @param isActive   가게 활성화 여부
     * @param todayHours 오늘 요일의 영업시간 정보 (null이면 판매종료)
     * @param hasStock   재고 존재 여부
     * @param now        현재 시간
     * @return 판매중이면 true, 판매종료이면 false
     */
    public static boolean isSelling(boolean isActive, StoreBusinessHoursEntity todayHours,
                                    boolean hasStock, LocalTime now) {
        return getSellingStatus(isActive, todayHours, hasStock, now) == SellingStatus.SELLING;
    }

    /**
     * 영업시간 목록에서 오늘 요일을 찾아 판매 상태를 판별합니다.
     * 오늘 요일 계산 + 영업시간 필터링 + getSellingStatus 호출을 캡슐화합니다.
     *
     * @param isActive      가게 활성화 여부
     * @param businessHours 가게의 전체 영업시간 목록
     * @param hasStock      재고 존재 여부
     * @param clock         시간 소스
     * @return 판매중이면 true, 판매종료이면 false
     */
    public static boolean isSelling(boolean isActive, List<StoreBusinessHoursEntity> businessHours,
                                    boolean hasStock, Clock clock) {
        return getSellingStatus(isActive, businessHours, hasStock, clock) == SellingStatus.SELLING;
    }
}
