package com.todaybread.server.domain.notification.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.interestarea.dto.NotificationTarget;
import com.todaybread.server.domain.interestarea.dto.NotificationTargetResult;
import com.todaybread.server.domain.interestarea.service.NotificationTargetCalculator;
import com.todaybread.server.domain.notification.dto.FcmSendRequest;
import com.todaybread.server.domain.notification.dto.FcmSendResult;
import com.todaybread.server.domain.notification.entity.FcmTokenEntity;
import com.todaybread.server.domain.notification.entity.NotificationLogEntity;
import com.todaybread.server.domain.notification.entity.NotificationType;
import com.todaybread.server.domain.notification.event.StockEventType;
import com.todaybread.server.domain.notification.repository.NotificationLogRepository;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.store.entity.FavouriteStoreEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.FavouriteStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * FCM 알림 대상 계산, 중복 확인, 발송, 이력 저장을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String KEYWORD_STOCK_TITLE = "찾던 빵이 등록됐어요";
    private static final String FAVORITE_STORE_STOCK_TITLE = "단골 매장에 새 빵이 등록됐어요";
    private static final String ORDER_CREATED_TITLE = "새 주문이 들어왔어요";

    private final NotificationTargetCalculator notificationTargetCalculator;
    private final FavouriteStoreRepository favouriteStoreRepository;
    private final FcmTokenService fcmTokenService;
    private final NotificationLogRepository notificationLogRepository;
    private final FcmSender fcmSender;
    private final OrderAvailableTextFormatter orderAvailableTextFormatter;
    private final Clock clock;

    /**
     * 새 빵 등록/품절 해제 이벤트에 대한 키워드 및 단골 매장 알림을 발송합니다.
     */
    @Transactional
    public void triggerStockNotification(BreadEntity bread, StoreEntity store,
                                         StockEventType eventType, LocalDateTime eventOccurredAt) {
        Optional<String> orderAvailableUntilText =
                orderAvailableTextFormatter.format(store, bread.getRemainingQuantity() > 0);
        if (orderAvailableUntilText.isEmpty()) {
            log.debug("재고 알림 스킵: 판매 불가 상태 storeId={}, breadId={}", store.getId(), bread.getId());
            return;
        }

        sendKeywordStockNotifications(bread, store, eventType, eventOccurredAt, orderAvailableUntilText.get());
        sendFavoriteStoreStockNotifications(bread, store, eventType, eventOccurredAt, orderAvailableUntilText.get());
    }

    /**
     * 주문 확정 이벤트에 대한 사장님 알림을 발송합니다.
     */
    @Transactional
    public void triggerOrderCreatedNotification(OrderEntity order, StoreEntity store) {
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            log.debug("주문 알림 스킵: 확정 상태 아님 orderId={}, status={}", order.getId(), order.getStatus());
            return;
        }

        String orderNumber = order.getOrderNumber() != null ? order.getOrderNumber() : String.valueOf(order.getId());
        String body = "주문을 확인해주세요 " + orderNumber;
        String targetId = String.valueOf(order.getId());
        String eventKey = EventKeyGenerator.orderCreatedKey(order.getId());
        Map<String, String> data = Map.of(
                "type", NotificationType.ORDER_CREATED.name(),
                "storeId", String.valueOf(store.getId()),
                "orderId", String.valueOf(order.getId())
        );

        sendToUser(store.getUserId(), NotificationType.ORDER_CREATED, targetId, eventKey,
                ORDER_CREATED_TITLE, body, data);
    }

    /**
     * 개발용 테스트 알림을 발송합니다.
     */
    @Transactional
    public FcmSendResult sendTestNotification(Long userId) {
        Optional<FcmTokenEntity> token = fcmTokenService.findActiveToken(userId);
        if (token.isEmpty()) {
            return FcmSendResult.failure("active fcm token not found");
        }

        FcmSendRequest request = new FcmSendRequest(
                token.get().getToken(),
                "FCM 테스트 알림",
                "테스트 알림입니다.",
                Map.of("type", "TEST")
        );
        FcmSendResult result = fcmSender.send(request);
        if (result.isTokenExpired()) {
            fcmTokenService.markTokenInvalid(token.get().getToken());
        }
        return result;
    }

    private void sendKeywordStockNotifications(BreadEntity bread, StoreEntity store,
                                               StockEventType eventType, LocalDateTime eventOccurredAt,
                                               String orderAvailableUntilText) {
        NotificationTargetResult result = notificationTargetCalculator.calculateTargets(bread, store, clock);
        String targetId = String.valueOf(bread.getId());
        String eventKey = EventKeyGenerator.keywordStockKey(eventType, bread.getId(), eventOccurredAt);
        Map<String, String> data = Map.of(
                "type", NotificationType.KEYWORD_STOCK.name(),
                "storeId", String.valueOf(store.getId()),
                "breadId", String.valueOf(bread.getId())
        );

        for (NotificationTarget target : result.targets()) {
            if (target.matchedKeywords().isEmpty()) {
                continue;
            }
            String keywordName = target.matchedKeywords().get(0);
            String body = "%s 근처 %s 매장에 \"%s\"으로 등록한 빵이 등록되었어요! %s 주문 가능해요."
                    .formatted(target.interestAreaName(), target.storeInfo().storeName(),
                            keywordName, orderAvailableUntilText);
            sendToUser(target.userId(), NotificationType.KEYWORD_STOCK, targetId, eventKey,
                    KEYWORD_STOCK_TITLE, body, data);
        }
    }

    private void sendFavoriteStoreStockNotifications(BreadEntity bread, StoreEntity store,
                                                    StockEventType eventType, LocalDateTime eventOccurredAt,
                                                    String orderAvailableUntilText) {
        List<FavouriteStoreEntity> favourites =
                favouriteStoreRepository.findByStoreIdForUserNotificationTargets(store.getId());
        String targetId = String.valueOf(bread.getId());
        String eventKey = EventKeyGenerator.favouriteStoreStockKey(eventType, bread.getId(), eventOccurredAt);
        String body = "단골 매장 %s에 \"%s\"이 등록되었어요! %s 주문 가능해요."
                .formatted(store.getName(), bread.getName(), orderAvailableUntilText);
        Map<String, String> data = Map.of(
                "type", NotificationType.FAVORITE_STORE_STOCK.name(),
                "storeId", String.valueOf(store.getId()),
                "breadId", String.valueOf(bread.getId())
        );

        for (FavouriteStoreEntity favourite : favourites) {
            sendToUser(favourite.getUserId(), NotificationType.FAVORITE_STORE_STOCK, targetId, eventKey,
                    FAVORITE_STORE_STOCK_TITLE, body, data);
        }
    }

    private void sendToUser(Long userId, NotificationType type, String targetId, String eventKey,
                            String title, String body, Map<String, String> data) {
        if (notificationLogRepository.existsByUserIdAndTypeAndEventKey(userId, type, eventKey)) {
            log.debug("알림 중복 스킵: userId={}, type={}, eventKey={}", userId, type, eventKey);
            return;
        }

        Optional<FcmTokenEntity> token = fcmTokenService.findActiveToken(userId);
        if (token.isEmpty()) {
            log.debug("활성 FCM 토큰 없음: userId={}, type={}, eventKey={}", userId, type, eventKey);
            return;
        }

        FcmSendResult result = fcmSender.send(new FcmSendRequest(token.get().getToken(), title, body, data));
        if (result.isTokenExpired()) {
            fcmTokenService.markTokenInvalid(token.get().getToken());
            log.warn("FCM 토큰 만료/무효: userId={}, type={}, targetId={}", userId, type, targetId);
            return;
        }
        if (!result.isSuccess()) {
            log.warn("FCM 발송 실패: userId={}, type={}, targetId={}, error={}",
                    userId, type, targetId, result.errorMessage());
            return;
        }
        if (!result.shouldSaveLog()) {
            log.debug("FCM Noop 발송: userId={}, type={}, targetId={}", userId, type, targetId);
            return;
        }

        try {
            notificationLogRepository.save(NotificationLogEntity.builder()
                    .userId(userId)
                    .type(type)
                    .targetId(targetId)
                    .eventKey(eventKey)
                    .title(title)
                    .body(body)
                    .build());
            log.info("FCM 발송 성공: userId={}, type={}, targetId={}", userId, type, targetId);
        } catch (DataIntegrityViolationException e) {
            log.debug("알림 로그 중복 저장 스킵: userId={}, type={}, eventKey={}", userId, type, eventKey);
        }
    }
}
