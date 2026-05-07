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

        // 2. cutoffTime 결정
        LocalTime cutoffTime = todayHours.getLastOrderTime() != null
                ? todayHours.getLastOrderTime() : endTime;

        // 3. 24시간 영업
        if (startTime.equals(cutoffTime)) {
            return hasStock ? SellingStatus.SELLING : SellingStatus.OPEN_SOLD_OUT;
        }

        // 4. withinBusinessHours 계산
        boolean withinBusinessHours;
        if (!startTime.isAfter(cutoffTime)) {
            // 일반 영업: startTime <= now < cutoffTime
            withinBusinessHours = !now.isBefore(startTime) && now.isBefore(cutoffTime);
        } else {
            // 자정 넘김: now >= startTime OR now < cutoffTime
            withinBusinessHours = !now.isBefore(startTime) || now.isBefore(cutoffTime);
        }

        // 5-6. 결과
        if (!withinBusinessHours) return SellingStatus.CLOSED;
        return hasStock ? SellingStatus.SELLING : SellingStatus.OPEN_SOLD_OUT;
    }

    /**
     * 영업시간 목록에서 오늘 요일을 찾아 판매 상태를 판별합니다.
     *
     * @param isActive      가게 활성화 여부
     * @param businessHours 가게의 전체 영업시간 목록
     * @param hasStock      재고 존재 여부
     * @param clock         시간 소스
     * @return 판매 상태 (SELLING, OPEN_SOLD_OUT, CLOSED)
     */
    public static SellingStatus getSellingStatus(boolean isActive,
            List<StoreBusinessHoursEntity> businessHours, boolean hasStock, Clock clock) {
        int todayDayOfWeek = LocalDate.now(clock).getDayOfWeek().getValue();
        StoreBusinessHoursEntity todayHours = businessHours.stream()
                .filter(h -> h.getDayOfWeek().equals(todayDayOfWeek))
                .findFirst()
                .orElse(null);
        return getSellingStatus(isActive, todayHours, hasStock, LocalTime.now(clock));
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
