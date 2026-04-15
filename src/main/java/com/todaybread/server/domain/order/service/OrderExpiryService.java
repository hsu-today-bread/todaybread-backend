package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 만료 PENDING 주문 처리를 담당하는 서비스입니다.
 * 만료 대상 조회, 개별 주문 취소 및 재고 복원을 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExpiryService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BreadRepository breadRepository;
    private final Clock clock;

    @Value("${order.expiry.timeout-minutes:10}")
    private long expiryTimeoutMinutes;

    /**
     * 만료 대상 PENDING 주문을 조회합니다.
     * 현재 시각에서 만료 기준 시간을 뺀 cutoffTime 이전에 생성된 PENDING 주문을 반환합니다.
     *
     * @return 만료 대상 주문 목록 (ID 오름차순)
     */
    @Transactional(readOnly = true)
    public List<OrderEntity> findExpiredPendingOrders() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoffTime = now.minusMinutes(expiryTimeoutMinutes);
        return orderRepository.findExpiredPendingOrders(cutoffTime);
    }

    /**
     * 개별 만료 주문을 취소하고 재고를 복원합니다.
     * 별도 트랜잭션(REQUIRES_NEW)으로 실행되어 하나의 실패가 다른 주문 처리에 영향을 주지 않습니다.
     *
     * @param orderId 취소할 주문 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelExpiredOrder(Long orderId) {
        Optional<OrderEntity> orderOptional = orderRepository.findByIdWithLock(orderId);

        if (orderOptional.isEmpty()) {
            log.warn("주문을 찾을 수 없어 건너뜀: orderId={}", orderId);
            return;
        }

        OrderEntity order = orderOptional.get();

        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("주문 상태 변경으로 건너뜀: orderId={}, status={}", orderId, order.getStatus());
            return;
        }

        order.updateStatus(OrderStatus.CANCELLED);

        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);

        List<Long> breadIds = orderItems.stream()
                .map(OrderItemEntity::getBreadId)
                .toList();

        List<BreadEntity> breads = breadRepository.findAllByIdWithLock(breadIds);

        Map<Long, BreadEntity> breadMap = breads.stream()
                .collect(Collectors.toMap(BreadEntity::getId, Function.identity()));

        for (OrderItemEntity item : orderItems) {
            BreadEntity bread = breadMap.get(item.getBreadId());
            if (bread == null) {
                log.warn("빵을 찾을 수 없어 재고 복원 건너뜀: orderId={}, breadId={}", orderId, item.getBreadId());
                continue;
            }
            bread.increaseQuantity(item.getQuantity());
        }

        log.info("주문 취소 완료: orderId={}", orderId);
    }

    /**
     * 만료 대상 PENDING 주문을 일괄 처리합니다.
     * 각 주문은 cancelExpiredOrder에서 개별 트랜잭션으로 처리되므로
     * 이 메서드에는 트랜잭션 어노테이션을 적용하지 않습니다.
     *
     * @return 취소된 주문 건수
     */
    public int processExpiredOrders() {
        List<OrderEntity> expiredOrders = findExpiredPendingOrders();

        if (expiredOrders.isEmpty()) {
            return 0;
        }

        int totalCount = expiredOrders.size();
        log.info("만료 대상 주문 {}건 조회됨", totalCount);

        int cancelledCount = 0;
        int failedCount = 0;

        for (OrderEntity order : expiredOrders) {
            try {
                cancelExpiredOrder(order.getId());
                cancelledCount++;
            } catch (Exception e) {
                failedCount++;
                log.error("주문 취소 중 오류 발생: orderId={}, error={}", order.getId(), e.getMessage(), e);
            }
        }

        int skippedCount = totalCount - cancelledCount - failedCount;
        log.info("만료 주문 처리 완료: 총 {}건 중 {}건 취소, {}건 건너뜀, {}건 실패",
                totalCount, cancelledCount, skippedCount, failedCount);

        return cancelledCount;
    }
}
