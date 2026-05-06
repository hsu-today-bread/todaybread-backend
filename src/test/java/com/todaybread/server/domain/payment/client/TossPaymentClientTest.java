package com.todaybread.server.domain.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaybread.server.domain.payment.client.dto.TossCancelResponse;
import com.todaybread.server.domain.payment.client.dto.TossConfirmResponse;
import com.todaybread.server.domain.payment.config.TossPaymentProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TossPaymentClient 단위 테스트입니다.
 * OkHttp MockWebServer를 사용하여 토스 API 서버를 모의합니다.
 *
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
class TossPaymentClientTest {

    private MockWebServer mockWebServer;
    private TossPaymentClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SECRET_KEY = "test_sk_abc123";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        // Remove trailing slash to match typical baseUrl format
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        TossPaymentProperties properties = new TossPaymentProperties(SECRET_KEY, "test_ck_client", baseUrl);
        client = new TossPaymentClient(properties, objectMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // --- 결제 승인 (confirmPayment) ---

    @Test
    void confirmPayment_성공_시_TossConfirmResponse를_반환한다() throws Exception {
        // given
        String responseBody = """
                {
                    "paymentKey": "tgen_20250101ABCDE",
                    "orderId": "order_42",
                    "status": "DONE",
                    "totalAmount": 7500,
                    "method": "카드",
                    "approvedAt": "2025-07-01T18:31:00+09:00"
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        // when
        TossConfirmResponse response = client.confirmPayment("tgen_20250101ABCDE", "order_42", 7500, "idem-key-1");

        // then
        assertThat(response.paymentKey()).isEqualTo("tgen_20250101ABCDE");
        assertThat(response.orderId()).isEqualTo("order_42");
        assertThat(response.status()).isEqualTo("DONE");
        assertThat(response.totalAmount()).isEqualTo(7500);
        assertThat(response.method()).isEqualTo("카드");
        assertThat(response.approvedAt()).isEqualTo("2025-07-01T18:31:00+09:00");

        // 요청 검증
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/payments/confirm");
        assertThat(request.getHeader("Content-Type")).contains("application/json");
    }

    @Test
    void confirmPayment_실패_시_TossPaymentException을_발생시킨다() {
        // given
        String errorBody = """
                {
                    "code": "ALREADY_PROCESSED_PAYMENT",
                    "message": "이미 처리된 결제입니다."
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(errorBody));

        // when & then
        assertThatThrownBy(() -> client.confirmPayment("tgen_key", "order_1", 5000, "idem-key-2"))
                .isInstanceOf(TossPaymentException.class)
                .satisfies(ex -> {
                    TossPaymentException tpe = (TossPaymentException) ex;
                    assertThat(tpe.getErrorCode()).isEqualTo("ALREADY_PROCESSED_PAYMENT");
                    assertThat(tpe.getErrorMessage()).isEqualTo("이미 처리된 결제입니다.");
                    assertThat(tpe.getHttpStatus()).isEqualTo(400);
                });
    }

    // --- 결제 취소 (cancelPayment) ---

    @Test
    void cancelPayment_성공_시_TossCancelResponse를_반환한다() throws Exception {
        // given
        String responseBody = """
                {
                    "paymentKey": "tgen_20250101ABCDE",
                    "orderId": "order_42",
                    "status": "CANCELED",
                    "cancels": [
                        {
                            "cancelAmount": 7500,
                            "cancelReason": "고객 요청",
                            "canceledAt": "2025-07-01T19:00:00+09:00"
                        }
                    ]
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        // when
        TossCancelResponse response = client.cancelPayment("tgen_20250101ABCDE", "고객 요청", 7500);

        // then
        assertThat(response.paymentKey()).isEqualTo("tgen_20250101ABCDE");
        assertThat(response.orderId()).isEqualTo("order_42");
        assertThat(response.status()).isEqualTo("CANCELED");
        assertThat(response.cancels()).hasSize(1);
        assertThat(response.cancels().get(0).cancelAmount()).isEqualTo(7500);
        assertThat(response.cancels().get(0).cancelReason()).isEqualTo("고객 요청");

        // 요청 검증
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/payments/tgen_20250101ABCDE/cancel");
        assertThat(request.getHeader("Content-Type")).contains("application/json");
    }

    @Test
    void cancelPayment_실패_시_TossPaymentException을_발생시킨다() {
        // given
        String errorBody = """
                {
                    "code": "ALREADY_CANCELED_PAYMENT",
                    "message": "이미 취소된 결제입니다."
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(errorBody));

        // when & then
        assertThatThrownBy(() -> client.cancelPayment("tgen_key", "고객 요청", 5000))
                .isInstanceOf(TossPaymentException.class)
                .satisfies(ex -> {
                    TossPaymentException tpe = (TossPaymentException) ex;
                    assertThat(tpe.getErrorCode()).isEqualTo("ALREADY_CANCELED_PAYMENT");
                    assertThat(tpe.getErrorMessage()).isEqualTo("이미 취소된 결제입니다.");
                    assertThat(tpe.getHttpStatus()).isEqualTo(400);
                });
    }

    // --- 인증 헤더 검증 ---

    @Test
    void Authorization_헤더가_Basic_Base64_형식으로_전송된다() throws Exception {
        // given
        String responseBody = """
                {
                    "paymentKey": "key",
                    "orderId": "order_1",
                    "status": "DONE",
                    "totalAmount": 1000,
                    "method": "카드",
                    "approvedAt": "2025-07-01T18:31:00+09:00"
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        // when
        client.confirmPayment("key", "order_1", 1000, "idem-key-3");

        // then
        RecordedRequest request = mockWebServer.takeRequest();
        String authHeader = request.getHeader("Authorization");
        assertThat(authHeader).isNotNull();
        assertThat(authHeader).startsWith("Basic ");

        // Base64 디코딩하여 {secretKey}: 형식인지 검증
        String encoded = authHeader.substring("Basic ".length());
        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        assertThat(decoded).isEqualTo(SECRET_KEY + ":");
    }

    // --- 5xx 서버 에러 ---

    @Test
    void 서버_에러_5xx_응답_시_TossPaymentException을_발생시킨다() {
        // given
        String errorBody = """
                {
                    "code": "PROVIDER_ERROR",
                    "message": "일시적인 오류가 발생했습니다."
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody(errorBody));

        // when & then
        assertThatThrownBy(() -> client.confirmPayment("key", "order_1", 1000, "idem-key-4"))
                .isInstanceOf(TossPaymentException.class)
                .satisfies(ex -> {
                    TossPaymentException tpe = (TossPaymentException) ex;
                    assertThat(tpe.getErrorCode()).isEqualTo("PROVIDER_ERROR");
                    assertThat(tpe.getErrorMessage()).isEqualTo("일시적인 오류가 발생했습니다.");
                    assertThat(tpe.getHttpStatus()).isEqualTo(500);
                });
    }
}
