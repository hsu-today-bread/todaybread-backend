package com.todaybread.server.domain.notification.service;

import com.todaybread.server.domain.notification.dto.FcmSendRequest;
import com.todaybread.server.domain.notification.dto.FcmSendResult;

/**
 * FCM 메시지 발송 전략 인터페이스.
 *
 * <p>구현체:
 * <ul>
 *   <li>{@code FirebaseFcmSender} - 실제 Firebase Admin SDK를 통한 발송</li>
 *   <li>{@code NoopFcmSender} - Firebase 초기화 실패 또는 비활성 시 무동작 대체 구현</li>
 * </ul>
 */
public interface FcmSender {

    /**
     * FCM 메시지를 발송한다.
     *
     * @param request 발송 요청 (토큰, 제목, 본문, data payload)
     * @return 발송 결과
     */
    FcmSendResult send(FcmSendRequest request);
}
