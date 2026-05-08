package com.todaybread.server.domain.notification.service;

import com.todaybread.server.domain.notification.dto.FcmSendRequest;
import com.todaybread.server.domain.notification.dto.FcmSendResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Firebase 초기화 실패 또는 notification.fcm.enabled=false일 때 사용되는 무동작 FcmSender 구현체.
 *
 * <p>항상 {@link FcmSendResult#noop()}을 반환하며, notification_log를 저장하지 않아
 * 나중에 FCM을 켰을 때 같은 (userId, type, event_key) 알림이 영구 차단되지 않도록 한다.
 */
@Slf4j
public class NoopFcmSender implements FcmSender {

    @Override
    public FcmSendResult send(FcmSendRequest request) {
        log.debug("NoopFcmSender: FCM send skipped (noop mode). token={}", request.token());
        return FcmSendResult.noop();
    }
}
