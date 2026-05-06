package com.todaybread.server.domain.payment.processor;

import com.todaybread.server.domain.payment.client.TossPaymentClient;
import com.todaybread.server.domain.payment.client.TossPaymentException;
import com.todaybread.server.domain.payment.client.dto.TossCancelDetail;
import com.todaybread.server.domain.payment.client.dto.TossCancelResponse;
import com.todaybread.server.domain.payment.client.dto.TossConfirmResponse;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TossPaymentProcessorTest {

    @Mock
    private TossPaymentClient tossPaymentClient;

    @InjectMocks
    private TossPaymentProcessor tossPaymentProcessor;

    @Test
    void confirm_성공시_APPROVED_상태와_결제정보를_반환한다() {
        // given
        String paymentKey = "tgen_20250101010101ABCDE";
        String orderId = "42";
        int amount = 7_500;

        TossConfirmResponse tossResponse = new TossConfirmResponse(
                paymentKey, orderId, "DONE", amount, "카드", "2025-07-01T18:31:00+09:00"
        );
        given(tossPaymentClient.confirmPayment(paymentKey, orderId, amount, "idem-key")).willReturn(tossResponse);

        // when
        PaymentResult result = tossPaymentProcessor.confirm(paymentKey, orderId, amount, "idem-key");

        // then
        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.paymentKey()).isEqualTo(paymentKey);
        assertThat(result.method()).isEqualTo("카드");
        assertThat(result.approvedAt()).isEqualTo("2025-07-01T18:31:00+09:00");
        assertThat(result.message()).isEqualTo("결제가 승인되었습니다");
    }

    @Test
    void confirm_토스API_에러시_TossPaymentException이_전파된다() {
        // given
        String paymentKey = "tgen_20250101010101ABCDE";
        String orderId = "42";
        int amount = 7_500;

        given(tossPaymentClient.confirmPayment(paymentKey, orderId, amount, "idem-key"))
                .willThrow(new TossPaymentException("INVALID_CARD_COMPANY", "유효하지 않은 카드사입니다.", 400));

        // when & then
        assertThatThrownBy(() -> tossPaymentProcessor.confirm(paymentKey, orderId, amount, "idem-key"))
                .isInstanceOf(TossPaymentException.class)
                .satisfies(ex -> {
                    TossPaymentException tpe = (TossPaymentException) ex;
                    assertThat(tpe.getErrorCode()).isEqualTo("INVALID_CARD_COMPANY");
                    assertThat(tpe.getErrorMessage()).isEqualTo("유효하지 않은 카드사입니다.");
                    assertThat(tpe.getHttpStatus()).isEqualTo(400);
                });
    }

    @Test
    void cancel_성공시_취소정보를_반환한다() {
        // given
        String paymentKey = "tgen_20250101010101ABCDE";
        String cancelReason = "고객 요청";
        int cancelAmount = 7_500;

        TossCancelDetail cancelDetail = new TossCancelDetail(cancelAmount, cancelReason, "2025-07-01T19:00:00+09:00");
        TossCancelResponse tossResponse = new TossCancelResponse(
                paymentKey, "42", "CANCELED", List.of(cancelDetail)
        );
        given(tossPaymentClient.cancelPayment(paymentKey, cancelReason, cancelAmount)).willReturn(tossResponse);

        // when
        CancelResult result = tossPaymentProcessor.cancel(paymentKey, cancelReason, cancelAmount);

        // then
        assertThat(result.paymentKey()).isEqualTo(paymentKey);
        assertThat(result.orderId()).isEqualTo("42");
        assertThat(result.status()).isEqualTo("CANCELED");
        assertThat(result.cancelledAt()).isEqualTo("2025-07-01T19:00:00+09:00");
    }

    @Test
    void cancel_토스API_에러시_TossPaymentException이_전파된다() {
        // given
        String paymentKey = "tgen_20250101010101ABCDE";
        String cancelReason = "고객 요청";
        int cancelAmount = 7_500;

        given(tossPaymentClient.cancelPayment(paymentKey, cancelReason, cancelAmount))
                .willThrow(new TossPaymentException("PROVIDER_ERROR", "결제 처리 중 오류가 발생했습니다.", 502));

        // when & then
        assertThatThrownBy(() -> tossPaymentProcessor.cancel(paymentKey, cancelReason, cancelAmount))
                .isInstanceOf(TossPaymentException.class)
                .satisfies(ex -> {
                    TossPaymentException tpe = (TossPaymentException) ex;
                    assertThat(tpe.getErrorCode()).isEqualTo("PROVIDER_ERROR");
                    assertThat(tpe.getErrorMessage()).isEqualTo("결제 처리 중 오류가 발생했습니다.");
                    assertThat(tpe.getHttpStatus()).isEqualTo(502);
                });
    }

    @Test
    void pay_호출시_UnsupportedOperationException이_발생한다() {
        // when & then
        assertThatThrownBy(() -> tossPaymentProcessor.pay(1L, 7_500))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
