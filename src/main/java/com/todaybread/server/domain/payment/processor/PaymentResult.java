package com.todaybread.server.domain.payment.processor;

import com.todaybread.server.domain.payment.entity.PaymentStatus;

/**
 * 결제 처리 결과를 나타내는 레코드입니다.
 *
 * @param status  결제 상태
 * @param message 결제 결과 메시지
 */
public record PaymentResult(PaymentStatus status, String message) {
}
