package com.todaybread.server.domain.interestarea.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.interestarea.dto.*;
import com.todaybread.server.domain.interestarea.entity.InterestAreaEntity;
import com.todaybread.server.domain.interestarea.repository.InterestAreaRepository;
import com.todaybread.server.domain.keyword.entity.KeywordEntity;
import com.todaybread.server.domain.keyword.entity.UserKeywordEntity;
import com.todaybread.server.domain.keyword.repository.KeywordRepository;
import com.todaybread.server.domain.keyword.repository.UserKeywordRepository;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NotificationTargetCalculatorTest {

    @Mock
    private InterestAreaRepository interestAreaRepository;

    @Mock
    private UserKeywordRepository userKeywordRepository;

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private StoreBusinessHoursRepository storeBusinessHoursRepository;

    @InjectMocks
    private NotificationTargetCalculator calculator;

    // 고정 시간: 2024-01-15 (월요일) 10:00
    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDateTime.of(2024, 1, 15, 10, 0).toInstant(ZoneOffset.of("+09:00")),
            ZoneId.of("Asia/Seoul")
    );

    // --- Helper methods ---

    private static BreadEntity breadEntity(Long id, Long storeId, String name,
                                           int remainingQuantity, boolean isDeleted) {
        BreadEntity bread = BreadEntity.builder()
                .storeId(storeId)
                .name(name)
                .description("설명")
                .originalPrice(5000)
                .salePrice(3000)
                .remainingQuantity(remainingQuantity)
                .build();
        ReflectionTestUtils.setField(bread, "id", id);
        if (isDeleted) {
            bread.softDelete(LocalDateTime.now());
        }
        return bread;
    }

    private static StoreEntity storeEntity(Long id, String name,
                                           BigDecimal latitude, BigDecimal longitude, boolean isActive) {
        StoreEntity store = StoreEntity.builder()
                .userId(100L)
                .name(name)
                .phoneNumber("010-1234-5678")
                .description("매장 설명")
                .addressLine1("서울시 강남구")
                .addressLine2("1층")
                .latitude(latitude)
                .longitude(longitude)
                .build();
        ReflectionTestUtils.setField(store, "id", id);
        ReflectionTestUtils.setField(store, "isActive", isActive);
        return store;
    }

    private static StoreBusinessHoursEntity businessHours(Long storeId, int dayOfWeek,
                                                          LocalTime startTime, LocalTime endTime,
                                                          LocalTime lastOrderTime) {
        return StoreBusinessHoursEntity.builder()
                .storeId(storeId)
                .dayOfWeek(dayOfWeek)
                .isClosed(false)
                .startTime(startTime)
                .endTime(endTime)
                .lastOrderTime(lastOrderTime)
                .build();
    }

    private static KeywordEntity keywordEntity(Long id, String normalisedText) {
        KeywordEntity keyword = KeywordEntity.builder()
                .normalisedText(normalisedText)
                .build();
        ReflectionTestUtils.setField(keyword, "id", id);
        return keyword;
    }

    private static UserKeywordEntity userKeywordEntity(Long id, Long userId, Long keywordId) {
        UserKeywordEntity uk = UserKeywordEntity.builder()
                .userId(userId)
                .keywordId(keywordId)
                .displayText("키워드")
                .build();
        ReflectionTestUtils.setField(uk, "id", id);
        return uk;
    }

    private static InterestAreaEntity interestAreaEntity(Long id, Long userId,
                                                         BigDecimal latitude, BigDecimal longitude) {
        InterestAreaEntity entity = InterestAreaEntity.builder()
                .userId(userId)
                .name("관심지역")
                .address("서울시 강남구")
                .latitude(latitude)
                .longitude(longitude)
                .radiusKm(3.0)
                .build();
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    // --- Test cases ---

    @Test
    void whenBreadIsDeleted_returnsEmptyResult() {
        // given
        BreadEntity bread = breadEntity(1L, 1L, "소금빵", 5, true);
        StoreEntity store = storeEntity(1L, "베이커리", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), true);

        // when
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // then
        assertThat(result.targets()).isEmpty();
    }

    @Test
    void whenBreadRemainingQuantityIsZero_returnsEmptyResult() {
        // given
        BreadEntity bread = breadEntity(1L, 1L, "소금빵", 0, false);
        StoreEntity store = storeEntity(1L, "베이커리", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), true);

        // when
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // then
        assertThat(result.targets()).isEmpty();
    }

    @Test
    void whenSellingStatusUtilReturnsFalse_returnsEmptyResult() {
        // given: store.isActive = false → SellingStatusUtil.isSelling() returns false
        BreadEntity bread = breadEntity(1L, 1L, "소금빵", 5, false);
        StoreEntity store = storeEntity(1L, "베이커리", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), false);

        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(1L))
                .willReturn(List.of());

        // when
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // then
        assertThat(result.targets()).isEmpty();
    }

    @Test
    void whenBreadNameMatchesKeyword_userWithInterestAreaWithin3kmIsIncluded() {
        // given
        BreadEntity bread = breadEntity(1L, 1L, "소금빵", 5, false);
        // 매장 좌표: 강남역 (37.4979, 127.0276)
        StoreEntity store = storeEntity(1L, "강남베이커리",
                BigDecimal.valueOf(37.4979), BigDecimal.valueOf(127.0276), true);

        // 영업 중 설정 (월요일=1, 09:00~21:00, 라스트오더 20:00)
        StoreBusinessHoursEntity hours = businessHours(1L, 1,
                LocalTime.of(9, 0), LocalTime.of(21, 0), LocalTime.of(20, 0));
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(1L))
                .willReturn(List.of(hours));

        // 키워드 "소금빵" 매칭
        KeywordEntity keyword = keywordEntity(1L, "소금빵");
        given(keywordRepository.findAll()).willReturn(List.of(keyword));

        // 유저 1이 해당 키워드 구독
        UserKeywordEntity uk = userKeywordEntity(1L, 10L, 1L);
        given(userKeywordRepository.findByKeywordIdIn(any())).willReturn(List.of(uk));

        // 유저 10의 관심지역: 매장에서 1km 이내 (37.505, 127.027)
        InterestAreaEntity interestArea = interestAreaEntity(1L, 10L,
                BigDecimal.valueOf(37.505), BigDecimal.valueOf(127.027));
        given(interestAreaRepository.findByUserIdIn(any())).willReturn(List.of(interestArea));

        // when
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // then
        assertThat(result.targets()).hasSize(1);
        NotificationTarget target = result.targets().get(0);
        assertThat(target.userId()).isEqualTo(10L);
        assertThat(target.matchedKeywords()).containsExactly("소금빵");
    }

    @Test
    void whenUserInterestAreaIsBeyond3km_userIsExcluded() {
        // given
        BreadEntity bread = breadEntity(1L, 1L, "소금빵", 5, false);
        // 매장 좌표: 강남역
        StoreEntity store = storeEntity(1L, "강남베이커리",
                BigDecimal.valueOf(37.4979), BigDecimal.valueOf(127.0276), true);

        StoreBusinessHoursEntity hours = businessHours(1L, 1,
                LocalTime.of(9, 0), LocalTime.of(21, 0), LocalTime.of(20, 0));
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(1L))
                .willReturn(List.of(hours));

        KeywordEntity keyword = keywordEntity(1L, "소금빵");
        given(keywordRepository.findAll()).willReturn(List.of(keyword));

        UserKeywordEntity uk = userKeywordEntity(1L, 10L, 1L);
        given(userKeywordRepository.findByKeywordIdIn(any())).willReturn(List.of(uk));

        // 유저 10의 관심지역: 매장에서 약 10km 떨어진 곳 (37.58, 127.1)
        InterestAreaEntity interestArea = interestAreaEntity(1L, 10L,
                BigDecimal.valueOf(37.58), BigDecimal.valueOf(127.1));
        given(interestAreaRepository.findByUserIdIn(any())).willReturn(List.of(interestArea));

        // when
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // then
        assertThat(result.targets()).isEmpty();
    }

    @Test
    void whenUserHasNoInterestArea_userIsExcluded() {
        // given
        BreadEntity bread = breadEntity(1L, 1L, "소금빵", 5, false);
        StoreEntity store = storeEntity(1L, "강남베이커리",
                BigDecimal.valueOf(37.4979), BigDecimal.valueOf(127.0276), true);

        StoreBusinessHoursEntity hours = businessHours(1L, 1,
                LocalTime.of(9, 0), LocalTime.of(21, 0), LocalTime.of(20, 0));
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(1L))
                .willReturn(List.of(hours));

        KeywordEntity keyword = keywordEntity(1L, "소금빵");
        given(keywordRepository.findAll()).willReturn(List.of(keyword));

        UserKeywordEntity uk = userKeywordEntity(1L, 10L, 1L);
        given(userKeywordRepository.findByKeywordIdIn(any())).willReturn(List.of(uk));

        // 유저 10에게 관심지역 없음
        given(interestAreaRepository.findByUserIdIn(any())).willReturn(List.of());

        // when
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // then
        assertThat(result.targets()).isEmpty();
    }

    @Test
    void whenMultipleKeywordsMatchForSameUser_userAppearsOnceWithAllMatchedKeywords() {
        // given: 빵 이름 "소금크림빵" → "소금" 매칭, "크림" 매칭
        BreadEntity bread = breadEntity(1L, 1L, "소금크림빵", 5, false);
        StoreEntity store = storeEntity(1L, "강남베이커리",
                BigDecimal.valueOf(37.4979), BigDecimal.valueOf(127.0276), true);

        StoreBusinessHoursEntity hours = businessHours(1L, 1,
                LocalTime.of(9, 0), LocalTime.of(21, 0), LocalTime.of(20, 0));
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(1L))
                .willReturn(List.of(hours));

        KeywordEntity keyword1 = keywordEntity(1L, "소금");
        KeywordEntity keyword2 = keywordEntity(2L, "크림");
        given(keywordRepository.findAll()).willReturn(List.of(keyword1, keyword2));

        // 동일 유저(10)가 두 키워드 모두 구독
        UserKeywordEntity uk1 = userKeywordEntity(1L, 10L, 1L);
        UserKeywordEntity uk2 = userKeywordEntity(2L, 10L, 2L);
        given(userKeywordRepository.findByKeywordIdIn(any())).willReturn(List.of(uk1, uk2));

        // 유저 10의 관심지역: 매장 근처 (1km 이내)
        InterestAreaEntity interestArea = interestAreaEntity(1L, 10L,
                BigDecimal.valueOf(37.505), BigDecimal.valueOf(127.027));
        given(interestAreaRepository.findByUserIdIn(any())).willReturn(List.of(interestArea));

        // when
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // then: 유저 10은 1회만 포함, 매칭 키워드 2개
        assertThat(result.targets()).hasSize(1);
        NotificationTarget target = result.targets().get(0);
        assertThat(target.userId()).isEqualTo(10L);
        assertThat(target.matchedKeywords()).containsExactlyInAnyOrder("소금", "크림");
    }

    @Test
    void whenBreadNameNormalizesToEmpty_returnsEmptyResult() {
        // given: 빵 이름이 공백만으로 구성
        BreadEntity bread = breadEntity(1L, 1L, "   ", 5, false);
        StoreEntity store = storeEntity(1L, "베이커리",
                BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0), true);

        StoreBusinessHoursEntity hours = businessHours(1L, 1,
                LocalTime.of(9, 0), LocalTime.of(21, 0), LocalTime.of(20, 0));
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(1L))
                .willReturn(List.of(hours));

        // when
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // then
        assertThat(result.targets()).isEmpty();
    }

    @Test
    void resultDtoStructureVerification_allFieldsPopulatedCorrectly() {
        // given
        BreadEntity bread = breadEntity(1L, 1L, "소금빵", 5, false);
        StoreEntity store = storeEntity(1L, "강남베이커리",
                BigDecimal.valueOf(37.4979), BigDecimal.valueOf(127.0276), true);

        // 영업 중 (월요일 09:00~21:00, 라스트오더 20:00)
        StoreBusinessHoursEntity hours = businessHours(1L, 1,
                LocalTime.of(9, 0), LocalTime.of(21, 0), LocalTime.of(20, 0));
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(1L))
                .willReturn(List.of(hours));

        KeywordEntity keyword = keywordEntity(1L, "소금빵");
        given(keywordRepository.findAll()).willReturn(List.of(keyword));

        UserKeywordEntity uk = userKeywordEntity(1L, 10L, 1L);
        given(userKeywordRepository.findByKeywordIdIn(any())).willReturn(List.of(uk));

        // 유저 10의 관심지역: 매장 근처
        InterestAreaEntity interestArea = interestAreaEntity(1L, 10L,
                BigDecimal.valueOf(37.505), BigDecimal.valueOf(127.027));
        given(interestAreaRepository.findByUserIdIn(any())).willReturn(List.of(interestArea));

        // when
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // then: 모든 필드가 올바르게 채워져 있는지 검증
        assertThat(result.targets()).hasSize(1);
        NotificationTarget target = result.targets().get(0);

        // userId
        assertThat(target.userId()).isEqualTo(10L);

        // matchedKeywords (1개 이상)
        assertThat(target.matchedKeywords()).isNotEmpty();
        assertThat(target.matchedKeywords()).containsExactly("소금빵");

        // storeInfo
        assertThat(target.storeInfo()).isNotNull();
        assertThat(target.storeInfo().storeId()).isEqualTo(1L);
        assertThat(target.storeInfo().storeName()).isEqualTo("강남베이커리");

        // breadInfo
        assertThat(target.breadInfo()).isNotNull();
        assertThat(target.breadInfo().breadId()).isEqualTo(1L);
        assertThat(target.breadInfo().breadName()).isEqualTo("소금빵");

        // minutesUntilLastOrder (현재 10:00, 라스트오더 20:00 → 600분)
        assertThat(target.minutesUntilLastOrder()).isEqualTo(600L);
    }

    @Test
    void whenSellingDuringPreviousDayOvernightHours_minutesUntilLastOrderUsesPreviousDayHours() {
        // given: 일요일 01:00이지만 토요일 자정 넘김 영업의 연장 구간
        Clock overnightClock = Clock.fixed(
                LocalDateTime.of(2024, 1, 14, 1, 0).toInstant(ZoneOffset.of("+09:00")),
                ZoneId.of("Asia/Seoul")
        );

        BreadEntity bread = breadEntity(1L, 1L, "소금빵", 5, false);
        StoreEntity store = storeEntity(1L, "심야베이커리",
                BigDecimal.valueOf(37.4979), BigDecimal.valueOf(127.0276), true);

        StoreBusinessHoursEntity saturdayHours = businessHours(1L, 6,
                LocalTime.of(22, 0), LocalTime.of(4, 0), LocalTime.of(3, 0));
        StoreBusinessHoursEntity sundayHours = businessHours(1L, 7,
                LocalTime.of(9, 0), LocalTime.of(21, 0), LocalTime.of(20, 0));
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(1L))
                .willReturn(List.of(saturdayHours, sundayHours));

        KeywordEntity keyword = keywordEntity(1L, "소금빵");
        given(keywordRepository.findAll()).willReturn(List.of(keyword));

        UserKeywordEntity uk = userKeywordEntity(1L, 10L, 1L);
        given(userKeywordRepository.findByKeywordIdIn(any())).willReturn(List.of(uk));

        InterestAreaEntity interestArea = interestAreaEntity(1L, 10L,
                BigDecimal.valueOf(37.505), BigDecimal.valueOf(127.027));
        given(interestAreaRepository.findByUserIdIn(any())).willReturn(List.of(interestArea));

        // when
        NotificationTargetResult result = calculator.calculateTargets(bread, store, overnightClock);

        // then: 토요일 라스트오더 03:00까지 남은 시간 120분이어야 한다.
        assertThat(result.targets()).hasSize(1);
        assertThat(result.targets().get(0).minutesUntilLastOrder()).isEqualTo(120L);
    }
}
