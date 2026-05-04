package com.todaybread.server.domain.payment.service;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.order.service.OrderService;
import com.todaybread.server.domain.payment.client.TossPaymentException;
import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import com.todaybread.server.domain.payment.processor.CancelResult;
import com.todaybread.server.domain.payment.processor.PaymentProcessor;
import com.todaybread.server.domain.payment.processor.PaymentResult;
import com.todaybread.server.domain.payment.repository.PaymentRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PaymentService 단위 테스트 (confirmPayment, cancelPayment)
 *
 * Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 8.3
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceConfirmTest {

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

    // ========================================================================
    // confirmPayment 테스트
    // ========================================================================

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPayment {

        @Test
        @DisplayName("성공: 토스 승인 성공 시 Payment=APPROVED, paymentKey 저장, 주문 CONFIRMED")
        void success() {
            // Arrange
            OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 5_000, "order-key");
            given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
            given(paymentRepository.findByIdempotencyKey("idem-1")).willReturn(Optional.empty());
            given(paymentRepository.findByOrderId(1L)).willReturn(Optional.empty());
            given(paymentRepository.save(any(PaymentEntity.class))).willAnswer(inv -> inv.getArgument(0));
            given(clock.instant()).willReturn(TestFixtures.FIXED_CLOCK.instant());
            given(clock.getZone()).willReturn(TestFixtures.FIXED_CLOCK.getZone());

            PaymentResult result = new PaymentResult(
                    PaymentStatus.APPROVED, "ok", "tgen_abc123", "카드", "2025-07-01T18:31:00+09:00");
            given(paymentProcessor.confirm("tgen_abc123", "order_1", 5_000)).willReturn(result);

            // Act
            PaymentEntity payment = paymentService.confirmPayment(1L, "tgen_abc123", 1L, 5_000, "idem-1");

            // Assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.getPaymentKey()).isEqualTo("tgen_abc123");
            assertThat(payment.getMethod()).isEqualTo("카드");
            verify(orderService).confirmOrder(1L);
        }

        @Test
        @DisplayName("실패: 토스 에러 응답 시 Payment=FAILED, 주문 PENDING 유지")
        void failure_tossError() {
            // Arrange
            OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 5_000, "order-key");
            given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
            given(paymentRepository.findByIdempotencyKey("idem-1")).willReturn(Optional.empty());
            given(paymentRepository.findByOrderId(1L)).willReturn(Optional.empty());
            given(paymentRepository.save(any(PaymentEntity.class))).willAnswer(inv -> inv.getArgument(0));

            given(paymentProcessor.confirm("tgen_abc123", "order_1", 5_000))
                    .willThrow(new TossPaymentException("REJECT_CARD_PAYMENT", "카드 결제가 거절되었습니다.", 400));

            // Act & Assert
            assertThatThrownBy(() ->
                    paymentService.confirmPayment(1L, "tgen_abc123", 1L, 5_000, "idem-1"))
                    .isInstanceOf(TossPaymentException.class);

            verify(orderService, never()).confirmOrder(any());
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("멱등성: 동일 idempotencyKey로 기존 APPROVED 결제가 있으면 기존 결과 반환")
        void idempotency_returnsExistingApproved() {
            // Arrange
            PaymentEntity existing = TestFixtures.payment(10L, 1L, 5_000, PaymentStatus.APPROVED,
                    LocalDateTime.of(2026, 4, 5, 12, 0), "idem-1");
            given(paymentRepository.findByIdempotencyKey("idem-1")).willReturn(Optional.of(existing));

            // Act
            PaymentEntity payment = paymentService.confirmPayment(1L, "tgen_abc123", 1L, 5_000, "idem-1");

            // Assert
            assertThat(payment.getId()).isEqualTo(10L);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            verify(paymentProcessor, never()).confirm(any(), any(), anyInt());
        }

        @Test
        @DisplayName("금액 불일치: PAYMENT_AMOUNT_MISMATCH 에러")
        void amountMismatch() {
            // Arrange
            OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 5_000, "order-key");
            given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
            given(paymentRepository.findByIdempotencyKey("idem-1")).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    paymentService.confirmPayment(1L, "tgen_abc123", 1L, 3_000, "idem-1"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        @Test
        @DisplayName("주문 미존재: ORDER_NOT_FOUND 에러")
        void orderNotFound() {
            // Arrange
            given(paymentRepository.findByIdempotencyKey("idem-1")).willReturn(Optional.empty());
            given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    paymentService.confirmPayment(1L, "tgen_abc123", 1L, 5_000, "idem-1"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("주문 상태 오류: PENDING이 아닌 주문에 대해 PAYMENT_ORDER_STATUS_INVALID 에러")
        void orderStatusInvalid() {
            // Arrange
            OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.CONFIRMED, 5_000, "order-key");
            given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
            given(paymentRepository.findByIdempotencyKey("idem-1")).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    paymentService.confirmPayment(1L, "tgen_abc123", 1L, 5_000, "idem-1"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PAYMENT_ORDER_STATUS_INVALID);
        }
    }

    // ========================================================================
    // cancelPayment 테스트
    // ========================================================================

    @Nested
    @DisplayName("cancelPayment")
    class CancelPayment {

        @Test
        @DisplayName("성공: 토스 취소 성공 시 Payment=CANCELLED, 주문 취소 위임")
        void success() {
            // Arrange
            PaymentEntity payment = TestFixtures.payment(10L, 1L, 5_000, PaymentStatus.APPROVED,
                    LocalDateTime.of(2026, 4, 5, 12, 0), "idem-1");
            // paymentKey 설정
            payment.approve(LocalDateTime.of(2026, 4, 5, 12, 0), "idem-1", "tgen_abc123", "카드");

            OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.CONFIRMED, 5_000, "order-key");

            given(paymentRepository.findByOrderIdAndStatus(1L, PaymentStatus.APPROVED))
                    .willReturn(Optional.of(payment));
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            CancelResult cancelResult = new CancelResult(
                    "tgen_abc123", "1", "CANCELED", "2025-07-01T19:00:00+09:00");
            given(paymentProcessor.cancel("tgen_abc123", "고객 요청", 5_000))
                    .willReturn(cancelResult);

            // Act
            paymentService.cancelPayment(1L, 1L);

            // Assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(payment.getCancelReason()).isEqualTo("고객 요청");
            assertThat(payment.getCancelledAt()).isNotNull();
            verify(orderService).cancelOrder(1L, 1L);
        }

        @Test
        @DisplayName("실패: 토스 취소 API 에러 시 PAYMENT_CANCEL_FAILED 에러")
        void failure_tossError() {
            // Arrange
            PaymentEntity payment = TestFixtures.payment(10L, 1L, 5_000, PaymentStatus.APPROVED,
                    LocalDateTime.of(2026, 4, 5, 12, 0), "idem-1");
            payment.approve(LocalDateTime.of(2026, 4, 5, 12, 0), "idem-1", "tgen_abc123", "카드");

            OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.CONFIRMED, 5_000, "order-key");

            given(paymentRepository.findByOrderIdAndStatus(1L, PaymentStatus.APPROVED))
                    .willReturn(Optional.of(payment));
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            given(paymentProcessor.cancel("tgen_abc123", "고객 요청", 5_000))
                    .willThrow(new TossPaymentException("CANCEL_FAILED", "취소 실패", 500));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.cancelPayment(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PAYMENT_CANCEL_FAILED);
        }

        @Test
        @DisplayName("결제 미존재: APPROVED 상태 결제가 없으면 ORDER_NOT_FOUND 에러")
        void paymentNotFound() {
            // Arrange
            given(paymentRepository.findByOrderIdAndStatus(1L, PaymentStatus.APPROVED))
                    .willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.cancelPayment(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 취소된 결제: APPROVED 상태가 아니면 결제를 찾을 수 없음")
        void alreadyCancelled() {
            // Arrange: CANCELLED 상태 결제는 findByOrderIdAndStatus(APPROVED)에서 조회되지 않음
            given(paymentRepository.findByOrderIdAndStatus(1L, PaymentStatus.APPROVED))
                    .willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.cancelPayment(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }
    }
}
