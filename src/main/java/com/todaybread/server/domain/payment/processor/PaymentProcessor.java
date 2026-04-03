package com.todaybread.server.domain.payment.processor;

/**
 * 결제 처리 인터페이스입니다.
 * 토스 결제 연동 시 구현체를 교체할 수 있도록 인터페이스로 분리합니다.
 */
public interface PaymentProcessor {

    /**
     * 결제를 처리합니다.
     *
     * @param amount 결제 금액
     * @return 결제 처리 결과
     */
    PaymentResult pay(int amount);
}
