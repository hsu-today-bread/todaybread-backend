package com.todaybread.server.domain.notification.service;

import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.util.SellingStatusUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 알림 문구에 사용할 주문 가능 시간 텍스트를 생성합니다.
 */
@Component
@RequiredArgsConstructor
public class OrderAvailableTextFormatter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final StoreBusinessHoursRepository storeBusinessHoursRepository;
    private final Clock clock;

    /**
     * 현재 판매 가능 상태라면 라스트 오더 기준 주문 가능 시간 텍스트를 반환합니다.
     *
     * @param store    매장
     * @param hasStock 재고 보유 여부
     * @return 예: "20:00까지", 판매 불가면 empty
     */
    public Optional<String> format(StoreEntity store, boolean hasStock) {
        List<StoreBusinessHoursEntity> businessHours =
                storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(store.getId());

        if (!SellingStatusUtil.isSelling(store.getIsActive(), businessHours, hasStock, clock)) {
            return Optional.empty();
        }

        StoreBusinessHoursEntity activeHours = findActiveHours(businessHours);
        if (activeHours == null) {
            return Optional.empty();
        }

        LocalTime cutoffTime = activeHours.getLastOrderTime() != null
                ? activeHours.getLastOrderTime()
                : activeHours.getEndTime();
        if (cutoffTime == null) {
            return Optional.empty();
        }

        return Optional.of(cutoffTime.format(TIME_FORMATTER) + "까지");
    }

    private StoreBusinessHoursEntity findActiveHours(List<StoreBusinessHoursEntity> businessHours) {
        int todayDow = LocalDate.now(clock).getDayOfWeek().getValue();
        LocalTime now = LocalTime.now(clock);

        StoreBusinessHoursEntity todayHours = findByDayOfWeek(businessHours, todayDow);
        if (SellingStatusUtil.isSelling(true, todayHours, true, now)) {
            return todayHours;
        }

        int yesterdayDow = (todayDow == 1) ? 7 : todayDow - 1;
        StoreBusinessHoursEntity yesterdayHours = findByDayOfWeek(businessHours, yesterdayDow);
        if (isOvernightAndWithinExtension(yesterdayHours, now)) {
            return yesterdayHours;
        }

        return null;
    }

    private StoreBusinessHoursEntity findByDayOfWeek(List<StoreBusinessHoursEntity> businessHours, int dayOfWeek) {
        return businessHours.stream()
                .filter(hours -> hours.getDayOfWeek().equals(dayOfWeek))
                .findFirst()
                .orElse(null);
    }

    private boolean isOvernightAndWithinExtension(StoreBusinessHoursEntity hours, LocalTime now) {
        if (hours == null || hours.getIsClosed()) {
            return false;
        }
        if (hours.getStartTime() == null || hours.getEndTime() == null) {
            return false;
        }

        LocalTime cutoffTime = hours.getLastOrderTime() != null
                ? hours.getLastOrderTime()
                : hours.getEndTime();

        return hours.getStartTime().isAfter(cutoffTime) && now.isBefore(cutoffTime);
    }
}
