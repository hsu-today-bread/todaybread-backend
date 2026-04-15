package com.todaybread.server.domain.order.scheduler;

import com.todaybread.server.domain.order.service.OrderExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PENDING 상태로 만료된 주문을 주기적으로 취소하는 스케줄러입니다.
 * fixedDelay 방식으로 이전 실행이 완료된 후 설정된 간격만큼 대기한 뒤 다음 실행을 시작합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PendingOrderExpiryScheduler {

    private final OrderExpiryService orderExpiryService;

    @Scheduled(fixedDelayString = "${order.expiry.scheduler-interval-ms:60000}")
    public void run() {
        log.info("만료 주문 처리 시작");
        int cancelledCount = orderExpiryService.processExpiredOrders();
        log.info("만료 주문 처리 완료: {}건 취소", cancelledCount);
    }
}
