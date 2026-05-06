package com.todaybread.server.domain.payment.service;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.order.service.OrderService;
import com.todaybread.server.domain.payment.client.TossPaymentException;
import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import com.todaybread.server.domain.payment.processor.PaymentProcessor;
import com.todaybread.server.domain.payment.processor.PaymentResult;
import com.todaybread.server.domain.payment.repository.PaymentRepository;
import com.todaybread.server.support.TestFixtures;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

// Feature: toss-payment-integration, Property 2, 3, 6: PaymentService 상태 전이 및 멱등성

/**
 * PaymentService의 결제 승인 성공/실패 상태 전이 불변 조건 및 멱등성 속성 테스트.
 * jqwik + Mockito를 사용하여 임의의 유효한 입력에 대해 속성을 검증합니다.
 */
class PaymentServicePropertyTest {

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private PaymentProcessor paymentProcessor;
    private OrderService orderService;
    private PaymentCancelExecutor paymentCancelExecutor;
    private Clock clock;
    private PaymentService paymentService;

    private void setupMocks() {
        orderRepository = Mockito.mock(OrderRepository.class);
        paymentRepository = Mockito.mock(PaymentRepository.class);
        paymentProcessor = Mockito.mock(PaymentProcessor.class);
        orderService = Mockito.mock(OrderService.class);
        paymentCancelExecutor = Mockito.mock(PaymentCancelExecutor.class);
        clock = TestFixtures.FIXED_CLOCK;
        paymentService = new PaymentService(orderRepository, paymentRepository, paymentProcessor, orderService, paymentCancelExecutor, clock);
    }

    // ========================================================================
    // Property 2: 결제 승인 성공 시 상태 전이 불변 조건
    // ========================================================================

    /**
     * **Validates: Requirements 2.2, 2.4**
     *
     * 임의의 유효한 주문(PENDING 상태, 금액 일치)과 토스 DONE 응답에 대해,
     * confirmPayment 호출 후:
     * - Payment 상태는 반드시 APPROVED
     * - paymentKey가 저장됨
     * - 주문 상태는 CONFIRMED로 변경됨 (confirmOrder 호출)
     */
    @Property(tries = 100)
    void confirmPaymentSuccess_setsApprovedAndConfirmsOrder(
            @ForAll("validOrderIds") Long orderId,
            @ForAll("validUserIds") Long userId,
            @ForAll("validAmounts") int amount,
            @ForAll("validPaymentKeys") String paymentKey,
            @ForAll("validIdempotencyKeys") String idempotencyKey,
            @ForAll("validMethods") String method) {

        setupMocks();

        // Arrange: PENDING 주문, 금액 일치
        OrderEntity order = TestFixtures.order(orderId, userId, 100L, OrderStatus.PENDING, amount, "order-key");
        given(orderRepository.findByIdWithLock(orderId)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(orderId, idempotencyKey)).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
        given(paymentRepository.save(any(PaymentEntity.class))).willAnswer(inv -> inv.getArgument(0));

        // 토스 DONE 응답
        PaymentResult successResult = new PaymentResult(
                PaymentStatus.APPROVED, "ok", paymentKey, method, "2025-07-01T18:31:00+09:00");
        given(paymentProcessor.confirm(eq(paymentKey), eq("order_" + orderId), eq(amount), eq(idempotencyKey)))
                .willReturn(successResult);

        // Act
        PaymentEntity result = paymentService.confirmPayment(userId, paymentKey, orderId, amount, idempotencyKey);

        // Assert: Payment 상태 APPROVED
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.APPROVED);

        // Assert: paymentKey 저장됨
        assertThat(result.getPaymentKey()).isEqualTo(paymentKey);

