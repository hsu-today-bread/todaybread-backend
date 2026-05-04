package com.todaybread.server.domain.payment.repository;

import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 결제 리포지토리입니다.
 */
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    /**
     * 주문 ID로 결제 정보를 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 결제 엔티티
     */
    Optional<PaymentEntity> findByOrderId(Long orderId);

    /**
     * 주문 ID와 idempotency key로 결제를 조회합니다.
     *
     * @param orderId 주문 ID
     * @param idempotencyKey idempotency key
     * @return 결제 엔티티
     */
    Optional<PaymentEntity> findByOrderIdAndIdempotencyKey(Long orderId, String idempotencyKey);

    /**
     * idempotency key로 결제를 조회합니다.
     *
     * @param idempotencyKey idempotency key
     * @return 결제 엔티티
     */
    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * 주문 ID와 결제 상태로 결제를 조회합니다.
     *
     * @param orderId 주문 ID
     * @param status  결제 상태
     * @return 결제 엔티티
     */
    Optional<PaymentEntity> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
