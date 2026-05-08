package com.todaybread.server.domain.notification.event;

/**
 * 주문 확정 알림 이벤트입니다.
 *
 * @param orderId 주문 ID
 */
public record OrderConfirmedNotificationEvent(
        Long orderId
) {
}
