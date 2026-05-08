package com.todaybread.server.domain.interestarea.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.interestarea.dto.NotificationTarget;
import com.todaybread.server.domain.interestarea.dto.NotificationTargetResult;
import com.todaybread.server.domain.interestarea.entity.InterestAreaEntity;
import com.todaybread.server.domain.interestarea.repository.InterestAreaRepository;
import com.todaybread.server.domain.keyword.entity.KeywordEntity;
import com.todaybread.server.domain.keyword.entity.UserKeywordEntity;
import com.todaybread.server.domain.keyword.repository.KeywordRepository;
import com.todaybread.server.domain.keyword.repository.UserKeywordRepository;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;

/**
 * Property 11: 알림 결과 완전성
 *
 * For any 알림 대상 계산 결과의 각 NotificationTarget에는 userId, 매칭된 키워드 목록(1개 이상),
 * 매장 정보(storeId, storeName), 빵 정보(breadId, breadName), 라스트 오더까지 남은 시간(분, 0 이상)이
 * 모두 포함되어야 한다.
 *
 * **Validates: Requirements 6.10**
 */
@Tag("Feature: keyword-area-notification, Property 11: 알림 결과 완전성")
class NotificationTargetCompletenessPropertyTest {

    @Mock
    private InterestAreaRepository interestAreaRepository;

    @Mock
    private UserKeywordRepository userKeywordRepository;

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private StoreBusinessHoursRepository storeBusinessHoursRepository;

    private NotificationTargetCalculator calculator;

    @BeforeProperty
    void setUp() {
        MockitoAnnotations.openMocks(this);
        calculator = new NotificationTargetCalculator(
                interestAreaRepository, userKeywordRepository,
                keywordRepository, storeBusinessHoursRepository
        );
    }

    /**
     * Property 11: 알림 대상 결과의 각 NotificationTarget 필드 완전성 검증
     *
     * 유효한 시나리오(빵이 키워드와 매칭, 유저가 관심지역 3km 이내, 매장 영업 중)에서
     * calculateTargets() 호출 시 반환된 각 NotificationTarget의 모든 필드가 완전한지 검증한다.
     *
     * **Validates: Requirements 6.10**
     */
    @Property(tries = 100)
    void calculateTargets_resultFieldsAreComplete(
            @ForAll("validNotificationScenarios") NotificationScenario scenario
    ) {
        reset(interestAreaRepository, userKeywordRepository, keywordRepository, storeBusinessHoursRepository);

        // Arrange: 키워드 리포지토리 - 빵 이름에 매칭되는 키워드 반환
        given(keywordRepository.findAll()).willReturn(List.of(scenario.keyword));

        // Arrange: 유저-키워드 관계 반환
        given(userKeywordRepository.findByKeywordIdIn(any()))
                .willReturn(List.of(scenario.userKeyword));

        // Arrange: 관심지역 반환 (매장과 3km 이내)
        given(interestAreaRepository.findByUserIdIn(any()))
                .willReturn(List.of(scenario.interestArea));

        // Arrange: 영업시간 반환 (영업 중 상태)
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(scenario.store.getId()))
                .willReturn(List.of(scenario.businessHours));

        // Act
        NotificationTargetResult result = calculator.calculateTargets(
                scenario.bread, scenario.store, scenario.clock);

        // Assert: 결과가 비어있지 않아야 함 (유효한 시나리오이므로)
        assertThat(result.targets()).isNotEmpty();

        // Assert: 각 NotificationTarget의 필드 완전성 검증
        for (NotificationTarget target : result.targets()) {
            // userId is not null
            assertThat(target.userId()).isNotNull();

            // matchedKeywords is not null and has at least 1 entry
            assertThat(target.matchedKeywords()).isNotNull();
            assertThat(target.matchedKeywords()).isNotEmpty();
            assertThat(target.matchedKeywords().size()).isGreaterThanOrEqualTo(1);

            // storeInfo is not null (storeId and storeName are not null)
            assertThat(target.storeInfo()).isNotNull();
            assertThat(target.storeInfo().storeId()).isNotNull();
            assertThat(target.storeInfo().storeName()).isNotNull();

            // breadInfo is not null (breadId and breadName are not null)
            assertThat(target.breadInfo()).isNotNull();
            assertThat(target.breadInfo().breadId()).isNotNull();
            assertThat(target.breadInfo().breadName()).isNotNull();

            // minutesUntilLastOrder >= 0
            assertThat(target.minutesUntilLastOrder()).isGreaterThanOrEqualTo(0);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<NotificationScenario> validNotificationScenarios() {
        // 유저/매장/빵/키워드 ID
        Arbitrary<Long> userIds = Arbitraries.longs().between(1, 10000);
        Arbitrary<Long> storeIds = Arbitraries.longs().between(1, 10000);
        Arbitrary<Long> breadIds = Arbitraries.longs().between(1, 10000);
        Arbitrary<Long> keywordIds = Arbitraries.longs().between(1, 10000);

        // 키워드 텍스트 (1~5자, 한글)
        Arbitrary<String> keywordTexts = Arbitraries.strings()
                .ofMinLength(1).ofMaxLength(5)
                .withChars("가나다라마바사아자차카타파하빵떡쿠키")
                .filter(s -> !s.isBlank());

        // 매장 이름
        Arbitrary<String> storeNames = Arbitraries.strings()
                .ofMinLength(1).ofMaxLength(20)
                .alpha()
                .filter(s -> !s.isBlank());

        // 매장 좌표 (서울 근처)
        Arbitrary<BigDecimal> storeLats = Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(37.4), BigDecimal.valueOf(37.6))
                .ofScale(7);
        Arbitrary<BigDecimal> storeLons = Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(126.8), BigDecimal.valueOf(127.1))
                .ofScale(7);

