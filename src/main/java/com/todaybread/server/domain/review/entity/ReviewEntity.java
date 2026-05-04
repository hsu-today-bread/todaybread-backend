package com.todaybread.server.domain.review.entity;

import com.todaybread.server.global.entity.BaseEntity;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 빵 리뷰를 정의하는 엔티티입니다.
 * 사용자가 수령 완료(PICKED_UP)한 주문의 빵에 대해 작성합니다.
 */
@Entity
@Table(name = "review",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_review_user_id_order_item_id",
           columnNames = {"user_id", "order_item_id"}
       ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "bread_id", nullable = false)
    private Long breadId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Builder
    private ReviewEntity(Long userId, Long storeId, Long breadId,
                         Long orderItemId, int rating, String content) {
        validateRating(rating);
        this.userId = userId;
        this.storeId = storeId;
        this.breadId = breadId;
        this.orderItemId = orderItemId;
        this.rating = rating;
        this.content = content;
    }

    /**
     * 평점이 1~5 범위인지 검증합니다.
     *
     * @param rating 평점
     */
    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new CustomException(ErrorCode.COMMON_REQUEST_VALIDATION_FAILED);
        }
    }
}
