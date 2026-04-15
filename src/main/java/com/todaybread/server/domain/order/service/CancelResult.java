package com.todaybread.server.domain.order.service;

/**
 * 만료 주문 취소 결과를 나타내는 열거형입니다.
 */
public enum CancelResult {
    CANCELLED,
    SKIPPED_NOT_FOUND,
    SKIPPED_STATUS_CHANGED
}