        return Combinators.combine(
                userIds, storeIds, breadIds, keywordIds,
                keywordTexts, storeNames, storeLats, storeLons
        ).as((userId, storeId, breadId, keywordId,
              keywordText, storeName, storeLat, storeLon) -> {

            // 빵 이름: 키워드를 포함하도록 구성
            String breadName = "맛있는" + keywordText + "빵";

            // 빵 엔티티 (isDeleted=false, remainingQuantity>0)
            BreadEntity bread = BreadEntity.builder()
                    .storeId(storeId)
                    .name(breadName)
                    .description("설명")
                    .originalPrice(5000)
                    .salePrice(3000)
                    .remainingQuantity(5)
                    .build();
            ReflectionTestUtils.setField(bread, "id", breadId);

            // 매장 엔티티 (isActive=true)
            StoreEntity store = StoreEntity.builder()
                    .userId(999L)
                    .name(storeName)
                    .phoneNumber("02-1234-5678")
                    .description("매장 설명")
                    .addressLine1("주소1")
                    .addressLine2("주소2")
                    .latitude(storeLat)
                    .longitude(storeLon)
                    .build();
            ReflectionTestUtils.setField(store, "id", storeId);

            // 관심지역 (매장과 동일 좌표 → 거리 0km, 반드시 3km 이내)
            InterestAreaEntity interestArea = InterestAreaEntity.builder()
                    .userId(userId)
                    .name("내 동네")
                    .address("서울시 강남구")
                    .latitude(storeLat)
                    .longitude(storeLon)
                    .radiusKm(3.0)
                    .build();
            ReflectionTestUtils.setField(interestArea, "id", userId);

            // 키워드 엔티티 (normalisedText = 정규화된 키워드)
            String normalisedText = keywordText.replaceAll("\\s+", "").toLowerCase();
            KeywordEntity keyword = KeywordEntity.builder()
                    .normalisedText(normalisedText)
                    .build();
            ReflectionTestUtils.setField(keyword, "id", keywordId);

            // 유저-키워드 관계
            UserKeywordEntity userKeyword = UserKeywordEntity.builder()
                    .userId(userId)
                    .keywordId(keywordId)
                    .displayText(keywordText)
                    .build();
            ReflectionTestUtils.setField(userKeyword, "id", keywordId * 100);

            // 오늘 요일 기준 영업시간 (영업 중: 09:00~22:00, lastOrder 21:00)
            LocalDate today = LocalDate.of(2025, 1, 6); // 월요일
            int todayDow = today.getDayOfWeek().getValue(); // 1 (월)

            StoreBusinessHoursEntity businessHours = StoreBusinessHoursEntity.builder()
                    .storeId(storeId)
                    .dayOfWeek(todayDow)
                    .isClosed(false)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(22, 0))
                    .lastOrderTime(LocalTime.of(21, 0))
                    .build();
            ReflectionTestUtils.setField(businessHours, "id", storeId * 10L + todayDow);

            // Clock: 영업 시간 내 (14:00)
            Clock clock = Clock.fixed(
                    today.atTime(LocalTime.of(14, 0)).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                    ZoneId.of("Asia/Seoul")
            );

            return new NotificationScenario(
                    userId, bread, store, interestArea,
                    keyword, userKeyword, businessHours,
                    clock, todayDow
            );
        });
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test data class
    // ──────────────────────────────────────────────────────────────────────

    static class NotificationScenario {
        final Long userId;
        final BreadEntity bread;
        final StoreEntity store;
        final InterestAreaEntity interestArea;
        final KeywordEntity keyword;
        final UserKeywordEntity userKeyword;
        final StoreBusinessHoursEntity businessHours;
        final Clock clock;
        final int todayDow;

        NotificationScenario(Long userId, BreadEntity bread, StoreEntity store,
                             InterestAreaEntity interestArea, KeywordEntity keyword,
                             UserKeywordEntity userKeyword, StoreBusinessHoursEntity businessHours,
                             Clock clock, int todayDow) {
            this.userId = userId;
            this.bread = bread;
            this.store = store;
            this.interestArea = interestArea;
            this.keyword = keyword;
            this.userKeyword = userKeyword;
            this.businessHours = businessHours;
            this.clock = clock;
            this.todayDow = todayDow;
        }
    }
}
