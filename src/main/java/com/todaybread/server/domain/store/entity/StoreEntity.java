package com.todaybread.server.domain.store.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Time;

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

    @Column(name = "end_time", nullable = false)
    private Time endTime;

    @Column(name = "last_order_time", nullable = false)
    private Time lastOrderTime;

    @Column(name = "order_time", nullable = false, length = 255)
    private String orderTime;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder
    private StoreEntity(Long userId, String name, String phoneNumber,
                        String description, String addressLine1, String addressLine2,
                        BigDecimal latitude, BigDecimal longitude, Time endTime,
                        Time lastOrderTime, String orderTime) {
        this.userId = userId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.description = description;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.latitude = latitude;
        this.longitude = longitude;
        this.endTime = endTime;
        this.lastOrderTime = lastOrderTime;
        this.orderTime = orderTime;
        this.isActive = true;
    }

    /**
     * 가게 정보를 업데이트 시 사용합니다.
     * @param name 이름
     * @param phone 전화번호
     * @param description 설명
     * @param addressLine1 주소1
     * @param addressLine2 주소2
     * @param latitude 위도
     * @param longitude 경도
     * @param endTime 마감 시간
     * @param lastOrderTime 라스트 오더 시간
     * @param orderTime 일반 영업 시간
     */
    public void updateInfo(String name, String phone, String description,
                           String addressLine1, String addressLine2, BigDecimal latitude,
                           BigDecimal longitude, Time endTime, Time lastOrderTime,
                           String orderTime) {
        this.name = name;
        this.phoneNumber = phone;
        this.description = description;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.latitude = latitude;
        this.longitude = longitude;
        this.endTime = endTime;
        this.lastOrderTime = lastOrderTime;
        this.orderTime = orderTime;
    }
}
