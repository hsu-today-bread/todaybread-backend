package com.todaybread.server.domain.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 결제 요청 DTO
 *
 * @param orderId 주문 ID
 * @param amount 결제 금액
 */
public record PaymentRequest(
        @NotNull Long orderId,
        @NotNull @Min(1) Integer amount
) {
}
