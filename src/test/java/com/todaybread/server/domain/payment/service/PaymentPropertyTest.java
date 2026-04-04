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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Payment 도메인 속성 기반 테스트 (jqwik)
 * Feature: order-flow
 */
class PaymentPropertyTest {

    private static final String IDEMPOTENCY_KEY = "payment-key";

    private PaymentService paymentService;
    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private PaymentProcessor paymentProcessor;
    private OrderService orderService;
    private Clock clock;

    @BeforeProperty
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        paymentRepository = Mockito.mock(PaymentRepository.class);
        paymentProcessor = Mockito.mock(PaymentProcessor.class);
        orderService = Mockito.mock(OrderService.class);
        clock = Clock.fixed(Instant.parse("2026-04-04T10:00:00Z"), ZoneId.of("Asia/Seoul"));
        paymentService = new PaymentService(orderRepository, paymentRepository, paymentProcessor, orderService, clock);
    }

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

    // Feature: order-flow, Property 18: 결제 금액 불일치 거부
    @Property(tries = 100)
    void paymentAmountMismatchIsRejected(
            @ForAll @IntRange(min = 1, max = 100_000) int totalAmount,
            @ForAll @IntRange(min = 1, max = 100_000) int paymentAmount
    ) {
        if (totalAmount == paymentAmount) {
            return;
        }

        Long orderId = 1L, userId = 1L, storeId = 100L;
        OrderEntity order = createOrder(orderId, userId, storeId, OrderStatus.PENDING, totalAmount);

        given(orderRepository.findByIdWithLock(orderId)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(orderId, IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());

        PaymentRequest request = new PaymentRequest(orderId, paymentAmount);

        assertThatThrownBy(() -> paymentService.processPayment(userId, request, IDEMPOTENCY_KEY))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(paymentProcessor, never()).pay(anyLong(), anyInt());
        verify(paymentRepository, never()).save(any());
    }

    // Feature: order-flow, Property 19: 결제 처리 후 데이터 정합성
    @Property(tries = 100)
    void paymentDataIntegrityAfterProcessing(
            @ForAll @IntRange(min = 1, max = 100_000) int amount
    ) {
        Mockito.reset(orderRepository, paymentRepository, paymentProcessor, orderService);

        Long orderId = 1L, userId = 1L, storeId = 100L;
        OrderEntity order = createOrder(orderId, userId, storeId, OrderStatus.PENDING, amount);

        given(orderRepository.findByIdWithLock(orderId)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(orderId, IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
        given(paymentProcessor.pay(orderId, amount)).willReturn(new PaymentResult(PaymentStatus.APPROVED, "OK"));

        ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
        given(paymentRepository.save(paymentCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        PaymentRequest request = new PaymentRequest(orderId, amount);
        paymentService.processPayment(userId, request, IDEMPOTENCY_KEY);

        PaymentEntity savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getOrderId()).isEqualTo(orderId);
        assertThat(savedPayment.getAmount()).isEqualTo(amount);
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(savedPayment.getPaidAt()).isNotNull();

        verify(orderService).confirmOrder(orderId);
    }
}
