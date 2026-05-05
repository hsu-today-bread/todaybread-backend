package com.todaybread.server.domain.payment.processor;

import com.todaybread.server.domain.payment.client.dto.TossPaymentResponse;

/**
 * 결제 처리 인터페이스입니다.
 * 토스 결제 연동 시 구현체를 교체할 수 있도록 인터페이스로 분리합니다.
 */
public interface PaymentProcessor {

    /**
     * 결제를 처리합니다.
     *
     * @param orderId 주문 ID (외부 PG의 idempotency key로 활용)
     * @param amount  결제 금액
     * @return 결제 처리 결과
     */
    PaymentResult pay(Long orderId, int amount);

    /**
     * 토스 결제 승인을 처리합니다.
     * 기본 구현은 {@link #pay(Long, int)}에 위임합니다.
     *
     * @param paymentKey     토스 페이먼츠 결제 고유 키
     * @param orderId        주문 ID (문자열)
     * @param amount         결제 금액
     * @param idempotencyKey 멱등성 키 (토스 Idempotency-Key 헤더로 전달)
     * @return 결제 처리 결과
     */
    default PaymentResult confirm(String paymentKey, String orderId, int amount, String idempotencyKey) {
        return pay(Long.parseLong(orderId), amount);
    }

    /**
     * 결제를 취소합니다.
     * 기본 구현은 {@link UnsupportedOperationException}을 발생시킵니다.
     *
     * @param paymentKey   토스 페이먼츠 결제 고유 키
     * @param cancelReason 취소 사유
     * @param cancelAmount 취소 금액
     * @return 결제 취소 결과
     * @throws UnsupportedOperationException 취소를 지원하지 않는 프로세서인 경우
     */
    default CancelResult cancel(String paymentKey, String cancelReason, int cancelAmount) {
        throw new UnsupportedOperationException("결제 취소를 지원하지 않는 프로세서입니다.");
    }

    /**
     * 토스 결제 조회 API를 호출하여 결제 상태를 확인합니다.
     * 기본 구현은 {@link UnsupportedOperationException}을 발생시킵니다.
     *
     * @param paymentKey 토스 페이먼츠 결제 고유 키
     * @return 결제 조회 응답
     * @throws UnsupportedOperationException 조회를 지원하지 않는 프로세서인 경우
     */
    default TossPaymentResponse getPayment(String paymentKey) {
        throw new UnsupportedOperationException("결제 조회를 지원하지 않는 프로세서입니다.");
    }
}
