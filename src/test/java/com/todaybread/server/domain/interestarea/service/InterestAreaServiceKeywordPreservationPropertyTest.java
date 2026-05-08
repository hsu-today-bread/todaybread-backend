package com.todaybread.server.domain.interestarea.service;

import com.todaybread.server.domain.interestarea.dto.InterestAreaDeleteResponse;
import com.todaybread.server.domain.interestarea.entity.InterestAreaEntity;
import com.todaybread.server.domain.interestarea.repository.InterestAreaRepository;
import com.todaybread.server.domain.keyword.repository.UserKeywordRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Property 3: 관심지역 삭제 시 키워드 보존
 *
 * For any 유저가 N개(N≥0)의 키워드를 보유한 상태에서 관심지역을 삭제하면,
 * 삭제 후에도 해당 유저의 UserKeyword 레코드 수는 N개로 유지되어야 한다.
 *
 * **Validates: Requirements 4.3**
 */
@Tag("Feature: keyword-area-notification, Property 3: 관심지역 삭제 시 키워드 보존")
class InterestAreaServiceKeywordPreservationPropertyTest {

    @Mock
    private InterestAreaRepository interestAreaRepository;

    @Mock
    private UserKeywordRepository userKeywordRepository;

    private InterestAreaService interestAreaService;

    @BeforeProperty
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interestAreaService = new InterestAreaService(interestAreaRepository, userKeywordRepository);
    }

    /**
     * Property 3: N개 키워드 보유 유저의 관심지역 삭제 후 UserKeyword 레코드 보존 검증
     *
     * deleteInterestArea()는 UserKeywordRepository의 delete 관련 메서드를 호출하지 않아야 하며,
     * 응답의 keywordNotificationDisabled는 N > 0일 때 true, N == 0일 때 false여야 한다.
     *
     * **Validates: Requirements 4.3**
     */
    @Property(tries = 100)
    void deleteInterestArea_preservesUserKeywords(
            @ForAll("keywordCounts") int keywordCount
    ) {
        // Arrange
        Long userId = 1L;
        Long entityId = 42L;

        InterestAreaEntity existingEntity = InterestAreaEntity.builder()
                .userId(userId)
                .name("테스트 지역")
                .address("서울시 강남구")
                .latitude(BigDecimal.valueOf(37.5))
                .longitude(BigDecimal.valueOf(127.0))
                .radiusKm(3.0)
                .build();
        ReflectionTestUtils.setField(existingEntity, "id", entityId);

        given(interestAreaRepository.findByUserId(userId))
                .willReturn(Optional.of(existingEntity));

        // N > 0이면 existsByUserId가 true, N == 0이면 false
        given(userKeywordRepository.existsByUserId(userId))
                .willReturn(keywordCount > 0);

        // Act
        InterestAreaDeleteResponse response = interestAreaService.deleteInterestArea(userId);

        // Assert 1: UserKeywordRepository의 delete 관련 메서드가 호출되지 않음 (키워드 보존)
        verify(userKeywordRepository, never()).delete(any());
        verify(userKeywordRepository, never()).deleteAll(any());
        verify(userKeywordRepository, never()).deleteById(anyLong());
        verify(userKeywordRepository, never()).deleteAll();

        // Assert 2: 응답의 keywordNotificationDisabled는 N > 0일 때 true
        assertThat(response.keywordNotificationDisabled()).isEqualTo(keywordCount > 0);

        // Assert 3: 삭제 성공
        assertThat(response.success()).isTrue();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 0~5 범위의 키워드 개수를 생성합니다.
     */
    @Provide
    Arbitrary<Integer> keywordCounts() {
        return Arbitraries.integers().between(0, 5);
    }
}
