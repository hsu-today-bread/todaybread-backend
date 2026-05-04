package com.todaybread.server.domain.payment.processor;

import com.todaybread.server.domain.payment.entity.PaymentStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * 임시 결제 처리 구현체입니다.
 * 토스 결제 연동 전까지 사용하며, 실제 PG 호출 없이 금액 검증만 수행합니다.
 * stub 프로필에서만 활성화됩니다.
 */
@Component
@Profile("stub")
public class StubPaymentProcessor implements PaymentProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentResult pay(Long orderId, int amount) {
        if (amount <= 0) {
            return new PaymentResult(PaymentStatus.FAILED, "결제 금액은 0보다 커야 합니다");
        }
        return new PaymentResult(PaymentStatus.APPROVED, "결제가 승인되었습니다");
    }

    @Override
    public CancelResult cancel(String paymentKey, String cancelReason, int cancelAmount) {
        return new CancelResult(
                paymentKey,
                null,
                PaymentStatus.CANCELLED.name(),
                OffsetDateTime.now().toString()
        );
    }
}
