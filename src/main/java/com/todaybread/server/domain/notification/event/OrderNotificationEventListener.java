package com.todaybread.server.domain.notification.event;

import com.todaybread.server.domain.notification.service.NotificationService;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 커밋 이후 주문 확정 알림 이벤트를 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationEventListener {

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderConfirmedNotificationEvent event) {
        OrderEntity order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) {
            log.debug("주문 알림 이벤트 스킵: 주문 없음 orderId={}", event.orderId());
            return;
        }

        StoreEntity store = storeRepository.findById(order.getStoreId()).orElse(null);
        if (store == null) {
            log.debug("주문 알림 이벤트 스킵: 매장 없음 storeId={}, orderId={}", order.getStoreId(), order.getId());
            return;
        }

        notificationService.triggerOrderCreatedNotification(order, store);
    }
}
