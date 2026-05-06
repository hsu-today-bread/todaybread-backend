package com.todaybread.server.domain.review.entity;

import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Feature: bread-review, Property 4: 리뷰 입력 검증
/**
 * Property 4: 리뷰 입력 검증
 *
 * 엔티티 레벨에서 rating 범위(1~5) 검증을 테스트합니다.
 * content 검증(10~500자)은 DTO 레벨의 Bean Validation에서 처리되므로
 * 이 테스트에서는 rating 검증에 집중합니다.
 *
 * **Validates: Requirements 1.5, 1.6**
 */
@Tag("Feature: bread-review")
class ReviewEntityPropertyTest {

    /**
     * Property 4a: 1~5 범위 밖의 rating으로 ReviewEntity를 생성하면
     * 항상 CustomException(COMMON_REQUEST_VALIDATION_FAILED)이 발생해야 한다.
     *
     * **Validates: Requirements 1.5**
     */
    @Property(tries = 100)
    @Tag("Property 4: 리뷰 입력 검증")
    void invalidRating_alwaysThrowsCustomException(
            @ForAll("invalidRatings") int invalidRating
    ) {
        assertThatThrownBy(() -> ReviewEntity.builder()
                .userId(1L)
                .storeId(1L)
                .breadId(1L)
                .orderItemId(1L)
                .rating(invalidRating)
                .content("맛있는 빵이에요! 추천합니다.")
                .build()
        )
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED);
                });
    }

    /**
     * Property 4b: 1~5 범위 내의 rating으로 ReviewEntity를 생성하면
     * 항상 성공하고, 저장된 rating이 입력값과 동일해야 한다.
     *
     * **Validates: Requirements 1.5**
     */
    @Property(tries = 100)
    @Tag("Property 4: 리뷰 입력 검증")
    void validRating_alwaysSucceeds(
            @ForAll("validRatings") int validRating
    ) {
        ReviewEntity review = ReviewEntity.builder()
                .userId(1L)
                .storeId(1L)
                .breadId(1L)
                .orderItemId(1L)
                .rating(validRating)
                .content("맛있는 빵이에요! 추천합니다.")
                .build();

        assertThat(review.getRating()).isEqualTo(validRating);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<Integer> invalidRatings() {
        return Arbitraries.oneOf(
                Arbitraries.integers().lessOrEqual(0),
                Arbitraries.integers().greaterOrEqual(6)
        );
    }

    @Provide
    Arbitrary<Integer> validRatings() {
        return Arbitraries.integers().between(1, 5);
    }
}
