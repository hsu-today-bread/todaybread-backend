package com.todaybread.server.domain.notification.event;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.notification.service.NotificationService;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 커밋 이후 재고 알림 이벤트를 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockNotificationEventListener {

    private final BreadRepository breadRepository;
    private final StoreRepository storeRepository;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(StockNotificationEvent event) {
        BreadEntity bread = breadRepository.findById(event.breadId()).orElse(null);
        if (bread == null) {
            log.debug("재고 알림 이벤트 스킵: 빵 없음 breadId={}", event.breadId());
            return;
        }

        StoreEntity store = storeRepository.findById(bread.getStoreId()).orElse(null);
        if (store == null) {
            log.debug("재고 알림 이벤트 스킵: 매장 없음 storeId={}, breadId={}", bread.getStoreId(), bread.getId());
            return;
        }

        notificationService.triggerStockNotification(bread, store, event.eventType(), event.eventOccurredAt());
    }
}
