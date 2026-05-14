package com.todaybread.server.domain.payment.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 토스 페이먼츠 orderId 형식 변환 유틸리티입니다.
 * 토스 orderId 규격: 6~64자 영문/숫자/-/_ 문자열
 */
public final class TossOrderIdHelper {

    private static final String PREFIX = "tb_";
    private static final String LEGACY_PREFIX = "order_";
    private static final int MAX_LENGTH = 64;
    private static final Pattern ALLOWED_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{6,64}$");
    private static final Pattern CURRENT_PATTERN = Pattern.compile("^tb_(\\d+)_([A-Za-z0-9_-]+)$");
    private static final Pattern LEGACY_PATTERN = Pattern.compile("^order_(\\d+)$");

    private TossOrderIdHelper() {
    }

    /**
     * 내부 주문 ID와 주문 생성 멱등성 키를 토스 orderId 형식으로 변환합니다.
     *
     * @param orderId             내부 주문 ID
     * @param orderIdempotencyKey 주문 생성 멱등성 키
     * @return 토스 orderId (예: "tb_123_550e8400e29b41d4a716446655440000")
     */
    public static String toTossOrderId(Long orderId, String orderIdempotencyKey) {
        String tossOrderId = PREFIX + orderId + "_" + normaliseToken(orderIdempotencyKey);
        if (!isValidTossOrderId(tossOrderId)) {
            throw new IllegalArgumentException("Invalid Toss orderId format");
        }
        return tossOrderId;
    }

    /**
     * 기존 orderId 형식입니다. 신규 결제 승인 경로에서는 사용하지 않습니다.
     */
    @Deprecated(forRemoval = false)
    public static String toTossOrderId(Long orderId) {
        return LEGACY_PREFIX + orderId;
    }

    /**
     * 토스 orderId에서 내부 주문 ID를 추출합니다.
     *
     * @param tossOrderId 토스 orderId (예: "tb_123_random" 또는 "order_123")
     * @return 내부 주문 ID
     */
    public static Long fromTossOrderId(String tossOrderId) {
        if (tossOrderId == null || tossOrderId.isBlank()) {
            throw new IllegalArgumentException("Toss orderId is required");
        }

        Matcher currentMatcher = CURRENT_PATTERN.matcher(tossOrderId);
        if (currentMatcher.matches()) {
            return Long.parseLong(currentMatcher.group(1));
        }

        Matcher legacyMatcher = LEGACY_PATTERN.matcher(tossOrderId);
        if (legacyMatcher.matches()) {
            return Long.parseLong(legacyMatcher.group(1));
        }

        throw new IllegalArgumentException("Invalid Toss orderId format");
    }

    /**
     * 신규 토스 orderId의 고유 토큰이 주문 생성 멱등성 키와 일치하는지 확인합니다.
     */
    public static boolean matchesOrderIdempotencyKey(String tossOrderId, String orderIdempotencyKey) {
        if (tossOrderId == null) {
            return false;
        }
        Matcher matcher = CURRENT_PATTERN.matcher(tossOrderId);
        if (!matcher.matches()) {
            return false;
        }
        try {
            return matcher.group(2).equals(normaliseToken(orderIdempotencyKey));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isCurrentFormat(String tossOrderId) {
        return tossOrderId != null
                && ALLOWED_PATTERN.matcher(tossOrderId).matches()
                && CURRENT_PATTERN.matcher(tossOrderId).matches();
    }

    private static boolean isValidTossOrderId(String tossOrderId) {
        return tossOrderId != null
                && tossOrderId.length() <= MAX_LENGTH
                && ALLOWED_PATTERN.matcher(tossOrderId).matches();
    }

    private static String normaliseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Order idempotency key is required");
        }
        return token.replace("-", "");
    }
}
