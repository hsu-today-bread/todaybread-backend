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

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "method", length = 50)
    private String method;

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

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
     * 결제 상태를 APPROVED로 변경하고 결제 시각, paymentKey, method를 설정합니다.
     *
     * @param paidAt         결제 처리 시각
     * @param idempotencyKey 멱등성 키
     * @param paymentKey     토스 페이먼츠 결제 고유 키
     * @param method         결제 수단 (카드, 간편결제 등)
     */
    public void approve(LocalDateTime paidAt, String idempotencyKey, String paymentKey, String method) {
        this.status = PaymentStatus.APPROVED;
        this.paidAt = paidAt;
        this.idempotencyKey = idempotencyKey;
        this.paymentKey = paymentKey;
        this.method = method;
    }

    /**
     * 결제를 승인 처리합니다. (하위 호환용)
     * paymentKey와 method 없이 승인 처리합니다.
     *
     * @param paidAt         결제 처리 시각
     * @param idempotencyKey 멱등성 키
     */
    public void approve(LocalDateTime paidAt, String idempotencyKey) {
        approve(paidAt, idempotencyKey, null, null);
    }

    /**
     * 결제를 취소 처리합니다.
     * 결제 상태를 CANCELLED로 변경하고 취소 사유와 취소 시각을 설정합니다.
     *
     * @param cancelReason 취소 사유
     * @param cancelledAt  취소 시각
     */
    public void cancel(String cancelReason, LocalDateTime cancelledAt) {
        this.status = PaymentStatus.CANCELLED;
        this.cancelReason = cancelReason;
        this.cancelledAt = cancelledAt;
    }

    /**
     * 결제 상태를 변경합니다.
     *
     * @param newStatus      변경할 결제 상태
     * @param idempotencyKey 멱등성 키
     */
    public void updateStatus(PaymentStatus newStatus, String idempotencyKey) {
        this.status = newStatus;
        this.paidAt = null;
        this.idempotencyKey = idempotencyKey;
    }
}
