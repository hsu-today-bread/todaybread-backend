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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Property 7: 알림 대상 유저 중복 제거
 *
 * For any 알림 대상 계산 결과에서, 동일 userId는 최대 1회만 포함되어야 한다.
 * 한 유저가 복수의 키워드로 매칭되더라도 결과 목록에 1회만 나타나야 한다.
 *
 * **Validates: Requirements 6.9**
 */
@Tag("Feature: keyword-area-notification, Property 7: 알림 대상 유저 중복 제거")
class NotificationTargetUserDeduplicationPropertyTest {

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
     * Property 7: 한 유저가 복수의 키워드로 동일 빵에 매칭되더라도
     * 알림 대상 결과에 해당 유저는 1회만 포함되어야 한다.
     * 또한 해당 유저의 matchedKeywords에는 매칭된 모든 키워드가 포함되어야 한다.
     *
     * **Validates: Requirements 6.9**
     */
    @Property(tries = 100)
    void singleUser_withMultipleMatchingKeywords_appearsOnlyOnceInResults(
            @ForAll("multipleMatchingKeywordsScenario") MultiKeywordScenario scenario
    ) {
        // Arrange: BreadEntity 설정 (isDeleted=false, remainingQuantity>0)
        BreadEntity bread = BreadEntity.builder()
                .storeId(1L)
                .name(scenario.breadName)
                .description("테스트 빵")
                .originalPrice(3000)
                .salePrice(2500)
                .remainingQuantity(5)
                .build();
        ReflectionTestUtils.setField(bread, "id", 1L);

        // Arrange: StoreEntity 설정 (관심지역 내 좌표)
        StoreEntity store = StoreEntity.builder()
                .userId(100L)
                .name("테스트 매장")
                .phoneNumber("010-1234-5678")
                .description("테스트")
                .addressLine1("서울시")
                .addressLine2("강남구")
                .latitude(BigDecimal.valueOf(37.5000))
                .longitude(BigDecimal.valueOf(127.0000))
                .build();
        ReflectionTestUtils.setField(store, "id", 1L);

        // Arrange: 영업시간 설정 (isSelling=true가 되도록)
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

        // Arrange: 키워드 엔티티 설정 (모두 빵 이름에 매칭되는 키워드)
        given(keywordRepository.findAll()).willReturn(scenario.keywords);

        // Arrange: UserKeyword 설정 (동일 유저가 모든 키워드를 등록)
        Long targetUserId = scenario.userId;
        List<UserKeywordEntity> userKeywords = new ArrayList<>();
        for (KeywordEntity keyword : scenario.keywords) {
            UserKeywordEntity userKeyword = UserKeywordEntity.builder()
                    .userId(targetUserId)
                    .keywordId(keyword.getId())
                    .displayText(keyword.getNormalisedText())
                    .build();
            userKeywords.add(userKeyword);
        }
        given(userKeywordRepository.findByKeywordIdIn(any()))
                .willReturn(userKeywords);

        // Arrange: InterestArea 설정 (매장과 동일 좌표 → 3km 이내)
        InterestAreaEntity interestArea = InterestAreaEntity.builder()
                .userId(targetUserId)
                .name("내 관심지역")
                .address("서울시 강남구")
                .latitude(BigDecimal.valueOf(37.5000))
                .longitude(BigDecimal.valueOf(127.0000))
                .radiusKm(3.0)
                .build();
        ReflectionTestUtils.setField(interestArea, "id", 1L);
        given(interestAreaRepository.findByUserIdIn(any()))
                .willReturn(List.of(interestArea));

        // Act
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // Assert: 결과에서 userId가 유일한지 검증
        List<Long> userIds = result.targets().stream()
                .map(NotificationTarget::userId)
                .collect(Collectors.toList());

        long distinctCount = userIds.stream().distinct().count();
        assertThat(distinctCount).isEqualTo(userIds.size())
                .as("알림 대상 결과에 동일 userId가 중복되면 안 됩니다");

        // Assert: 해당 유저가 정확히 1회만 포함됨
        long userOccurrences = userIds.stream()
                .filter(id -> id.equals(targetUserId))
                .count();
        assertThat(userOccurrences).isEqualTo(1L)
                .as("한 유저가 복수 키워드로 매칭되더라도 결과에 1회만 포함되어야 합니다");

        // Assert: 해당 유저의 matchedKeywords에 모든 매칭 키워드가 포함됨
        NotificationTarget target = result.targets().stream()
                .filter(t -> t.userId().equals(targetUserId))
                .findFirst()
                .orElseThrow();

        List<String> expectedKeywords = scenario.keywords.stream()
                .map(KeywordEntity::getNormalisedText)
                .collect(Collectors.toList());

        assertThat(target.matchedKeywords())
                .containsExactlyInAnyOrderElementsOf(expectedKeywords)
                .as("유저의 matchedKeywords에 매칭된 모든 키워드가 포함되어야 합니다");
    }

    // ──────────────────────────────────────────────────────────────────────
    // Scenario record
    // ──────────────────────────────────────────────────────────────────────

    record MultiKeywordScenario(
            String breadName,
            List<KeywordEntity> keywords,
            Long userId
    ) {}

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 복수 키워드 매칭 시나리오를 생성합니다.
     * 빵 이름에 여러 키워드가 부분 문자열로 포함되도록 구성합니다.
     */
    @Provide
    Arbitrary<MultiKeywordScenario> multipleMatchingKeywordsScenario() {
        // 2~4개의 한글 키워드 조각을 생성하고, 이를 조합하여 빵 이름을 만듦
        Arbitrary<List<String>> keywordTexts = koreanKeywordText()
                .list().ofMinSize(2).ofMaxSize(4)
                .filter(list -> list.stream().distinct().count() == list.size()); // 중복 키워드 제거

        Arbitrary<Long> userIds = Arbitraries.longs().between(1L, 10000L);

        return Combinators.combine(keywordTexts, userIds).as((texts, userId) -> {
            // 빵 이름: 모든 키워드를 포함하도록 조합
            String breadName = String.join("", texts) + "빵";

            // KeywordEntity 목록 생성 (각 키워드의 normalisedText가 빵 이름에 포함됨)
            List<KeywordEntity> keywords = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                KeywordEntity keyword = createKeywordEntity((long) (i + 1), texts.get(i));
                keywords.add(keyword);
            }

            return new MultiKeywordScenario(breadName, keywords, userId);
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

    /**
     * ReflectionTestUtils를 사용하여 KeywordEntity의 id를 설정합니다.
     */
    private KeywordEntity createKeywordEntity(Long id, String normalisedText) {
        KeywordEntity entity = KeywordEntity.builder()
                .normalisedText(normalisedText)
                .build();
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
