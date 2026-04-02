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
        if (!isActive) {
            return false;
        }

        if (todayHours == null || todayHours.getIsClosed()) {
            return false;
        }

        if (todayHours.getStartTime() == null || todayHours.getEndTime() == null) {
            return false;
        }

        LocalTime startTime = todayHours.getStartTime();
        LocalTime endTime = todayHours.getEndTime();

        // startTime == endTime이면 24시간 영업
        if (startTime.equals(endTime)) {
            return hasStock;
        }

        boolean withinBusinessHours;

        if (!startTime.isAfter(endTime)) {
            // 일반 영업: startTime <= now < endTime
            withinBusinessHours = !now.isBefore(startTime) && now.isBefore(endTime);
        } else {
            // 자정 넘김 영업: now >= startTime OR now < endTime
            withinBusinessHours = !now.isBefore(startTime) || now.isBefore(endTime);
        }

        return withinBusinessHours && hasStock;
    }

    /**
     * 영업시간 목록에서 오늘 요일을 찾아 판매 상태를 판별합니다.
     * 오늘 요일 계산 + 영업시간 필터링 + isSelling 호출을 캡슐화합니다.
     *
     * @param isActive      가게 활성화 여부
     * @param businessHours 가게의 전체 영업시간 목록
     * @param hasStock      재고 존재 여부
     * @param clock         시간 소스
     * @return 판매중이면 true, 판매종료이면 false
     */
    public static boolean isSelling(boolean isActive, List<StoreBusinessHoursEntity> businessHours,
                                    boolean hasStock, Clock clock) {
        int todayDayOfWeek = LocalDate.now(clock).getDayOfWeek().getValue();
        StoreBusinessHoursEntity todayHours = businessHours.stream()
                .filter(h -> h.getDayOfWeek().equals(todayDayOfWeek))
                .findFirst()
                .orElse(null);
        return isSelling(isActive, todayHours, hasStock, LocalTime.now(clock));
    }
}
