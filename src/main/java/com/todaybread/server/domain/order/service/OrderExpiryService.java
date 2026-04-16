package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.order.config.OrderExpiryProperties;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 만료 PENDING 주문 처리를 담당하는 서비스입니다.
 * 만료 대상 조회 및 일괄 처리를 수행합니다.
 * 개별 주문 취소는 OrderExpiryCanceller에 위임합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExpiryService {

    private final OrderRepository orderRepository;
    private final OrderExpiryCanceller orderExpiryCanceller;
    private final Clock clock;
    private final OrderExpiryProperties properties;

    /**
     * 만료 대상 PENDING 주문을 조회합니다.
     * 현재 시각에서 만료 기준 시간을 뺀 cutoffTime 이전에 생성된 PENDING 주문을 반환합니다.
     *
     * @return 만료 대상 주문 목록 (ID 오름차순)
     */
    public List<OrderEntity> findExpiredPendingOrders() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoffTime = now.minusMinutes(properties.getTimeoutMinutes());
        return orderRepository.findExpiredPendingOrders(OrderStatus.PENDING, cutoffTime, PageRequest.of(0, properties.getBatchSize()));
    }

    /**
     * 만료 대상 PENDING 주문을 일괄 처리합니다.
     * 각 주문은 OrderExpiryCanceller에서 개별 트랜잭션으로 처리되므로
     * 이 메서드에는 트랜잭션 어노테이션을 적용하지 않습니다.
     *
     * <p>한 번의 실행에서 최대 {@code batch-size}건만 처리합니다.
     * 만료 대상이 batch-size를 초과하면 다음 스케줄러 실행에서 나머지를 처리합니다.
     * fixedDelay 방식이므로 이전 실행 완료 후 즉시 다음 실행이 시작됩니다.</p>
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
        int skippedCount = 0;
        int failedCount = 0;

        for (OrderEntity order : expiredOrders) {
            try {
                CancelResult result = orderExpiryCanceller.cancelExpiredOrder(order.getId());
                if (result == CancelResult.CANCELLED) {
                    cancelledCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                failedCount++;
                log.error("주문 취소 중 오류 발생: orderId={}, error={}", order.getId(), e.getMessage(), e);
            }
        }

        log.info("만료 주문 처리 완료: 총 {}건 중 {}건 취소, {}건 건너뜀, {}건 실패",
                totalCount, cancelledCount, skippedCount, failedCount);

        return cancelledCount;
    }
}
