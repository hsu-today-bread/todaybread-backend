package com.todaybread.server.domain.store.util;

/**
 * 매장 판매 상태를 나타내는 enum입니다.
 */
public enum SellingStatus {
    /** 영업시간 내 + 주문마감 전 + 재고 있음 */
    SELLING,
    /** 영업시간 내 + 주문마감 전 + 재고 없음 */
    OPEN_SOLD_OUT,
    /** 비활성 / 휴무 / 영업시간 밖 / 주문마감 이후 */
    CLOSED
}
