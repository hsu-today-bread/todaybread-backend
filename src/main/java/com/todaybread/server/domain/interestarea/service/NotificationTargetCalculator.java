package com.todaybread.server.domain.interestarea.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.interestarea.dto.*;
import com.todaybread.server.domain.interestarea.entity.InterestAreaEntity;
import com.todaybread.server.domain.interestarea.repository.InterestAreaRepository;
import com.todaybread.server.domain.interestarea.util.HaversineDistanceUtil;
import com.todaybread.server.domain.keyword.entity.KeywordEntity;
import com.todaybread.server.domain.keyword.entity.UserKeywordEntity;
import com.todaybread.server.domain.keyword.repository.KeywordRepository;
import com.todaybread.server.domain.keyword.repository.UserKeywordRepository;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.util.SellingStatusUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 빵 등록/품절 해제 시 알림 대상 유저를 계산하는 서비스입니다.
 * 전제조건 검증 → 키워드 매칭 → 관심지역 필터 → 거리 필터 → 결과 생성
 */
@Service
@RequiredArgsConstructor
public class NotificationTargetCalculator {

    private final InterestAreaRepository interestAreaRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final KeywordRepository keywordRepository;
    private final StoreBusinessHoursRepository storeBusinessHoursRepository;

    /**
     * 빵 등록/품절 해제 시 알림 대상을 계산한다.
     *
     * @param bread 대상 빵 엔티티
     * @param store 대상 매장 엔티티
     * @param clock 시간 소스 (테스트 용이성)
     * @return 알림 대상 결과 DTO (전제조건 미충족 시 빈 목록)
     */
    public NotificationTargetResult calculateTargets(BreadEntity bread, StoreEntity store, Clock clock) {
        // 1. 전제조건 검증: isDeleted, remainingQuantity
        if (bread.isDeleted() || bread.getRemainingQuantity() <= 0) {
            return emptyResult();
        }

        // 2. SellingStatusUtil로 영업 상태 판별
        List<StoreBusinessHoursEntity> businessHours =
                storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(store.getId());

        boolean isSelling = SellingStatusUtil.isSelling(
                store.getIsActive(), businessHours, bread.getRemainingQuantity() > 0, clock);

        if (!isSelling) {
            return emptyResult();
        }

        // 3. 빵 이름 정규화
        String normalizedBreadName = normaliseBreadName(bread.getName());
        if (normalizedBreadName.isEmpty()) {
            return emptyResult();
        }

        // 4. 매칭되는 키워드 찾기 (normalised_text가 정규화된 빵 이름에 부분 문자열로 포함)
        List<KeywordEntity> allKeywords = keywordRepository.findAll();
        List<KeywordEntity> matchedKeywords = allKeywords.stream()
                .filter(keyword -> normalizedBreadName.contains(keyword.getNormalisedText()))
                .toList();

        if (matchedKeywords.isEmpty()) {
            return emptyResult();
        }

        // 5. 매칭된 키워드 구독 유저를 일괄 조회 → userId별 매칭 키워드 그룹핑
        Map<Long, String> keywordTextById = matchedKeywords.stream()
                .collect(Collectors.toMap(KeywordEntity::getId, KeywordEntity::getNormalisedText));
        List<Long> matchedKeywordIds = matchedKeywords.stream()
                .map(KeywordEntity::getId)
                .toList();
        List<UserKeywordEntity> matchedUserKeywords = userKeywordRepository.findByKeywordIdIn(matchedKeywordIds);

        Map<Long, List<String>> userKeywordMap = new LinkedHashMap<>();
        for (UserKeywordEntity userKeyword : matchedUserKeywords) {
            String keywordText = keywordTextById.get(userKeyword.getKeywordId());
            if (keywordText == null) {
                continue;
            }
            userKeywordMap
                    .computeIfAbsent(userKeyword.getUserId(), k -> new ArrayList<>())
                    .add(keywordText);
        }

        if (userKeywordMap.isEmpty()) {
            return emptyResult();
        }

        // 6. interest_area 존재 유저 일괄 조회 + Haversine 거리 3km 이내 필터
        Map<Long, InterestAreaEntity> interestAreaByUserId = interestAreaRepository
                .findByUserIdIn(userKeywordMap.keySet())
                .stream()
                .collect(Collectors.toMap(InterestAreaEntity::getUserId, entity -> entity));

        double storeLat = store.getLatitude().doubleValue();
        double storeLon = store.getLongitude().doubleValue();

        List<NotificationTarget> targets = new ArrayList<>();
        for (Map.Entry<Long, List<String>> entry : userKeywordMap.entrySet()) {
            Long userId = entry.getKey();
            List<String> keywords = entry.getValue().stream().distinct().toList();

            InterestAreaEntity interestArea = interestAreaByUserId.get(userId);
            if (interestArea == null) {
                continue;
            }

            double userLat = interestArea.getLatitude().doubleValue();
            double userLon = interestArea.getLongitude().doubleValue();

            double distance = HaversineDistanceUtil.calculateDistance(userLat, userLon, storeLat, storeLon);
            if (distance > interestArea.getRadiusKm()) {
                continue;
            }

            // 7. 라스트 오더까지 남은 시간(분) 계산
            long minutesUntilLastOrder = calculateMinutesUntilLastOrder(businessHours, clock);

            targets.add(new NotificationTarget(
                    userId,
                    keywords,
                    new StoreInfo(store.getId(), store.getName()),
                    new BreadInfo(bread.getId(), bread.getName()),
                    minutesUntilLastOrder
            ));
        }

        return new NotificationTargetResult(targets);
    }