        // Assert: 주문 CONFIRMED 전환 (confirmOrder 호출)
        verify(orderService).confirmOrder(orderId);
    }

    // ========================================================================
    // Property 3: 결제 승인 실패 시 상태 전이 불변 조건
    // ========================================================================

    /**
     * **Validates: Requirements 2.3**
     *
     * 임의의 유효한 주문(PENDING 상태, 금액 일치)과 토스 에러 응답에 대해,
     * confirmPayment 호출 후:
     * - Payment 상태는 반드시 FAILED
     * - 주문 상태는 PENDING 유지 (confirmOrder 호출되지 않음)
     */
    @Property(tries = 100)
    void confirmPaymentFailure_setsFailedAndKeepsOrderPending(
            @ForAll("validOrderIds") Long orderId,
            @ForAll("validUserIds") Long userId,
            @ForAll("validAmounts") int amount,
            @ForAll("validPaymentKeys") String paymentKey,
            @ForAll("validIdempotencyKeys") String idempotencyKey,
            @ForAll("tossErrorCodes") String errorCode,
            @ForAll("tossErrorMessages") String errorMessage) {

        setupMocks();

        // Arrange: PENDING 주문, 금액 일치
        OrderEntity order = TestFixtures.order(orderId, userId, 100L, OrderStatus.PENDING, amount, "order-key");
        given(orderRepository.findByIdWithLock(orderId)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(orderId, idempotencyKey)).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());
        given(paymentRepository.save(any(PaymentEntity.class))).willAnswer(inv -> inv.getArgument(0));

        // 토스 에러 응답 (카드 관련 에러 - TossPaymentException으로 전파)
        given(paymentProcessor.confirm(eq(paymentKey), eq("order_" + orderId), eq(amount), eq(idempotencyKey)))
                .willThrow(new TossPaymentException(errorCode, errorMessage, 400));

        // Act & Assert: TossPaymentException이 전파됨
        assertThatThrownBy(() ->
                paymentService.confirmPayment(userId, paymentKey, orderId, amount, idempotencyKey))
                .isInstanceOf(TossPaymentException.class);

        // Assert: Payment가 FAILED로 저장됨
        ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository).save(captor.capture());
        PaymentEntity savedPayment = captor.getValue();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);

        // Assert: 주문 PENDING 유지 (confirmOrder 호출되지 않음)
        verify(orderService, never()).confirmOrder(any());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    // ========================================================================
    // Property 6: 결제 멱등성
    // ========================================================================

    /**
     * **Validates: Requirements 8.3, 8.4**
     *
     * 임의의 유효한 결제 승인 요청에 대해, 동일 idempotencyKey로 두 번 호출 시
     * 두 번째 호출은 토스 API를 호출하지 않고 동일 결과를 반환합니다.
     */
    @Property(tries = 100)
    void confirmPaymentIdempotency_secondCallSkipsTossApi(
            @ForAll("validOrderIds") Long orderId,
            @ForAll("validUserIds") Long userId,
            @ForAll("validAmounts") int amount,
            @ForAll("validPaymentKeys") String paymentKey,
            @ForAll("validIdempotencyKeys") String idempotencyKey) {

        setupMocks();

        // Arrange: 기존 APPROVED 결제가 이미 존재 (첫 번째 호출 결과)
        PaymentEntity existingPayment = TestFixtures.payment(
                10L, orderId, amount, PaymentStatus.APPROVED,
                java.time.LocalDateTime.of(2026, 4, 5, 12, 0), idempotencyKey);
        // paymentKey도 설정
        existingPayment.approve(
                java.time.LocalDateTime.of(2026, 4, 5, 12, 0),
                idempotencyKey, paymentKey, "카드");

        given(paymentRepository.findByOrderIdAndIdempotencyKey(orderId, idempotencyKey))
                .willReturn(Optional.of(existingPayment));

        // C3: 소유자 검증을 위한 주문 조회 stub
        OrderEntity order = TestFixtures.order(orderId, userId, 100L, OrderStatus.CONFIRMED, amount, "order-key");
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // Act: 두 번째 호출 (동일 idempotencyKey)
        PaymentEntity result = paymentService.confirmPayment(userId, paymentKey, orderId, amount, idempotencyKey);

        // Assert: 토스 API 호출하지 않음
        verify(paymentProcessor, never()).confirm(any(), any(), anyInt(), any());

        // Assert: 기존 결과와 동일
        assertThat(result.getId()).isEqualTo(existingPayment.getId());
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.getPaymentKey()).isEqualTo(paymentKey);
        assertThat(result.getAmount()).isEqualTo(amount);
    }

    // ========================================================================
    // Arbitrary Providers
    // ========================================================================

    @Provide
    Arbitrary<Long> validOrderIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    @Provide
    Arbitrary<Long> validUserIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    @Provide
    Arbitrary<Integer> validAmounts() {
        return Arbitraries.integers().between(100, 1_000_000);
    }

    @Provide
    Arbitrary<String> validPaymentKeys() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(10).ofMaxLength(50)
                .map(s -> "tgen_" + s);
    }

    @Provide
    Arbitrary<String> validIdempotencyKeys() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(8).ofMaxLength(36)
                .map(s -> "idem_" + s);
    }

    @Provide
    Arbitrary<String> validMethods() {
        return Arbitraries.of("카드", "간편결제", "계좌이체", "가상계좌", "휴대폰");
    }

    @Provide
    Arbitrary<String> tossErrorCodes() {
        return Arbitraries.of(
                "INVALID_CARD_COMPANY",
                "INVALID_STOPPED_CARD",
                "EXCEED_MAX_CARD_INSTALLMENT_PLAN",
                "NOT_ALLOWED_POINT_USE",
                "INVALID_CARD_EXPIRATION",
                "REJECT_CARD_PAYMENT"
        );
    }

    @Provide
    Arbitrary<String> tossErrorMessages() {
        return Arbitraries.of(
                "유효하지 않은 카드사입니다.",
                "정지된 카드입니다.",
                "할부 개월 수가 초과되었습니다.",
                "포인트 사용이 불가합니다.",
                "카드 유효기간이 만료되었습니다.",
                "카드 결제가 거절되었습니다."
        );
    }
}
