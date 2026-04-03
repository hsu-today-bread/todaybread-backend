package com.todaybread.server.domain.order.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문에 포함된 개별 빵 항목을 나타내는 엔티티입니다.
 * 주문 시점의 빵 이름, 가격, 수량을 스냅샷으로 저장합니다.
 */
@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "bread_id")
    private Long breadId;

    @Column(name = "bread_name", nullable = false, length = 100)
    private String breadName;

    @Column(name = "bread_price", nullable = false)
    private int breadPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Builder
    private OrderItemEntity(Long orderId, Long breadId, String breadName, int breadPrice, int quantity) {
        this.orderId = orderId;
        this.breadId = breadId;
        this.breadName = breadName;
        this.breadPrice = breadPrice;
        this.quantity = quantity;
    }
}
