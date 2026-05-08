package com.todaybread.server.domain.notification.dto;

/**
 * FCM 메시지 발송 결과 DTO
 *
 * @param isSuccess 발송 성공 여부
 * @param isNoop NoopFcmSender에 의한 무동작 여부
 * @param isTokenExpired 토큰 만료/무효 여부
 * @param errorMessage 실패 시 에러 메시지
 */
public record FcmSendResult(
    boolean isSuccess,
    boolean isNoop,
    boolean isTokenExpired,
    String errorMessage
) {

    public static FcmSendResult success() {
        return new FcmSendResult(true, false, false, null);
    }

    public static FcmSendResult noop() {
        return new FcmSendResult(false, true, false, null);
    }

    public static FcmSendResult failure(String error) {
        return new FcmSendResult(false, false, false, error);
    }

    public static FcmSendResult tokenExpired() {
        return new FcmSendResult(false, false, true, "token expired or invalid");
    }

    /**
     * notification_log 저장 여부를 결정한다.
     * 실제 발송 성공(isSuccess=true)이고 Noop이 아닌 경우에만 true를 반환한다.
     */
    public boolean shouldSaveLog() {
        return isSuccess && !isNoop;
    }
}
