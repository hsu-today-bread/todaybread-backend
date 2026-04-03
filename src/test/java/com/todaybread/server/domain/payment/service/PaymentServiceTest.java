package com.todaybread.server.domain.payment.service;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.order.service.OrderService;
import com.todaybread.server.domain.payment.dto.PaymentRequest;
import com.todaybread.server.domain.payment.dto.PaymentResponse;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import com.todaybread.server.domain.payment.processor.PaymentProcessor;
import com.todaybread.server.domain.payment.processor.PaymentResult;
import com.todaybread.server.domain.payment.processor.StubPaymentProcessor;
import com.todaybread.server.domain.payment.repository.PaymentRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProcessor paymentProcessor;

    @Mock
    private OrderService orderService;

    // ── 헬퍼 메서드 ──

    private OrderEntity createOrderEntity(Long orderId, Long userId, Long storeId, OrderStatus status, int totalAmount) {
        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .storeId(storeId)
                .status(status)
                .totalAmount(totalAmount)
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    // ── 테스트 ──

    @Nested
    @DisplayName("StubPaymentProcessor 동작 검증")
    class StubPaymentProcessorTest {

        @Test
        @DisplayName("양수 금액이면 APPROVED 상태를 반환한다")
        void positiveAmountReturnsApproved() {
            StubPaymentProcessor stub = new StubPaymentProcessor();

            PaymentResult result = stub.pay(10000);

            assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(result.message()).isNotBlank();
        }

        @Test
        @DisplayName("0 이하 금액이면 FAILED 상태를 반환한다")
        void zeroOrNegativeAmountReturnsFailed() {
            StubPaymentProcessor stub = new StubPaymentProcessor();

            PaymentResult zeroResult = stub.pay(0);
            PaymentResult negativeResult = stub.pay(-1000);

            assertThat(zeroResult.status()).isEqualTo(PaymentStatus.FAILED);
            assertThat(negativeResult.status()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("processPayment — 금액 불일치 거부")
    class ProcessPayment_AmountMismatch {

        @Test
        @DisplayName("결제 금액이 주문 totalAmount와 다르면 PAYMENT_AMOUNT_MISMATCH 예외를 던진다")
        void mismatchedAmountThrowsPaymentAmountMismatch() {
            Long userId = 1L;
            Long orderId = 100L;
            OrderEntity order = createOrderEntity(orderId, userId, 10L, OrderStatus.PENDING, 7000);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            PaymentRequest request = new PaymentRequest(orderId, 5000); // 7000 != 5000

            assertThatThrownBy(() -> paymentService.processPayment(userId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH));

            verify(paymentProcessor, never()).pay(any(Integer.class));
            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("processPayment — 0 이하 금액 거부")
    class ProcessPayment_NonPositiveAmount {

        @Test
        @DisplayName("결제 금액이 0이면 PAYMENT_AMOUNT_MUST_BE_POSITIVE 예외를 던진다")
        void zeroAmountThrowsPaymentAmountMustBePositive() {
            Long userId = 1L;
            Long orderId = 100L;
            OrderEntity order = createOrderEntity(orderId, userId, 10L, OrderStatus.PENDING, 0);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            PaymentRequest request = new PaymentRequest(orderId, 0);

            assertThatThrownBy(() -> paymentService.processPayment(userId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MUST_BE_POSITIVE));

            verify(paymentProcessor, never()).pay(any(Integer.class));
        }

        @Test
        @DisplayName("결제 금액이 음수이면 PAYMENT_AMOUNT_MUST_BE_POSITIVE 예외를 던진다")
        void negativeAmountThrowsPaymentAmountMustBePositive() {
            Long userId = 1L;
            Long orderId = 100L;
            OrderEntity order = createOrderEntity(orderId, userId, 10L, OrderStatus.PENDING, -1000);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            PaymentRequest request = new PaymentRequest(orderId, -1000);

            assertThatThrownBy(() -> paymentService.processPayment(userId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MUST_BE_POSITIVE));

            verify(paymentProcessor, never()).pay(any(Integer.class));
        }
    }

    @Nested
    @DisplayName("processPayment — 다른 유저 Order 결제 거부")
    class ProcessPayment_AccessDenied {

        @Test
        @DisplayName("다른 유저의 Order에 결제하면 ORDER_ACCESS_DENIED 예외를 던진다")
        void otherUserOrderThrowsAccessDenied() {
            Long ownerUserId = 1L;
            Long otherUserId = 2L;
            Long orderId = 100L;
            OrderEntity order = createOrderEntity(orderId, ownerUserId, 10L, OrderStatus.PENDING, 7000);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            PaymentRequest request = new PaymentRequest(orderId, 7000);

            assertThatThrownBy(() -> paymentService.processPayment(otherUserId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED));

            verify(paymentProcessor, never()).pay(any(Integer.class));
            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("processPayment — PENDING이 아닌 Order 결제 거부")
    class ProcessPayment_InvalidOrderStatus {

        @Test
        @DisplayName("CONFIRMED 상태의 Order에 결제하면 PAYMENT_ORDER_STATUS_INVALID 예외를 던진다")
        void confirmedOrderThrowsPaymentOrderStatusInvalid() {
            Long userId = 1L;
            Long orderId = 100L;
            OrderEntity order = createOrderEntity(orderId, userId, 10L, OrderStatus.CONFIRMED, 7000);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            PaymentRequest request = new PaymentRequest(orderId, 7000);

            assertThatThrownBy(() -> paymentService.processPayment(userId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PAYMENT_ORDER_STATUS_INVALID));

            verify(paymentProcessor, never()).pay(any(Integer.class));
        }

        @Test
        @DisplayName("CANCELLED 상태의 Order에 결제하면 PAYMENT_ORDER_STATUS_INVALID 예외를 던진다")
        void cancelledOrderThrowsPaymentOrderStatusInvalid() {
            Long userId = 1L;
            Long orderId = 100L;
            OrderEntity order = createOrderEntity(orderId, userId, 10L, OrderStatus.CANCELLED, 7000);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            PaymentRequest request = new PaymentRequest(orderId, 7000);

            assertThatThrownBy(() -> paymentService.processPayment(userId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PAYMENT_ORDER_STATUS_INVALID));

            verify(paymentProcessor, never()).pay(any(Integer.class));
        }
    }
}
