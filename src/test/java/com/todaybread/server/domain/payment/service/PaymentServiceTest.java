package com.todaybread.server.domain.payment.service;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.order.service.InventoryRestorer;
import com.todaybread.server.domain.order.service.OrderService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PaymentService 단위 테스트입니다.
 * processPayment()는 I2(Deprecated 엔드포인트 제거)에 의해 삭제되었으므로,
 * confirmPayment() 기반 테스트만 유지합니다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProcessor paymentProcessor;

    @Mock
    private OrderService orderService;

    @Mock
    private InventoryRestorer inventoryRestorer;

    @Mock
    private Clock clock;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void confirmPayment_returnsExistingPaymentForSameOrderIdAndIdempotencyKey() {
        PaymentEntity payment = TestFixtures.payment(10L, 1L, 3_000, PaymentStatus.APPROVED,
                LocalDateTime.of(2026, 4, 5, 12, 0), "pay-key");
        given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "pay-key")).willReturn(Optional.of(payment));
        // C3: 소유자 검증을 위한 주문 조회 stub
        OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.CONFIRMED, 3_000, "order-key");
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        PaymentEntity result = paymentService.confirmPayment(1L, "tgen_abc", 1L, 3_000, "pay-key");

        assertThat(result.getId()).isEqualTo(10L);
        verify(paymentProcessor, never()).confirm(any(), any(), any(Integer.class), any());
    }

    @Test
    void confirmPayment_rejectsMissingOrder() {
        given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "pay-key")).willReturn(Optional.empty());
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, "tgen_abc", 1L, 3_000, "pay-key"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void confirmPayment_rejectsAmountMismatch() {
        OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 3_000, "order-key");
        given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "pay-key")).willReturn(Optional.empty());
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, "tgen_abc", 1L, 2_000, "pay-key"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    @Test
    void confirmPayment_rejectsNonPendingOrder() {
        OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.CONFIRMED, 3_000, "order-key");
        given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "pay-key")).willReturn(Optional.empty());
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, "tgen_abc", 1L, 3_000, "pay-key"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_ORDER_STATUS_INVALID);
    }

    @Test
    void confirmPayment_approvesNewPaymentAndConfirmsOrder() {
        OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 3_000, "order-key");
        given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "pay-key")).willReturn(Optional.empty());
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrderId(1L)).willReturn(Optional.empty());
        given(paymentProcessor.confirm(eq("tgen_abc"), eq("order_1"), eq(3_000), eq("pay-key")))
                .willReturn(new PaymentResult(PaymentStatus.APPROVED, "ok", "tgen_abc", "카드", "2025-07-01T18:31:00+09:00"));
        given(clock.instant()).willReturn(TestFixtures.FIXED_CLOCK.instant());
        given(clock.getZone()).willReturn(TestFixtures.FIXED_CLOCK.getZone());
        given(paymentRepository.save(any(PaymentEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        PaymentEntity result = paymentService.confirmPayment(1L, "tgen_abc", 1L, 3_000, "pay-key");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.getPaidAt()).isNotNull();
        verify(orderService).confirmOrder(1L);
    }
}
