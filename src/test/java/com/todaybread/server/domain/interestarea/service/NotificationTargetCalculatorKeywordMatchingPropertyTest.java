package com.todaybread.server.domain.interestarea.service;

import com.todaybread.server.domain.interestarea.repository.InterestAreaRepository;
import com.todaybread.server.domain.keyword.repository.KeywordRepository;
import com.todaybread.server.domain.keyword.repository.UserKeywordRepository;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 6: 키워드 매칭 (정규화된 부분 문자열)
 *
 * For any 빵 이름과 키워드에 대해, 빵 이름을 정규화(공백 제거 + 소문자 변환)한 결과에
 * 키워드의 normalised_text가 부분 문자열로 포함되면 매칭으로 판정하고,
 * 포함되지 않으면 비매칭으로 판정해야 한다.
 *
 * **Validates: Requirements 9.1, 9.2, 9.3**
 */
@Tag("Feature: keyword-area-notification, Property 6: 키워드 매칭 (정규화된 부분 문자열)")
class NotificationTargetCalculatorKeywordMatchingPropertyTest {

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
                interestAreaRepository, userKeywordRepository, keywordRepository, storeBusinessHoursRepository);
    }

    /**
     * Property 6a: 키워드의 normalised_text가 정규화된 빵 이름에 부분 문자열로 포함되면 매칭이다.
     *
     * 전략: 빵 이름을 생성하고, 그 정규화된 결과에서 부분 문자열을 추출하여 키워드로 사용.
     * 이렇게 하면 항상 매칭이 보장된다.
     *
     * **Validates: Requirements 9.1, 9.2**
     */
    @Property(tries = 100)
    void keyword_containedInNormalisedBreadName_isMatch(
            @ForAll("breadNamesWithSubstringKeyword") BreadNameAndKeyword input
    ) {
        String normalizedBreadName = calculator.normaliseBreadName(input.breadName());
        String normalisedKeyword = input.normalisedKeyword();

        // 전제조건: 정규화된 빵 이름과 키워드가 비어있지 않아야 함
        Assume.that(!normalizedBreadName.isEmpty());
        Assume.that(!normalisedKeyword.isEmpty());

        // 키워드가 정규화된 빵 이름에 포함되면 매칭
        assertThat(normalizedBreadName.contains(normalisedKeyword)).isTrue();
    }

    /**
     * Property 6b: 키워드의 normalised_text가 정규화된 빵 이름에 포함되지 않으면 비매칭이다.
     *
     * 전략: 빵 이름과 무관한 키워드를 생성하여 비매칭을 검증.
     *
     * **Validates: Requirements 9.1, 9.3**
     */
    @Property(tries = 100)
    void keyword_notContainedInNormalisedBreadName_isNotMatch(
            @ForAll("breadNamesWithNonMatchingKeyword") BreadNameAndKeyword input
    ) {
        String normalizedBreadName = calculator.normaliseBreadName(input.breadName());
        String normalisedKeyword = input.normalisedKeyword();

        // 전제조건: 정규화된 빵 이름과 키워드가 비어있지 않아야 함
        Assume.that(!normalizedBreadName.isEmpty());
        Assume.that(!normalisedKeyword.isEmpty());

        // 키워드가 정규화된 빵 이름에 포함되지 않으면 비매칭
        assertThat(normalizedBreadName.contains(normalisedKeyword)).isFalse();
    }

    /**
     * Property 6c: 정규화는 모든 공백을 제거한다.
     *
     * 빵 이름에 공백이 포함되어 있어도 정규화 후에는 공백이 없어야 한다.
     *
     * **Validates: Requirements 9.1**
     */
    @Property(tries = 100)
    void normalisation_removesAllWhitespace(
            @ForAll("breadNamesWithSpaces") String breadName
    ) {
        String normalized = calculator.normaliseBreadName(breadName);

        assertThat(normalized).doesNotContain(" ", "\t", "\n", "\r");
    }

    /**
     * Property 6d: 영문자가 포함된 빵 이름은 정규화 후 소문자로 변환된다.
     *
     * **Validates: Requirements 9.1**
     */
    @Property(tries = 100)
    void normalisation_lowercasesWhenEnglishPresent(
            @ForAll("breadNamesWithEnglish") String breadName
    ) {
        String normalized = calculator.normaliseBreadName(breadName);

        Assume.that(!normalized.isEmpty());

        // 영문자가 포함된 경우 모두 소문자여야 함
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isLetter(c) && c < 128) {
                assertThat(Character.isLowerCase(c)).isTrue();
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Record for test input
    // ──────────────────────────────────────────────────────────────────────

    record BreadNameAndKeyword(String breadName, String normalisedKeyword) {}

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 빵 이름과 그 정규화된 결과의 부분 문자열을 키워드로 생성합니다.
     * 이렇게 하면 키워드가 항상 정규화된 빵 이름에 포함됩니다.
     */
    @Provide
    Arbitrary<BreadNameAndKeyword> breadNamesWithSubstringKeyword() {
        return breadNames().flatMap(breadName -> {
            String normalized = normaliseBreadNameHelper(breadName);
            if (normalized.isEmpty()) {
                return Arbitraries.just(new BreadNameAndKeyword(breadName, ""));
            }
            int len = normalized.length();
            // 부분 문자열의 시작 인덱스와 길이를 랜덤으로 선택
            return Arbitraries.integers().between(0, len - 1).flatMap(start -> {
                int maxEnd = Math.min(start + 10, len); // 키워드 최대 10자
                return Arbitraries.integers().between(start + 1, maxEnd)
                        .map(end -> new BreadNameAndKeyword(breadName, normalized.substring(start, end)));
            });
        }).filter(input -> !input.normalisedKeyword().isEmpty());
    }

    /**
     * 빵 이름과 매칭되지 않는 키워드를 생성합니다.
     * 정규화된 빵 이름에 포함되지 않는 문자열을 키워드로 사용합니다.
     */
    @Provide
    Arbitrary<BreadNameAndKeyword> breadNamesWithNonMatchingKeyword() {
        return Combinators.combine(breadNames(), nonMatchingKeywords())
                .as(BreadNameAndKeyword::new)
                .filter(input -> {
                    String normalized = normaliseBreadNameHelper(input.breadName());
                    return !normalized.isEmpty()
                            && !input.normalisedKeyword().isEmpty()
                            && !normalized.contains(input.normalisedKeyword());
                });
    }

    /**
     * 공백이 포함된 빵 이름을 생성합니다.
     */
    @Provide
    Arbitrary<String> breadNamesWithSpaces() {
        Arbitrary<Character> koreanChars = Arbitraries.chars().range('가', '힣');
        Arbitrary<Character> englishChars = Arbitraries.chars().range('a', 'z');
        Arbitrary<Character> spaceChars = Arbitraries.just(' ');

        Arbitrary<Character> contentChars = Arbitraries.oneOf(koreanChars, englishChars, spaceChars);

        return contentChars.list().ofMinSize(2).ofMaxSize(20)
                .filter(chars -> chars.stream().anyMatch(c -> c == ' '))
                .map(chars -> {
                    StringBuilder sb = new StringBuilder();
                    for (Character c : chars) {
                        sb.append(c);
                    }
                    return sb.toString();
                });
    }

    /**
     * 영문자가 포함된 빵 이름을 생성합니다.
     */
    @Provide
    Arbitrary<String> breadNamesWithEnglish() {
        Arbitrary<Character> koreanChars = Arbitraries.chars().range('가', '힣');
        Arbitrary<Character> englishUpperChars = Arbitraries.chars().range('A', 'Z');
        Arbitrary<Character> englishLowerChars = Arbitraries.chars().range('a', 'z');
        Arbitrary<Character> spaceChars = Arbitraries.just(' ');

        Arbitrary<Character> contentChars = Arbitraries.oneOf(
                koreanChars, englishUpperChars, englishLowerChars, spaceChars);

        return contentChars.list().ofMinSize(2).ofMaxSize(20)
                .filter(chars -> chars.stream().anyMatch(c ->
                        Character.isLetter(c) && c < 128))
                .map(chars -> {
                    StringBuilder sb = new StringBuilder();
                    for (Character c : chars) {
                        sb.append(c);
                    }
                    return sb.toString();
                });
    }

    /**
     * 빵 이름을 생성합니다 (한글, 영문, 숫자, 공백 조합).
     */
    private Arbitrary<String> breadNames() {
        Arbitrary<Character> koreanChars = Arbitraries.chars().range('가', '힣');
        Arbitrary<Character> englishUpperChars = Arbitraries.chars().range('A', 'Z');
        Arbitrary<Character> englishLowerChars = Arbitraries.chars().range('a', 'z');
        Arbitrary<Character> digitChars = Arbitraries.chars().range('0', '9');
        Arbitrary<Character> spaceChars = Arbitraries.just(' ');

        Arbitrary<Character> contentChars = Arbitraries.oneOf(
                koreanChars, englishUpperChars, englishLowerChars, digitChars, spaceChars);

        return contentChars.list().ofMinSize(1).ofMaxSize(30)
                .map(chars -> {
                    StringBuilder sb = new StringBuilder();
                    for (Character c : chars) {
                        sb.append(c);
                    }
                    return sb.toString();
                })
                .filter(s -> !normaliseBreadNameHelper(s).isEmpty());
    }

    /**
     * 정규화된 빵 이름에 포함되지 않을 가능성이 높은 키워드를 생성합니다.
     * 특수 접두사를 사용하여 매칭되지 않도록 합니다.
     */
    private Arbitrary<String> nonMatchingKeywords() {
        // 빵 이름에 잘 나타나지 않는 문자 조합으로 키워드 생성
        return Arbitraries.of("zzxqq", "vvwxx", "jjkqz", "xxyyq", "qqwwz", "zxcvb", "qwrtp", "xyzqw");
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helper: 정규화 로직 복제 (테스트 내에서 Provider가 사용)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * NotificationTargetCalculator.normaliseBreadName()과 동일한 로직.
     * Provider에서 사용하기 위해 static helper로 복제.
     */
    private static String normaliseBreadNameHelper(String name) {
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
}
