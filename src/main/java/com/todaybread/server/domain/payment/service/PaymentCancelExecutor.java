package com.todaybread.server.domain.payment.service;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.order.service.InventoryRestorer;
import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import com.todaybread.server.domain.payment.processor.CancelResult;
import com.todaybread.server.domain.payment.repository.PaymentRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 결제 취소의 트랜잭션 단위 작업을 담당하는 클래스입니다.
 * PaymentService에서 분리하여 Spring AOP 프록시를 통한 @Transactional 적용을 보장합니다.
 *
 * @see com.todaybread.server.domain.order.service.OrderExpiryCanceller 동일 패턴
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCancelExecutor {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryRestorer inventoryRestorer;
    private final Clock clock;

    /** 취소 준비에 필요한 정보를 담는 레코드 */
    public record CancelPreparation(Long paymentId, String paymentKey, int amount) {}

    /**
     * 취소 1단계: 비관적 락으로 주문을 조회하고 CANCEL_PENDING으로 전환합니다.
     *
     * @param userId  유저 ID
     * @param orderId 주문 ID
     * @return 취소 준비 정보
     */
    @Transactional
    public CancelPreparation prepareCancelPayment(Long userId, Long orderId) {
        // 1. 비관적 락으로 주문 조회
        OrderEntity order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 소유자 검증
        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 3. 상태 검증: CONFIRMED만 허용
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.ORDER_STATUS_CANNOT_CHANGE);
        }

        // 4. APPROVED 상태 결제 조회
        PaymentEntity payment = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.APPROVED)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 5. CANCEL_PENDING으로 전환
        order.updateStatus(OrderStatus.CANCEL_PENDING);

        return new CancelPreparation(payment.getId(), payment.getPaymentKey(), payment.getAmount());
    }

    /**
     * 취소 3단계 (성공): CANCELLED + payment.cancel() + 재고 복원
     *
     * @param orderId      주문 ID
     * @param paymentId    결제 ID
     * @param cancelResult 토스 취소 결과
     */
    @Transactional
    public void completeCancelPayment(Long orderId, Long paymentId, CancelResult cancelResult) {
        OrderEntity order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // cancelledAt 파싱
        LocalDateTime cancelledAt;
        if (cancelResult.cancelledAt() != null) {
            cancelledAt = parseCancelledAt(cancelResult.cancelledAt(), orderId);
        } else {
            log.warn("토스 취소 응답에 cancelledAt 없음, 현재 시각으로 대체: orderId={}", orderId);
            cancelledAt = LocalDateTime.now(clock);
        }
        payment.cancel("고객 요청", cancelledAt);

        // 주문 CANCELLED 전환
        order.updateStatus(OrderStatus.CANCELLED);

        // 재고 복원
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);
        inventoryRestorer.restoreInventory(orderId, orderItems);

        log.info("결제 취소 완료: orderId={}, paymentKey={}", orderId, payment.getPaymentKey());
    }

    /**
     * 취소 3단계 (실패): CONFIRMED로 복원
     *
     * @param orderId 주문 ID
     */
    @Transactional
    public void rollbackCancelPayment(Long orderId) {
        OrderEntity order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        order.updateStatus(OrderStatus.CONFIRMED);
        log.warn("결제 취소 실패로 CONFIRMED 복원: orderId={}", orderId);
    }

    /**
     * cancelledAt 문자열을 LocalDateTime으로 파싱합니다.
     * 파싱 실패 시 현재 시각으로 대체합니다.
     */
    private LocalDateTime parseCancelledAt(String cancelledAtStr, Long orderId) {
        try {
            OffsetDateTime odt = OffsetDateTime.parse(cancelledAtStr);
            return odt.atZoneSameInstant(clock.getZone()).toLocalDateTime();
        } catch (DateTimeParseException e) {
            log.warn("cancelledAt 파싱 실패: {}, 현재 시각으로 대체: orderId={}", cancelledAtStr, orderId, e);
            return LocalDateTime.now(clock);
        }
    }
}
