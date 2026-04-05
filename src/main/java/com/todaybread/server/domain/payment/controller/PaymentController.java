package com.todaybread.server.domain.payment.controller;

import com.todaybread.server.domain.payment.dto.PaymentRequest;
import com.todaybread.server.domain.payment.dto.PaymentResponse;
import com.todaybread.server.domain.payment.service.PaymentService;
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
}
