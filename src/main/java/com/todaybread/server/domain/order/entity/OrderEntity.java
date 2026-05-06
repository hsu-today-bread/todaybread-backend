package com.todaybread.server.domain.order.entity;

import com.todaybread.server.global.entity.BaseEntity;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * 주문 정보를 나타내는 엔티티입니다.
 * 유저가 장바구니 또는 바로 구매를 통해 생성한 주문을 관리합니다.
 */
@Entity
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orders_user_id_idempotency_key",
                columnNames = {"user_id", "idempotency_key"}
        )
)
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

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "order_number", length = 4)
    private String orderNumber;

    @Column(name = "order_date")
    private LocalDate orderDate;

    /** 허용된 상태 전이 맵 */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PICKED_UP, OrderStatus.CANCELLED, OrderStatus.CANCEL_PENDING),
            OrderStatus.CANCEL_PENDING, Set.of(OrderStatus.CANCELLED, OrderStatus.CONFIRMED)
    );

    @Builder
    private OrderEntity(Long userId, Long storeId, OrderStatus status, int totalAmount, String idempotencyKey) {
        this.userId = userId;
        this.storeId = storeId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * 주문 상태를 변경합니다.
     * 허용된 전환: PENDING→CONFIRMED, PENDING→CANCELLED, CONFIRMED→PICKED_UP
     *
     * @param newStatus 변경할 상태
     * @throws CustomException 허용되지 않은 전환 시 ORDER_STATUS_CANNOT_CHANGE 예외
     */
    public void updateStatus(OrderStatus newStatus) {
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(this.status, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new CustomException(ErrorCode.ORDER_STATUS_CANNOT_CHANGE);
        }
        this.status = newStatus;
    }

    /**
     * 주문 번호를 설정합니다.
     *
     * @param orderNumber 주문 번호 (영숫자 4자리)
     */
    public void assignOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    /**
     * 주문 날짜를 설정합니다.
     *
     * @param orderDate 주문 날짜
     */
    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }
}
