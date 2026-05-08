package com.todaybread.server.domain.interestarea.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 유저의 관심지역을 정의하는 엔티티입니다.
 * 유저당 1개의 관심지역만 등록할 수 있습니다.
 */
@Entity
@Table(name = "interest_area")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestAreaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "radius_km", nullable = false)
    private double radiusKm;

    @Builder
    private InterestAreaEntity(Long userId, String name, String address,
                               BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        this.userId = userId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = radiusKm;
    }

    public void updateInfo(String name, String address,
                           BigDecimal latitude, BigDecimal longitude) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