    /**
     * 재고 변경 이벤트에서 알림 대상 계산을 수행합니다.
     * 품절 상태(0개)에서 재고가 1개 이상으로 바뀐 경우에만 계산합니다.
     */
    public NotificationTargetResult calculateTargetsForStockChange(
            BreadEntity bread, StoreEntity store, int previousQuantity, int newQuantity, Clock clock) {
        if (!shouldTriggerForStockChange(previousQuantity, newQuantity)) {
            return emptyResult();
        }
        return calculateTargets(bread, store, clock);
    }

    /**
     * 재고 변경 알림 트리거 조건입니다.
     * 0개에서 1개 이상으로 바뀌는 품절 해제만 알림 계산 대상입니다.
     */
    public boolean shouldTriggerForStockChange(int previousQuantity, int newQuantity) {
        return previousQuantity == 0 && newQuantity >= 1;
    }

    /**
     * 빵 이름을 정규화합니다.
     * 모든 공백을 제거하고, 영문자가 포함된 경우 소문자로 변환합니다.
     * KeywordService.normalise()와 동일한 로직입니다.
     */
    String normaliseBreadName(String name) {
        if (name == null) {
            return "";
        }
        String stripped = name.replaceAll("\\s+", "");
        if (stripped.isEmpty()) {
            return "";
        }
        boolean hasEnglish = false;
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (Character.isLetter(c) && c < 128) {
                hasEnglish = true;
                break;
            }
        }
        if (hasEnglish) {
            return stripped.toLowerCase();
        }
        return stripped;
    }

    /**
     * 라스트 오더까지 남은 시간(분)을 계산합니다.
     * 오늘 영업시간 또는 전날 자정 넘김 영업의 lastOrderTime과 현재 시간의 차이를 분 단위로 반환합니다.
     * lastOrderTime이 없거나 이미 지난 경우 endTime을 기준으로 계산하고, 계산할 수 없으면 0을 반환합니다.
     */
    private long calculateMinutesUntilLastOrder(List<StoreBusinessHoursEntity> businessHours, Clock clock) {
        int todayDow = LocalDate.now(clock).getDayOfWeek().getValue();
        LocalTime now = LocalTime.now(clock);

        StoreBusinessHoursEntity todayHours = findByDayOfWeek(businessHours, todayDow);
        if (SellingStatusUtil.isSelling(true, todayHours, true, now)) {
            return calculateMinutesUntilCutoff(todayHours, now);
        }

        int yesterdayDow = (todayDow == 1) ? 7 : todayDow - 1;
        StoreBusinessHoursEntity yesterdayHours = findByDayOfWeek(businessHours, yesterdayDow);
        if (isOvernightAndWithinExtension(yesterdayHours, now)) {
            return calculateMinutesUntilCutoff(yesterdayHours, now);
        }

        return 0;
    }

    private StoreBusinessHoursEntity findByDayOfWeek(List<StoreBusinessHoursEntity> businessHours, int dayOfWeek) {
        return businessHours.stream()
                .filter(hours -> hours.getDayOfWeek().equals(dayOfWeek))
                .findFirst()
                .orElse(null);
    }

    private boolean isOvernightAndWithinExtension(StoreBusinessHoursEntity hours, LocalTime now) {
        if (hours == null || hours.getIsClosed() || hours.getStartTime() == null) {
            return false;
        }
        LocalTime cutoffTime = getCutoffTime(hours);
        if (cutoffTime == null) {
            return false;
        }
        return hours.getStartTime().isAfter(cutoffTime) && now.isBefore(cutoffTime);
    }

    private long calculateMinutesUntilCutoff(StoreBusinessHoursEntity hours, LocalTime now) {
        if (hours == null || hours.getStartTime() == null) {
            return 0;
        }
        LocalTime startTime = hours.getStartTime();
        LocalTime cutoffTime = getCutoffTime(hours);
        if (cutoffTime == null) {
            return 0;
        }

        LocalDate anchorDate = LocalDate.of(2000, 1, 1);
        LocalDateTime nowDateTime = anchorDate.atTime(now);
        LocalDateTime cutoffDateTime = anchorDate.atTime(cutoffTime);

        if (startTime.isAfter(cutoffTime) && !now.isBefore(startTime)) {
            cutoffDateTime = cutoffDateTime.plusDays(1);
        }

        if (!cutoffDateTime.isAfter(nowDateTime)) {
            return 0;
        }

        return ChronoUnit.MINUTES.between(nowDateTime, cutoffDateTime);
    }

    private LocalTime getCutoffTime(StoreBusinessHoursEntity hours) {
        return hours.getLastOrderTime() != null ? hours.getLastOrderTime() : hours.getEndTime();
    }

    private NotificationTargetResult emptyResult() {
        return new NotificationTargetResult(List.of());
    }
}
