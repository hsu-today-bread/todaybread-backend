package com.todaybread.server.domain.payment.service;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.order.service.OrderService;
import com.todaybread.server.domain.payment.dto.PaymentRequest;
import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import com.todaybread.server.domain.payment.processor.PaymentProcessor;
import com.todaybread.server.domain.payment.processor.PaymentResult;
import com.todaybread.server.domain.payment.repository.PaymentRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Payment 도메인 속성 기반 테스트 (jqwik)
 * Feature: order-flow
 */
class PaymentPropertyTest {

    private PaymentService paymentService;
    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private PaymentProcessor paymentProcessor;
    private OrderService orderService;

    @BeforeProperty
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        paymentRepository = Mockito.mock(PaymentRepository.class);
        paymentProcessor = Mockito.mock(PaymentProcessor.class);
        orderService = Mockito.mock(OrderService.class);
        paymentService = new PaymentService(orderRepository, paymentRepository, paymentProcessor, orderService);
    }

    // ── Helper methods ──

    private OrderEntity createOrder(Long orderId, Long userId, Long storeId, OrderStatus status, int totalAmount) {
        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .storeId(storeId)
                .status(status)
                .totalAmount(totalAmount)
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    // ── Property 18: 결제 금액 불일치 거부 ──
    // Feature: order-flow, Property 18: 결제 금액 불일치 거부
    // **Validates: Requirements 10.2**
    @Property(tries = 100)
    void paymentAmountMismatchIsRejected(
            @ForAll @IntRange(min = 1, max = 100_000) int totalAmount,
            @ForAll @IntRange(min = 1, max = 100_000) int paymentAmount
    ) {
        // Only test when amounts differ
        if (totalAmount == paymentAmount) {
            return;
        }

        Long orderId = 1L, userId = 1L, storeId = 100L;
        OrderEntity order = createOrder(orderId, userId, storeId, OrderStatus.PENDING, totalAmount);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        PaymentRequest request = new PaymentRequest(orderId, paymentAmount);

        assertThatThrownBy(() -> paymentService.processPayment(userId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH));

        // Verify Order status remains PENDING
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

        // Verify PaymentProcessor was never called
        verify(paymentProcessor, never()).pay(anyInt());
        // Verify no Payment was saved
        verify(paymentRepository, never()).save(any());
    }

    // ── Property 19: 결제 처리 후 데이터 정합성 ──
    // Feature: order-flow, Property 19: 결제 처리 후 데이터 정합성
    // **Validates: Requirements 10.1, 10.5, 10.10**
    @Property(tries = 100)
    void paymentDataIntegrityAfterProcessing(
            @ForAll @IntRange(min = 1, max = 100_000) int amount
    ) {
        // Reset mocks to avoid accumulated invocation counts across tries
        Mockito.reset(orderRepository, paymentRepository, paymentProcessor, orderService);

        Long orderId = 1L, userId = 1L, storeId = 100L;
        OrderEntity order = createOrder(orderId, userId, storeId, OrderStatus.PENDING, amount);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(paymentProcessor.pay(amount)).willReturn(new PaymentResult(PaymentStatus.APPROVED, "OK"));

        ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
        given(paymentRepository.save(paymentCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        PaymentRequest request = new PaymentRequest(orderId, amount);
        paymentService.processPayment(userId, request);

        // Verify saved Payment fields
        PaymentEntity savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getOrderId()).isEqualTo(orderId);
        assertThat(savedPayment.getAmount()).isEqualTo(amount);
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(savedPayment.getPaidAt()).isNotNull();

        // Verify OrderService.confirmOrder was called
        verify(orderService).confirmOrder(orderId);
    }
}
