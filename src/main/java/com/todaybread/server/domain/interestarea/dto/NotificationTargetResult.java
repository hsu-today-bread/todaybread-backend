package com.todaybread.server.domain.interestarea.dto;

import java.util.List;

/**
 * 알림 대상 계산 결과 DTO
 *
 * @param targets 알림 대상 목록
 */
public record NotificationTargetResult(
        List<NotificationTarget> targets
) {
}
