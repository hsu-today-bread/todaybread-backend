package com.todaybread.server.domain.notification.dto;

import java.util.Map;

/**
 * FCM 메시지 발송 요청 DTO
 *
 * @param token FCM 디바이스 토큰
 * @param title 알림 제목
 * @param body 알림 본문
 * @param data 알림 클릭 시 화면 이동용 data payload
 */
public record FcmSendRequest(
    String token,
    String title,
    String body,
    Map<String, String> data
) {
}
