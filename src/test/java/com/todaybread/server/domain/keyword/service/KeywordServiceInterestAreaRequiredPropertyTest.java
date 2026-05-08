package com.todaybread.server.domain.keyword.service;

import com.todaybread.server.domain.interestarea.repository.InterestAreaRepository;
import com.todaybread.server.domain.keyword.dto.KeywordCreateRequest;
import com.todaybread.server.domain.keyword.repository.KeywordRepository;
import com.todaybread.server.domain.keyword.repository.UserKeywordRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Property 10: 관심지역 미보유 시 키워드 등록 차단 (부작용 없음)
 *
 * For any 관심지역이 없는 유저가 키워드 등록을 시도하면,
 * INTEREST_AREA_REQUIRED 에러가 반환되고 KeywordEntity 및 UserKeywordEntity에
 * 어떠한 데이터도 생성되지 않아야 한다.
 *
 * **Validates: Requirements 5.1, 5.2**
 */
@Tag("Feature: keyword-area-notification, Property 10: 관심지역 미보유 시 키워드 등록 차단 (부작용 없음)")
class KeywordServiceInterestAreaRequiredPropertyTest {

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private UserKeywordRepository userKeywordRepository;

    @Mock
    private InterestAreaRepository interestAreaRepository;

    private KeywordService keywordService;

    @BeforeProperty
    void setUp() {
        MockitoAnnotations.openMocks(this);
        keywordService = new KeywordService(keywordRepository, userKeywordRepository, interestAreaRepository);
    }

    /**
     * Property 10: 관심지역 미보유 유저의 키워드 등록 시도 시
     * INTEREST_AREA_REQUIRED 에러 반환 및 KeywordEntity/UserKeywordEntity 미생성 검증
     *
     * **Validates: Requirements 5.1, 5.2**
     */
    @Property(tries = 100)
    void createKeyword_withoutInterestArea_throwsInterestAreaRequiredAndNoSideEffects(
            @ForAll("validKeywordStrings") String keyword
    ) {
        // Arrange
        Long userId = 1L;
        given(interestAreaRepository.existsByUserId(userId)).willReturn(false);

        // Act & Assert: INTEREST_AREA_REQUIRED 에러 반환
        assertThatThrownBy(() -> keywordService.createKeyword(userId, new KeywordCreateRequest(keyword)))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException customEx = (CustomException) ex;
                    assertThat(customEx.getErrorCode()).isEqualTo(ErrorCode.INTEREST_AREA_REQUIRED);
                });

        // Assert: KeywordRepository.save()가 호출되지 않음
        verify(keywordRepository, never()).save(any());

        // Assert: UserKeywordRepository.save()가 호출되지 않음
        verify(userKeywordRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 정규화 후 1~10자가 되는 유효한 키워드 문자열을 생성합니다.
     * 한글, 영문, 숫자 조합으로 공백 포함 가능 (정규화 시 공백 제거됨).
     */
    @Provide
    Arbitrary<String> validKeywordStrings() {
        // 한글 음절 범위 (가~힣) + 영문 소문자 + 숫자
        Arbitrary<Character> koreanChars = Arbitraries.chars().range('가', '힣');
        Arbitrary<Character> englishChars = Arbitraries.chars().range('a', 'z');
        Arbitrary<Character> digitChars = Arbitraries.chars().range('0', '9');

        Arbitrary<Character> contentChars = Arbitraries.oneOf(koreanChars, englishChars, digitChars);

        // 1~10자의 유효 문자열 생성 (정규화 후 길이 기준)
        return contentChars.list().ofMinSize(1).ofMaxSize(10)
                .map(chars -> {
                    StringBuilder sb = new StringBuilder();
                    for (Character c : chars) {
                        sb.append(c);
                    }
                    return sb.toString();
                })
                .filter(s -> {
                    // 정규화 후 1~10자인지 확인
                    String normalised = s.strip().replaceAll("\\s+", "");
                    return !normalised.isEmpty() && normalised.length() <= 10;
                });
    }
}
