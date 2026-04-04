package com.todaybread.server.domain.payment.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제 정보를 나타내는 엔티티입니다.
 * 주문에 대한 결제 금액, 상태, 결제 시각을 관리합니다.
 */
@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Builder
    private PaymentEntity(Long orderId, int amount, PaymentStatus status, LocalDateTime paidAt,
                          String idempotencyKey) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.paidAt = paidAt;
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * 결제를 승인 처리합니다.
     * 결제 상태를 APPROVED로 변경하고 결제 시각을 설정합니다.
     *
     * @param paidAt 결제 처리 시각
     */
    public void approve(LocalDateTime paidAt, String idempotencyKey) {
        this.status = PaymentStatus.APPROVED;
        this.paidAt = paidAt;
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * 결제 상태를 변경합니다.
     *
     * @param newStatus 변경할 결제 상태
     */
    public void updateStatus(PaymentStatus newStatus, String idempotencyKey) {
        this.status = newStatus;
        this.paidAt = null;
        this.idempotencyKey = idempotencyKey;
    }
}
