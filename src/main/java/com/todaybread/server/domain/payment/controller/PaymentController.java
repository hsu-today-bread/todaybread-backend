package com.todaybread.server.domain.payment.controller;

import com.todaybread.server.domain.payment.config.TossPaymentProperties;
import com.todaybread.server.domain.payment.dto.PaymentConfirmRequest;
import com.todaybread.server.domain.payment.dto.PaymentConfirmResponse;
import com.todaybread.server.domain.payment.dto.PaymentRequest;
import com.todaybread.server.domain.payment.dto.PaymentResponse;
import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.service.PaymentService;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.global.util.JwtRoleHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 결제 컨트롤러입니다.
 * 결제 요청 처리 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final TossPaymentProperties tossPaymentProperties;

    /**
     * 결제를 처리합니다.
     *
     * @param jwt            JWT 토큰
     * @param idempotencyKey 멱등성 키
     * @param request        결제 요청
     * @return 결제 응답
     */
    @Operation(summary = "결제 요청")
    @PostMapping
    public PaymentResponse processPayment(@AuthenticationPrincipal Jwt jwt,
                                          @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
                                          @RequestBody @Valid PaymentRequest request) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return paymentService.processPayment(userId, request, idempotencyKey);
    }

    /**
     * 토스 결제 승인을 확정합니다.
     * 프론트엔드에서 받은 paymentKey, orderId, amount를 검증하고 토스 Confirm API를 호출합니다.
     *
     * @param jwt            JWT 토큰
     * @param idempotencyKey 멱등성 키 (Idempotency-Key 헤더)
     * @param request        결제 승인 확정 요청
     * @return 결제 승인 확정 응답
     */
    @Operation(summary = "결제 승인 확정 (토스)")
    @PostMapping("/confirm")
    public PaymentConfirmResponse confirmPayment(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid PaymentConfirmRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new CustomException(ErrorCode.PAYMENT_IDEMPOTENCY_KEY_MISSING);
        }

        Long userId = JwtRoleHelper.getUserId(jwt);
        PaymentEntity payment = paymentService.confirmPayment(
                userId, request.paymentKey(), request.orderId(), request.amount(), idempotencyKey);
        return PaymentConfirmResponse.of(payment);
    }

    /**
     * 토스 페이먼츠 Client Key를 조회합니다.
     * 프론트엔드에서 토스 결제 위젯을 초기화할 때 사용합니다.
     * Client Key는 공개 키이므로 인증 없이 접근 가능합니다.
     *
     * @return clientKey를 포함한 JSON 응답
     */
    @Operation(summary = "토스 Client Key 조회")
    @GetMapping("/client-key")
    public Map<String, String> getClientKey() {
        return Map.of("clientKey", tossPaymentProperties.clientKey());
    }
}
