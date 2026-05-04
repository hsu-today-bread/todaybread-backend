package com.todaybread.server.domain.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.todaybread.server.domain.payment.config.TossPaymentProperties;
import com.todaybread.server.domain.payment.dto.PaymentConfirmRequest;
import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import com.todaybread.server.domain.payment.service.PaymentService;
import com.todaybread.server.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PaymentController 단위 테스트입니다.
 * 스탠드얼론 MockMvc 설정으로 Spring Security 없이 컨트롤러 로직만 검증합니다.
 *
 * Validates: Requirements 6.1, 6.2, 8.1, 8.2
 */
@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private TossPaymentProperties tossPaymentProperties;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final Jwt MOCK_JWT = Jwt.withTokenValue("test-token")
            .header("alg", "HS256")
            .subject("1")
            .claim("role", "USER")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new MockJwtArgumentResolver())
                .build();
    }

    /**
     * POST /api/payments/confirm 성공 케이스
     * 유효한 요청과 Idempotency-Key 헤더가 있으면 200 OK와 PaymentConfirmResponse를 반환한다.
     *
     * Validates: Requirements 6.1, 6.2
     */
    @Test
    void confirmPayment_withValidRequestAndIdempotencyKey_returns200() throws Exception {
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest("tgen_test123", 1L, 5000);

        PaymentEntity payment = PaymentEntity.builder()
                .orderId(1L)
                .amount(5000)
                .status(PaymentStatus.APPROVED)
                .paidAt(LocalDateTime.of(2025, 7, 1, 18, 30, 0))
                .idempotencyKey("idem-key-001")
                .build();
        ReflectionTestUtils.setField(payment, "id", 100L);
        ReflectionTestUtils.setField(payment, "method", "카드");

        given(paymentService.confirmPayment(eq(1L), eq("tgen_test123"), eq(1L), eq(5000), eq("idem-key-001")))
                .willReturn(payment);

        // when & then
        mockMvc.perform(post("/api/payments/confirm")
                        .header("Idempotency-Key", "idem-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(100))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.amount").value(5000))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.method").value("카드"));
    }

    /**
     * POST /api/payments/confirm에서 Idempotency-Key 헤더가 누락되면 PAYMENT_008 에러를 반환한다.
     *
     * Validates: Requirements 8.1, 8.2
     */
    @Test
    void confirmPayment_withoutIdempotencyKey_returns400WithPayment008() throws Exception {
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest("tgen_test123", 1L, 5000);

        // when & then
        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_008"))
                .andExpect(jsonPath("$.message").value("Idempotency-Key 헤더가 필요합니다."));
    }

    /**
     * POST /api/payments/confirm에서 요청 바디가 유효하지 않으면 (paymentKey 누락) 400 에러를 반환한다.
     *
     * Validates: Requirements 6.1
     */
    @Test
    void confirmPayment_withInvalidRequestBody_returns400() throws Exception {
        // given - paymentKey가 빈 문자열 (validation 실패)
        String invalidBody = """
                {
                    "paymentKey": "",
                    "orderId": 1,
                    "amount": 5000
                }
                """;

        // when & then
        mockMvc.perform(post("/api/payments/confirm")
                        .header("Idempotency-Key", "idem-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    /**
     * GET /api/payments/client-key는 clientKey를 포함한 JSON 응답을 반환한다.
     * 인증 없이 접근 가능해야 한다 (SecurityConfig에서 permitAll 설정).
     * 이 테스트에서는 스탠드얼론 MockMvc이므로 Security 없이 직접 접근 가능함을 확인한다.
     *
     * Validates: Requirements 1.1
     */
    @Test
    void getClientKey_returnsClientKey() throws Exception {
        // given
        given(tossPaymentProperties.clientKey()).willReturn("test_ck_abc123");

        // when & then
        mockMvc.perform(get("/api/payments/client-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientKey").value("test_ck_abc123"));
    }

    /**
     * @AuthenticationPrincipal Jwt 파라미터를 스탠드얼론 MockMvc에서 주입하기 위한 커스텀 리졸버입니다.
     */
    private static class MockJwtArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && Jwt.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            return MOCK_JWT;
        }
    }
}
