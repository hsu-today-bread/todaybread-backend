package com.todaybread.server.domain.store.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * store 도메인 엔티티입니다.
 */
@Entity
@Table(name = "store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name ="name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 30, unique = true)
    private String phoneNumber;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "address_line1", nullable =false, length = 200)
    private String addressLine1;

    @Column(name = "address_line2", nullable = false, length = 200)
    private String addressLine2;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "last_order_time", nullable = false)
    private LocalTime lastOrderTime;

    @Column(name = "order_time", nullable = false)
    private String orderTime;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder
    private StoreEntity(Long userId, String name, String phoneNumber,
                        String description, String addressLine1, String addressLine2,
                        BigDecimal latitude, BigDecimal longitude, LocalTime endTime,
                        LocalTime lastOrderTime, String orderTime) {
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
}
