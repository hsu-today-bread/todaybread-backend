package com.todaybread.server.domain.payment.entity;

/**
 * 결제 상태를 나타내는 열거형입니다.
 */
public enum PaymentStatus {
    PENDING,
    APPROVED,
    FAILED,
    CANCELLED
}
