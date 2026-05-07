package com.todaybread.server.domain.order.scheduler;

import com.todaybread.server.domain.order.service.OrderExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PENDING 상태로 만료된 주문을 주기적으로 취소하는 스케줄러입니다.
 * fixedDelay 방식으로 이전 실행이 완료된 후 설정된 간격만큼 대기한 뒤 다음 실행을 시작합니다.
 *
 * 다중 인스턴스 환경에서는 모든 인스턴스가 동일한 만료 주문을 조회할 수 있습니다.
 * 비관적 락과 상태 재확인으로 데이터 정합성은 보장되지만, 불필요한 락 경합이 발생할 수 있습니다.
 * 다중 인스턴스 배포 시 {@code order.expiry.scheduler-enabled=false}로 특정 인스턴스의 스케줄러를
 * 비활성화하거나, ShedLock 등의 분산 락을 도입하는 것을 권장합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "order.expiry.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class PendingOrderExpiryScheduler {

    private final OrderExpiryService orderExpiryService;

    @Scheduled(fixedDelayString = "${order.expiry.scheduler-interval-ms:60000}")
    public void run() {
        log.info("만료 주문 처리 시작");
        orderExpiryService.processExpiredOrders();
    }
}
