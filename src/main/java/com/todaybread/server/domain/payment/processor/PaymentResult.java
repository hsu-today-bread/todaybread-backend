package com.todaybread.server.domain.payment.processor;

import com.todaybread.server.domain.payment.entity.PaymentStatus;

/**
 * 결제 처리 결과를 나타내는 레코드입니다.
 *
 * @param status     결제 상태
 * @param message    결제 결과 메시지
 * @param paymentKey 토스 페이먼츠 결제 고유 키 (nullable)
 * @param method     결제 수단 - 카드, 간편결제 등 (nullable)
 * @param approvedAt 결제 승인 시각 ISO 8601 (nullable)
 */
public record PaymentResult(
        PaymentStatus status,
        String message,
        String paymentKey,
        String method,
        String approvedAt
) {

    /**
     * 기존 호환용 생성자입니다.
     * paymentKey, method, approvedAt을 null로 설정합니다.
     *
     * @param status  결제 상태
     * @param message 결제 결과 메시지
     */
    public PaymentResult(PaymentStatus status, String message) {
        this(status, message, null, null, null);
    }
}
