package com.todaybread.server.domain.payment.repository;

import com.todaybread.server.domain.payment.entity.PaymentEntity;
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
}
