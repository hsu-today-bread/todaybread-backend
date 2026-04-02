package com.todaybread.server.domain.store.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 요일별 영업시간 정보를 저장하는 엔티티입니다.
 * store_business_hours 테이블에 매핑됩니다.
 */
@Entity
@Table(name = "store_business_hours", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_store_business_hours_store_id_day_of_week",
                columnNames = {"store_id", "day_of_week"}
        )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreBusinessHoursEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "last_order_time")
    private LocalTime lastOrderTime;

    @Builder
    private StoreBusinessHoursEntity(Long storeId, Integer dayOfWeek, Boolean isClosed,
                                     LocalTime startTime, LocalTime endTime, LocalTime lastOrderTime) {
        this.storeId = storeId;
        this.dayOfWeek = dayOfWeek;
        this.isClosed = isClosed;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lastOrderTime = lastOrderTime;
    }
}
