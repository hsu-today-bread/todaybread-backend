package com.todaybread.server.domain.payment.service;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.order.service.OrderService;
import com.todaybread.server.domain.payment.dto.PaymentRequest;
import com.todaybread.server.domain.payment.dto.PaymentResponse;
import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import com.todaybread.server.domain.payment.processor.PaymentProcessor;
import com.todaybread.server.domain.payment.processor.PaymentResult;
import com.todaybread.server.domain.payment.repository.PaymentRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProcessor paymentProcessor;

    @Mock
    private OrderService orderService;

    @Mock
    private Clock clock;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPayment_returnsExistingPaymentForSameIdempotencyKey() {
        OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 3_000, "order-key");
        PaymentEntity payment = TestFixtures.payment(10L, 1L, 3_000, PaymentStatus.APPROVED,
                LocalDateTime.of(2026, 4, 5, 12, 0), "pay-key");
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "pay-key")).willReturn(Optional.of(payment));

        PaymentResponse response = paymentService.processPayment(1L, new PaymentRequest(1L, 3_000), "pay-key");

        assertThat(response.paymentId()).isEqualTo(10L);
        verify(paymentProcessor, never()).pay(any(), any(Integer.class));
    }

    @Test
    void processPayment_rejectsMissingOrder() {
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(1L, new PaymentRequest(1L, 3_000), "pay-key"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void processPayment_rejectsAmountMismatch() {
        OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 3_000, "order-key");
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "pay-key")).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(1L, new PaymentRequest(1L, 2_000), "pay-key"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    @Test
    void processPayment_rejectsNonPendingOrder() {
        OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.CONFIRMED, 3_000, "order-key");
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "pay-key")).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(1L, new PaymentRequest(1L, 3_000), "pay-key"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_ORDER_STATUS_INVALID);
    }

    @Test
    void processPayment_approvesNewPaymentAndConfirmsOrder() {
        OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 3_000, "order-key");
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "pay-key")).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(1L)).willReturn(Optional.empty());
        given(paymentProcessor.pay(1L, 3_000)).willReturn(new PaymentResult(PaymentStatus.APPROVED, "ok"));
        given(clock.instant()).willReturn(TestFixtures.FIXED_CLOCK.instant());
        given(clock.getZone()).willReturn(TestFixtures.FIXED_CLOCK.getZone());
        given(paymentRepository.save(any(PaymentEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(1L, new PaymentRequest(1L, 3_000), "pay-key");

        assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.paidAt()).isNotNull();
        verify(orderService).confirmOrder(1L);
    }

    @Test
    void processPayment_updatesExistingFailedPaymentWhenRetryFailsAgain() {
        OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 3_000, "order-key");
        PaymentEntity existingPayment = TestFixtures.payment(10L, 1L, 3_000, PaymentStatus.FAILED, null, "old-key");
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "new-key")).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(1L)).willReturn(Optional.of(existingPayment));
        given(paymentProcessor.pay(1L, 3_000)).willReturn(new PaymentResult(PaymentStatus.FAILED, "declined"));

        PaymentResponse response = paymentService.processPayment(1L, new PaymentRequest(1L, 3_000), "new-key");

        assertThat(response.paymentId()).isEqualTo(10L);
        assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(existingPayment.getIdempotencyKey()).isEqualTo("new-key");
        verify(orderService, never()).confirmOrder(any());
    }
}
