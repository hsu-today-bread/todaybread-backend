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

    @Builder
    private PaymentEntity(Long orderId, int amount, PaymentStatus status, LocalDateTime paidAt) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.paidAt = paidAt;
    }

    /**
     * 결제 상태를 변경합니다.
     * APPROVED 상태로 변경 시 결제 시각을 현재 시각으로 설정합니다.
     *
     * @param newStatus 변경할 결제 상태
     */
    public void updateStatus(PaymentStatus newStatus) {
        this.status = newStatus;
        if (newStatus == PaymentStatus.APPROVED) {
            this.paidAt = LocalDateTime.now();
        }
    }
}
