package com.todaybread.server.domain.payment.service;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.order.service.InventoryRestorer;
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
import java.util.Collections;
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
            given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "idem-1")).willReturn(Optional.empty());
            given(paymentRepository.findByOrderId(1L)).willReturn(Optional.empty());
            given(paymentRepository.save(any(PaymentEntity.class))).willAnswer(inv -> inv.getArgument(0));
            given(clock.instant()).willReturn(TestFixtures.FIXED_CLOCK.instant());
            given(clock.getZone()).willReturn(TestFixtures.FIXED_CLOCK.getZone());

            PaymentResult result = new PaymentResult(
                    PaymentStatus.APPROVED, "ok", "tgen_abc123", "카드", "2025-07-01T18:31:00+09:00");
            given(paymentProcessor.confirm("tgen_abc123", "order_1", 5_000, "idem-1")).willReturn(result);

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
            given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "idem-1")).willReturn(Optional.empty());
            given(paymentRepository.findByOrderId(1L)).willReturn(Optional.empty());
            given(paymentRepository.save(any(PaymentEntity.class))).willAnswer(inv -> inv.getArgument(0));

            given(paymentProcessor.confirm("tgen_abc123", "order_1", 5_000, "idem-1"))
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
            given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "idem-1")).willReturn(Optional.of(existing));
            // C3: 소유자 검증을 위한 주문 조회 stub
            OrderEntity existingOrder = TestFixtures.order(1L, 1L, 100L, OrderStatus.CONFIRMED, 5_000, "order-key");
            given(orderRepository.findById(1L)).willReturn(Optional.of(existingOrder));

            // Act
            PaymentEntity payment = paymentService.confirmPayment(1L, "tgen_abc123", 1L, 5_000, "idem-1");

            // Assert
            assertThat(payment.getId()).isEqualTo(10L);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            verify(paymentProcessor, never()).confirm(any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("금액 불일치: PAYMENT_AMOUNT_MISMATCH 에러")
        void amountMismatch() {
            // Arrange
            OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 5_000, "order-key");
            given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
            given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "idem-1")).willReturn(Optional.empty());

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
            given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "idem-1")).willReturn(Optional.empty());
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
            given(paymentRepository.findByOrderIdAndIdempotencyKey(1L, "idem-1")).willReturn(Optional.empty());

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
        @DisplayName("성공: 토스 취소 성공 시 Payment=CANCELLED, 주문 CANCELLED, 재고 복원")
        void success() {
            // Arrange
            PaymentEntity payment = TestFixtures.payment(10L, 1L, 5_000, PaymentStatus.APPROVED,
                    LocalDateTime.of(2026, 4, 5, 12, 0), "idem-1");
            // paymentKey 설정
            payment.approve(LocalDateTime.of(2026, 4, 5, 12, 0), "idem-1", "tgen_abc123", "카드");

            OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.CONFIRMED, 5_000, "order-key");

            // prepareCancelPayment: 1단계
            given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
            given(paymentRepository.findByOrderIdAndStatus(1L, PaymentStatus.APPROVED))
                    .willReturn(Optional.of(payment));

            // 2단계: 토스 Cancel API
            CancelResult cancelResult = new CancelResult(
                    "tgen_abc123", "1", "CANCELED", "2025-07-01T19:00:00+09:00");
            given(paymentProcessor.cancel("tgen_abc123", "고객 요청", 5_000))
                    .willReturn(cancelResult);

            // completeCancelPayment: 3단계
            given(paymentRepository.findById(10L)).willReturn(Optional.of(payment));
            given(orderItemRepository.findByOrderId(1L)).willReturn(Collections.emptyList());
            given(clock.getZone()).willReturn(TestFixtures.FIXED_CLOCK.getZone());

            // Act
            paymentService.cancelPayment(1L, 1L);

            // Assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(payment.getCancelReason()).isEqualTo("고객 요청");
            assertThat(payment.getCancelledAt()).isNotNull();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(inventoryRestorer).restoreInventory(eq(1L), any());
        }

        @Test
        @DisplayName("실패: 토스 취소 API 에러 시 PAYMENT_CANCEL_FAILED 에러, 주문 CONFIRMED 복원")
        void failure_tossError() {
            // Arrange
            PaymentEntity payment = TestFixtures.payment(10L, 1L, 5_000, PaymentStatus.APPROVED,
                    LocalDateTime.of(2026, 4, 5, 12, 0), "idem-1");
            payment.approve(LocalDateTime.of(2026, 4, 5, 12, 0), "idem-1", "tgen_abc123", "카드");

            OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.CONFIRMED, 5_000, "order-key");

            // prepareCancelPayment: 1단계
            given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
            given(paymentRepository.findByOrderIdAndStatus(1L, PaymentStatus.APPROVED))
                    .willReturn(Optional.of(payment));

            // 2단계: 토스 Cancel API 실패
            given(paymentProcessor.cancel("tgen_abc123", "고객 요청", 5_000))
                    .willThrow(new TossPaymentException("CANCEL_FAILED", "취소 실패", 500));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.cancelPayment(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PAYMENT_CANCEL_FAILED);

            // C4: 실패 시 CONFIRMED로 복원됨
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("주문 미존재: findByIdWithLock이 빈 결과면 ORDER_NOT_FOUND 에러")
        void orderNotFound() {
            // Arrange
            given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.cancelPayment(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("CONFIRMED가 아닌 주문: ORDER_STATUS_CANNOT_CHANGE 에러")
        void orderNotConfirmed() {
            // Arrange
            OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 5_000, "order-key");
            given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.cancelPayment(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_STATUS_CANNOT_CHANGE);
        }
    }
}
