package com.todaybread.server.domain.store.entity;

import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Feature: bread-review, Property 13: 평점 집계 정확성
/**
 * Property 13: 평점 집계 정확성
 *
 * 가게에 N개의 리뷰가 순차적으로 추가된 후,
 * averageRating은 모든 리뷰 평점의 합을 N으로 나눈 값을 소수점 첫째 자리까지 반올림한 결과와 일치해야 하며,
 * reviewCount는 N과 일치해야 한다.
 *
 * **Validates: Requirements 5.3, 5.5**
 */
@Tag("Feature: bread-review")
class StoreEntityRatingPropertyTest {

    /**
     * Property 13a: 랜덤 rating 시퀀스를 addReviewRating()에 적용한 후
     * averageRating이 Math.round(sum/count * 10.0) / 10.0과 일치하고,
     * reviewCount가 리스트 크기와 일치해야 한다.
     *
     * **Validates: Requirements 5.3, 5.5**
     */
    @Property(tries = 100)
    @Tag("Property 13: 평점 집계 정확성")
    void averageRating_matchesExpectedCalculation(
            @ForAll("ratingLists") List<Integer> ratings
    ) {
        StoreEntity store = createStore();

        for (int rating : ratings) {
            store.addReviewRating(rating);
        }

        int expectedCount = ratings.size();
        assertThat(store.getReviewCount()).isEqualTo(expectedCount);

        if (expectedCount == 0) {
            assertThat(store.getAverageRating()).isEqualTo(0.0);
        } else {
            int sum = ratings.stream().mapToInt(Integer::intValue).sum();
            double expectedAvg = Math.round((double) sum / expectedCount * 10.0) / 10.0;
            assertThat(store.getAverageRating()).isEqualTo(expectedAvg);
        }
    }

    /**
     * Property 13b: 리뷰가 없는 가게의 averageRating은 0.0이고 reviewCount는 0이어야 한다.
     *
     * **Validates: Requirements 5.3, 5.5**
     */
    @Example
    @Tag("Property 13: 평점 집계 정확성")
    void emptyStore_returnsZeroAverageAndCount() {
        StoreEntity store = createStore();

        assertThat(store.getAverageRating()).isEqualTo(0.0);
        assertThat(store.getReviewCount()).isEqualTo(0);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    private StoreEntity createStore() {
        return StoreEntity.builder()
                .userId(1L)
                .name("테스트 빵집")
                .phoneNumber("010-1234-5678")
                .description("테스트 설명")
                .addressLine1("서울시 성북구")
                .addressLine2("1층")
                .latitude(BigDecimal.valueOf(37.5665))
                .longitude(BigDecimal.valueOf(126.9780))
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<List<Integer>> ratingLists() {
        return Arbitraries.integers().between(1, 5)
                .list()
                .ofMinSize(1)
                .ofMaxSize(50);
    }
}
