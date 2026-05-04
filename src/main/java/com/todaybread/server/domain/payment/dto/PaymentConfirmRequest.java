package com.todaybread.server.domain.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 토스 결제 승인 확정 요청 DTO
 *
 * @param paymentKey 토스 페이먼츠 결제 고유 키
 * @param orderId    주문 ID
 * @param amount     결제 금액
 */
public record PaymentConfirmRequest(
        @NotBlank String paymentKey,
        @NotNull Long orderId,
        @NotNull @Min(1) Integer amount
) {
}
