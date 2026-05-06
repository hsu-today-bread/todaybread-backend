package com.todaybread.server.domain.payment.processor;

import com.todaybread.server.domain.payment.client.TossPaymentClient;
import com.todaybread.server.domain.payment.client.dto.TossCancelDetail;
import com.todaybread.server.domain.payment.client.dto.TossCancelResponse;
import com.todaybread.server.domain.payment.client.dto.TossConfirmResponse;
import com.todaybread.server.domain.payment.client.dto.TossPaymentResponse;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 토스 페이먼츠 API를 사용하는 결제 처리 구현체입니다.
 * {@link TossPaymentClient}를 통해 토스 결제 승인 및 취소 API를 호출합니다.
 *
 * <p>stub 프로필이 아닌 환경에서 활성화됩니다.</p>
 */
@Component
@Profile("!stub")
@RequiredArgsConstructor
public class TossPaymentProcessor implements PaymentProcessor {

    private final TossPaymentClient tossPaymentClient;

    /**
     * 토스 결제는 프론트엔드에서 결제 위젯을 통해 진행되므로,
     * 서버에서 직접 결제를 시작하는 {@code pay()} 메서드는 지원하지 않습니다.
     * 대신 {@link #confirm(String, String, int)}을 사용하세요.
     *
     * @throws UnsupportedOperationException 항상 발생
     */
    @Override
    public PaymentResult pay(Long orderId, int amount) {
        throw new UnsupportedOperationException(
                "토스 결제는 confirm()을 사용해주세요. 프론트엔드에서 결제 위젯을 통해 paymentKey를 발급받은 뒤 confirm()으로 승인합니다.");
    }

    /**
     * 토스 페이먼츠 결제 승인 API를 호출하여 결제를 확정합니다.
     *
     * <p>토스 API가 {@code status=DONE} 응답을 반환하면 {@link PaymentStatus#APPROVED} 상태의
     * {@link PaymentResult}를 반환합니다.</p>
     *
     * <p>토스 API가 에러 응답을 반환하면 {@link com.todaybread.server.domain.payment.client.TossPaymentException}이
     * 그대로 전파됩니다.</p>
     *
     * @param paymentKey     토스 페이먼츠 결제 고유 키
     * @param orderId        주문 ID (문자열)
     * @param amount         결제 금액
     * @param idempotencyKey 멱등성 키 (토스 Idempotency-Key 헤더로 전달)
     * @return 결제 승인 결과
     */
    @Override
    public PaymentResult confirm(String paymentKey, String orderId, int amount, String idempotencyKey) {
        TossConfirmResponse response = tossPaymentClient.confirmPayment(paymentKey, orderId, amount, idempotencyKey);

        return new PaymentResult(
                PaymentStatus.APPROVED,
                "결제가 승인되었습니다",
                response.paymentKey(),
                response.method(),
                response.approvedAt()
        );
    }

    /**
     * 토스 페이먼츠 결제 조회 API를 호출하여 결제 상태를 확인합니다.
     *
     * @param paymentKey 토스 페이먼츠 결제 고유 키
     * @return 결제 조회 응답
     */
    @Override
    public TossPaymentResponse getPayment(String paymentKey) {
        return tossPaymentClient.getPayment(paymentKey);
    }

    /**
     * 토스 페이먼츠 결제 취소 API를 호출하여 결제를 취소합니다.
     *
     * <p>토스 API가 성공 응답을 반환하면 취소 상세 정보를 포함한
     * {@link CancelResult}를 반환합니다.</p>
     *
     * <p>토스 API가 에러 응답을 반환하면 {@link com.todaybread.server.domain.payment.client.TossPaymentException}이
     * 그대로 전파됩니다.</p>
     *
     * @param paymentKey   토스 페이먼츠 결제 고유 키
     * @param cancelReason 취소 사유
     * @param cancelAmount 취소 금액
     * @return 결제 취소 결과
     */
    @Override
    public CancelResult cancel(String paymentKey, String cancelReason, int cancelAmount) {
        TossCancelResponse response = tossPaymentClient.cancelPayment(paymentKey, cancelReason, cancelAmount);

        String cancelledAt = null;
        if (response.cancels() != null && !response.cancels().isEmpty()) {
            TossCancelDetail latestCancel = response.cancels().get(response.cancels().size() - 1);
            cancelledAt = latestCancel.canceledAt();
        }

        return new CancelResult(
                response.paymentKey(),
                response.orderId(),
                response.status(),
                cancelledAt
        );
    }
}
