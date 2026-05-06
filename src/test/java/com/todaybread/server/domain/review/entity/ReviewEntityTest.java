package com.todaybread.server.domain.review.entity;

import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewEntityTest {

    @Test
    void create_withValidRating_succeeds() {
        for (int rating = 1; rating <= 5; rating++) {
            int r = rating;
            assertThatCode(() -> ReviewEntity.builder()
                    .userId(1L)
                    .storeId(1L)
                    .breadId(1L)
                    .orderItemId(1L)
                    .rating(r)
                    .content("맛있는 빵이에요! 추천합니다.")
                    .build()
            ).doesNotThrowAnyException();
        }
    }

    @Test
    void create_withValidRating_storesCorrectValues() {
        ReviewEntity review = ReviewEntity.builder()
                .userId(10L)
                .storeId(20L)
                .breadId(30L)
                .orderItemId(40L)
                .rating(4)
                .content("정말 맛있는 빵이에요!")
                .build();

        assertThat(review.getUserId()).isEqualTo(10L);
        assertThat(review.getStoreId()).isEqualTo(20L);
        assertThat(review.getBreadId()).isEqualTo(30L);
        assertThat(review.getOrderItemId()).isEqualTo(40L);
        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getContent()).isEqualTo("정말 맛있는 빵이에요!");
    }

    @Test
    void create_withRatingZero_throwsCustomException() {
        assertThatThrownBy(() -> ReviewEntity.builder()
                .userId(1L)
                .storeId(1L)
                .breadId(1L)
                .orderItemId(1L)
                .rating(0)
                .content("맛있는 빵이에요! 추천합니다.")
                .build()
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED);
    }

    @Test
    void create_withRatingSix_throwsCustomException() {
        assertThatThrownBy(() -> ReviewEntity.builder()
                .userId(1L)
                .storeId(1L)
                .breadId(1L)
                .orderItemId(1L)
                .rating(6)
                .content("맛있는 빵이에요! 추천합니다.")
                .build()
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED);
    }

    @Test
    void create_withNegativeRating_throwsCustomException() {
        assertThatThrownBy(() -> ReviewEntity.builder()
                .userId(1L)
                .storeId(1L)
                .breadId(1L)
                .orderItemId(1L)
                .rating(-1)
                .content("맛있는 빵이에요! 추천합니다.")
                .build()
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED);
    }
}
