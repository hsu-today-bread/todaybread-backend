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

/**
 * Property 9: 관심지역 재등록 시 알림 복원
 *
 * For any 유저가 관심지역 삭제 후 새 관심지역을 등록하면,
 * 이후 발생하는 빵 이벤트에 대해 해당 유저의 기존 키워드가 새 관심지역 반경 내 매장과
 * 매칭되어 알림 대상 계산에 포함되어야 한다.
 *
 * **Validates: Requirements 8.1, 8.2**
 */
@Tag("Feature: keyword-area-notification, Property 9: 관심지역 재등록 시 알림 복원")
class NotificationTargetReregistrationPropertyTest {

    @Mock
    private InterestAreaRepository interestAreaRepository;

    @Mock
    private UserKeywordRepository userKeywordRepository;

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private StoreBusinessHoursRepository storeBusinessHoursRepository;

    private NotificationTargetCalculator calculator;

    // 고정 시간: 수요일 10:00 (영업시간 내)
    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDateTime.of(2024, 1, 3, 10, 0) // 수요일
                    .toInstant(ZoneOffset.of("+09:00")),
            ZoneId.of("Asia/Seoul")
    );

    @BeforeProperty
    void setUp() {
        MockitoAnnotations.openMocks(this);
        calculator = new NotificationTargetCalculator(
                interestAreaRepository, userKeywordRepository,
                keywordRepository, storeBusinessHoursRepository
        );
    }

    /**
     * Property 9a: 관심지역이 없는 유저(삭제 후 상태)는 알림 대상에서 제외된다.
     *
     * 관심지역 삭제 후 interest_area row가 없으면 NotificationTargetCalculator는
     * 해당 유저를 알림 대상에서 제외해야 한다.
     *
     * **Validates: Requirements 8.1**
     */
    @Property(tries = 100)
    void userWithoutInterestArea_isExcludedFromNotificationTargets(
            @ForAll("reregistrationScenarios") ReregistrationScenario scenario
    ) {
        // Arrange: 빵과 매장 설정
        BreadEntity bread = createBread(scenario.breadName());
        StoreEntity store = createStore(scenario.storeLat(), scenario.storeLon());

        // Arrange: 영업시간 설정 (isSelling=true)
        setupBusinessHours();

        // Arrange: 키워드 설정 (빵 이름에 매칭되는 키워드)
        KeywordEntity keyword = createKeywordEntity(1L, scenario.keywordText());
        given(keywordRepository.findAll()).willReturn(List.of(keyword));

        // Arrange: UserKeyword 설정 (유저가 키워드를 보유)
        UserKeywordEntity userKeyword = UserKeywordEntity.builder()
                .userId(scenario.userId())
                .keywordId(keyword.getId())
                .displayText(keyword.getNormalisedText())
                .build();
        given(userKeywordRepository.findByKeywordIdInForUserNotificationTargets(any()))
                .willReturn(List.of(userKeyword));

        // Arrange: 관심지역 없음 (삭제된 상태 시뮬레이션)
        given(interestAreaRepository.findByUserIdIn(any()))
                .willReturn(List.of());

        // Act
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // Assert: 관심지역이 없으므로 유저는 알림 대상에 포함되지 않아야 한다
        assertThat(result.targets().stream()
                .map(NotificationTarget::userId)
                .toList())
                .as("관심지역이 없는 유저(삭제 후 상태)는 알림 대상에서 제외되어야 한다")
                .doesNotContain(scenario.userId());
    }

    /**
     * Property 9b: 관심지역을 재등록한 유저(3km 이내)는 기존 키워드로 알림 대상에 포함된다.
     *
     * 관심지역 재등록 후 interest_area row가 존재하고, 새 관심지역이 매장 반경 3km 이내이면
     * 기존 키워드가 매칭되어 알림 대상에 포함되어야 한다.
     *
     * **Validates: Requirements 8.1, 8.2**
     */
    @Property(tries = 100)
    void userWithReregisteredInterestArea_isIncludedInNotificationTargets(
            @ForAll("reregistrationScenarios") ReregistrationScenario scenario
    ) {
        // Arrange: 빵과 매장 설정
        BreadEntity bread = createBread(scenario.breadName());
        StoreEntity store = createStore(scenario.storeLat(), scenario.storeLon());

        // Arrange: 영업시간 설정 (isSelling=true)
        setupBusinessHours();

        // Arrange: 키워드 설정 (빵 이름에 매칭되는 키워드)
        KeywordEntity keyword = createKeywordEntity(1L, scenario.keywordText());
        given(keywordRepository.findAll()).willReturn(List.of(keyword));

        // Arrange: UserKeyword 설정 (유저가 키워드를 보유 - 삭제 전에 등록한 키워드)
        UserKeywordEntity userKeyword = UserKeywordEntity.builder()
                .userId(scenario.userId())
                .keywordId(keyword.getId())
                .displayText(keyword.getNormalisedText())
                .build();
        given(userKeywordRepository.findByKeywordIdInForUserNotificationTargets(any()))
                .willReturn(List.of(userKeyword));

        // Arrange: 관심지역 재등록 (매장과 동일 좌표 → 3km 이내)
        InterestAreaEntity interestArea = InterestAreaEntity.builder()
                .userId(scenario.userId())
                .name("재등록 관심지역")
                .address("서울시 강남구")
                .latitude(BigDecimal.valueOf(scenario.storeLat()))
                .longitude(BigDecimal.valueOf(scenario.storeLon()))
                .radiusKm(3.0)
                .build();
        ReflectionTestUtils.setField(interestArea, "id", 1L);
        given(interestAreaRepository.findByUserIdIn(any()))
                .willReturn(List.of(interestArea));

        // Act
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // Assert: 관심지역 재등록 후 기존 키워드로 알림 대상에 포함되어야 한다
        assertThat(result.targets().stream()
                .map(NotificationTarget::userId)
                .toList())
                .as("관심지역 재등록 후 기존 키워드가 매칭되면 알림 대상에 포함되어야 한다")
                .contains(scenario.userId());

        // Assert: 매칭된 키워드가 결과에 포함됨
        NotificationTarget target = result.targets().stream()
                .filter(t -> t.userId().equals(scenario.userId()))
                .findFirst()
                .orElseThrow();
        assertThat(target.matchedKeywords())
                .as("재등록 후 기존 키워드가 매칭 결과에 포함되어야 한다")
                .contains(scenario.keywordText());
    }

    // ──────────────────────────────────────────────────────────────────────
    // Scenario record
    // ──────────────────────────────────────────────────────────────────────

    record ReregistrationScenario(
            Long userId,
            String breadName,
            String keywordText,
            double storeLat,
            double storeLon
    ) {}

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 관심지역 재등록 시나리오를 생성합니다.
     * 빵 이름에 키워드가 포함되도록 구성합니다.
     */
    @Provide
    Arbitrary<ReregistrationScenario> reregistrationScenarios() {
        Arbitrary<Long> userIds = Arbitraries.longs().between(1L, 10000L);
        Arbitrary<String> keywordTexts = koreanKeywordText();
        Arbitrary<Double> latitudes = Arbitraries.doubles().between(33.0, 38.0);
        Arbitrary<Double> longitudes = Arbitraries.doubles().between(124.0, 132.0);

        return Combinators.combine(userIds, keywordTexts, latitudes, longitudes)
                .as((userId, keyword, lat, lon) -> {
                    // 빵 이름: 키워드를 포함하도록 구성
                    String breadName = keyword + "빵";
                    return new ReregistrationScenario(userId, breadName, keyword, lat, lon);
                });
    }

    /**
     * 한글 키워드 텍스트를 생성합니다 (2~5자).
     */
    private Arbitrary<String> koreanKeywordText() {
        Arbitrary<Character> koreanChars = Arbitraries.chars().range('가', '힣');
        return koreanChars.list().ofMinSize(2).ofMaxSize(5)
                .map(chars -> {
                    StringBuilder sb = new StringBuilder();
                    for (Character c : chars) {
                        sb.append(c);
                    }
                    return sb.toString();
                });
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helper methods
    // ──────────────────────────────────────────────────────────────────────

    private BreadEntity createBread(String name) {
        BreadEntity bread = BreadEntity.builder()
                .storeId(1L)
                .name(name)
                .description("테스트 빵")
                .originalPrice(3000)
                .salePrice(2500)
                .remainingQuantity(5)
                .build();
        ReflectionTestUtils.setField(bread, "id", 1L);
        return bread;
    }

    private StoreEntity createStore(double lat, double lon) {
        StoreEntity store = StoreEntity.builder()
                .userId(100L)
                .name("테스트 매장")
                .phoneNumber("010-1234-5678")
                .description("테스트")
                .addressLine1("서울시")
                .addressLine2("강남구")
                .latitude(BigDecimal.valueOf(lat))
                .longitude(BigDecimal.valueOf(lon))
                .build();
        ReflectionTestUtils.setField(store, "id", 1L);
        return store;
    }

    private void setupBusinessHours() {
        StoreBusinessHoursEntity businessHours = StoreBusinessHoursEntity.builder()
                .storeId(1L)
                .dayOfWeek(3) // 수요일
                .isClosed(false)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(22, 0))
                .lastOrderTime(LocalTime.of(21, 0))
                .build();

        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(1L))
                .willReturn(List.of(businessHours));
    }

    private KeywordEntity createKeywordEntity(Long id, String normalisedText) {
        KeywordEntity entity = KeywordEntity.builder()
                .normalisedText(normalisedText)
                .build();
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
