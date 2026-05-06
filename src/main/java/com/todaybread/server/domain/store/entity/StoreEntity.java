package com.todaybread.server.domain.store.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * store 도메인 엔티티입니다.
 */
@Entity
@Table(name = "store", indexes = {
        @Index(name = "idx_store_lat_lng", columnList = "latitude, longitude"),
        @Index(name = "idx_store_is_active", columnList = "is_active")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 30, unique = true)
    private String phoneNumber;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "address_line1", nullable = false, length = 200)
    private String addressLine1;

    @Column(name = "address_line2", nullable = false, length = 200)
    private String addressLine2;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "rating_sum", nullable = false)
    private int ratingSum = 0;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Builder
    private StoreEntity(Long userId, String name, String phoneNumber,
                        String description, String addressLine1, String addressLine2,
                        BigDecimal latitude, BigDecimal longitude) {
        this.userId = userId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.description = description;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isActive = true;
    }

    /**
     * 가게 정보를 업데이트 시 사용합니다.
     *
     * @param name 이름
     * @param phone 전화번호
     * @param description 설명
     * @param addressLine1 주소1
     * @param addressLine2 주소2
     * @param latitude 위도
     * @param longitude 경도
     */
    public void updateInfo(String name, String phone, String description,
                           String addressLine1, String addressLine2, BigDecimal latitude,
                           BigDecimal longitude) {
        this.name = name;
        this.phoneNumber = phone;
        this.description = description;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * 리뷰 추가 시 평점 집계를 갱신합니다.
     * ratingSum에 평점을 더하고 reviewCount를 1 증가시킵니다.
     *
     * @param rating 추가된 리뷰의 평점 (1~5)
     */
    public void addReviewRating(int rating) {
        this.ratingSum += rating;
        this.reviewCount += 1;
    }

    /**
     * 리뷰 삭제 시 평점 집계를 차감합니다.
     * ratingSum에서 평점을 빼고 reviewCount를 1 감소시킵니다.
     *
     * @param rating 삭제된 리뷰의 평점 (1~5)
     */
    public void subtractReviewRating(int rating) {
        this.ratingSum -= rating;
        this.reviewCount -= 1;
    }

    /**
     * 평균 평점을 소수점 첫째 자리까지 반올림하여 반환합니다.
     * 리뷰가 없는 경우 0.0을 반환합니다.
     *
     * @return 평균 평점 (리뷰가 없으면 0.0)
     */
    public double getAverageRating() {
        if (this.reviewCount == 0) return 0.0;
        return Math.round((double) this.ratingSum / this.reviewCount * 10.0) / 10.0;
    }
}
