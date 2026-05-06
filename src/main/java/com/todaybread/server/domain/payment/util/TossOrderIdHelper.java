package com.todaybread.server.domain.payment.util;

/**
 * 토스 페이먼츠 orderId 형식 변환 유틸리티입니다.
 * 토스 orderId 규격: 6~64자 영문/숫자/-/_ 문자열
 */
public final class TossOrderIdHelper {

    private static final String PREFIX = "order_";

    private TossOrderIdHelper() {
    }

    /**
     * 내부 주문 ID를 토스 orderId 형식으로 변환합니다.
     *
     * @param orderId 내부 주문 ID
     * @return 토스 orderId (예: "order_123")
     */
    public static String toTossOrderId(Long orderId) {
        return PREFIX + orderId;
    }

    /**
     * 토스 orderId에서 내부 주문 ID를 추출합니다.
     *
     * @param tossOrderId 토스 orderId (예: "order_123")
     * @return 내부 주문 ID
     */
    public static Long fromTossOrderId(String tossOrderId) {
        return Long.parseLong(tossOrderId.replace(PREFIX, ""));
    }
}
