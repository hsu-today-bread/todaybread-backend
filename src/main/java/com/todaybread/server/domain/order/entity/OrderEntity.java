package com.todaybread.server.domain.order.entity;

import com.todaybread.server.global.entity.BaseEntity;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 정보를 나타내는 엔티티입니다.
 * 유저가 장바구니 또는 바로 구매를 통해 생성한 주문을 관리합니다.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    @Builder
    private OrderEntity(Long userId, Long storeId, OrderStatus status, int totalAmount) {
        this.userId = userId;
        this.storeId = storeId;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    /**
     * 주문 상태를 변경합니다.
     * PENDING 상태에서만 변경이 가능합니다.
     *
     * @param newStatus 변경할 상태
     */
    public void updateStatus(OrderStatus newStatus) {
        if (this.status != OrderStatus.PENDING) {
            throw new CustomException(ErrorCode.ORDER_STATUS_CANNOT_CHANGE);
        }
        this.status = newStatus;
    }
}
