package com.todaybread.server.domain.order.service;

/**
 * 만료 주문 취소 결과를 나타내는 열거형입니다.
 */
public enum CancelResult {
    /** 주문이 정상적으로 취소되고 재고가 복원됨 */
    CANCELLED,
    /** 비관적 락 재조회 시 주문이 존재하지 않아 건너뜀 */
    SKIPPED_NOT_FOUND,
    /** 비관적 락 재조회 시 주문 상태가 이미 PENDING이 아니어서 건너뜀 (결제 완료 등) */
    SKIPPED_STATUS_CHANGED
}
