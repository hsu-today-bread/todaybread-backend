package com.todaybread.server.domain.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaybread.server.domain.payment.client.dto.TossCancelRequest;
import com.todaybread.server.domain.payment.client.dto.TossCancelResponse;
import com.todaybread.server.domain.payment.client.dto.TossConfirmRequest;
import com.todaybread.server.domain.payment.client.dto.TossConfirmResponse;
import com.todaybread.server.domain.payment.client.dto.TossErrorResponse;
import com.todaybread.server.domain.payment.client.dto.TossPaymentResponse;
import com.todaybread.server.domain.payment.config.TossPaymentProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 토스 페이먼츠 REST API와의 HTTP 통신을 전담하는 클라이언트입니다.
 * Spring 6 RestClient를 사용하여 결제 승인 및 취소 API를 호출합니다.
 *
 * <p>인증: Secret Key를 {@code {secretKey}:} 형식으로 Base64 인코딩하여
 * {@code Authorization: Basic {encoded}} 헤더로 전송합니다.</p>
 *
 * <p>타임아웃: 연결 5초, 읽기 5초로 설정합니다.</p>
 *
 * <p>에러 처리: HTTP 4xx/5xx 응답 시 응답 본문의 code, message를 포함한
 * {@link TossPaymentException}을 발생시킵니다.</p>
 */
@Slf4j
@Component
@Profile("!stub")
public class TossPaymentClient {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 5_000;

    private final RestClient restClient;
    private final TossPaymentProperties properties;
    private final ObjectMapper objectMapper;

    public TossPaymentClient(TossPaymentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        String authorization = buildAuthorizationHeader(properties.secretKey());

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", authorization)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    byte[] body = response.getBody().readAllBytes();
                    TossErrorResponse errorResponse;
                    try {
                        errorResponse = objectMapper.readValue(body, TossErrorResponse.class);
                    } catch (Exception e) {
                        throw new TossPaymentException(
                                "UNKNOWN_ERROR",
                                new String(body, StandardCharsets.UTF_8),
                                response.getStatusCode().value()
                        );
                    }
                    throw new TossPaymentException(
                            errorResponse.code(),
                            errorResponse.message(),
                            response.getStatusCode().value()
                    );
                })
                .build();
    }

    /**
     * Secret Key가 미설정된 경우 애플리케이션 시작 시 경고 메시지를 출력합니다.
     */
    @PostConstruct
    void validateSecretKey() {
        if (properties.secretKey() == null || properties.secretKey().isBlank()) {
            log.warn("⚠️ 토스 페이먼츠 Secret Key가 설정되지 않았습니다. "
                    + "환경 변수 TOSS_SECRET_KEY를 설정해주세요. "
                    + "결제 API 호출 시 인증 오류가 발생합니다.");
        }
    }

    /**
     * 토스 페이먼츠 결제 승인 API를 호출합니다.
     * POST /v1/payments/confirm
     *
     * @param paymentKey     토스 페이먼츠 결제 고유 키
     * @param orderId        주문 ID
     * @param amount         결제 금액
     * @param idempotencyKey 멱등성 키 (Idempotency-Key 헤더로 전달)
     * @return 결제 승인 응답
     * @throws TossPaymentException 토스 API가 4xx/5xx 응답을 반환한 경우
     */
    public TossConfirmResponse confirmPayment(String paymentKey, String orderId, int amount,
                                               String idempotencyKey) {
        TossConfirmRequest request = new TossConfirmRequest(paymentKey, orderId, amount);

        return restClient.post()
                .uri("/v1/payments/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .retrieve()
                .body(TossConfirmResponse.class);
    }

    /**
     * 토스 페이먼츠 결제 취소 API를 호출합니다.
     * POST /v1/payments/{paymentKey}/cancel
     * paymentKey를 멱등성 키로 사용합니다.
     *
     * @param paymentKey   토스 페이먼츠 결제 고유 키
     * @param cancelReason 취소 사유
     * @param cancelAmount 취소 금액
     * @return 결제 취소 응답
     * @throws TossPaymentException 토스 API가 4xx/5xx 응답을 반환한 경우
     */
    public TossCancelResponse cancelPayment(String paymentKey, String cancelReason, int cancelAmount) {
        TossCancelRequest request = new TossCancelRequest(cancelReason, cancelAmount);

        return restClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", paymentKey)
                .body(request)
                .retrieve()
                .body(TossCancelResponse.class);
    }

    /**
     * 토스 페이먼츠 결제 조회 API를 호출합니다.
     * GET /v1/payments/{paymentKey}
     *
     * @param paymentKey 토스 페이먼츠 결제 고유 키
     * @return 결제 조회 응답
     * @throws TossPaymentException 토스 API가 4xx/5xx 응답을 반환한 경우
     */
    public TossPaymentResponse getPayment(String paymentKey) {
        return restClient.get()
                .uri("/v1/payments/{paymentKey}", paymentKey)
                .retrieve()
                .body(TossPaymentResponse.class);
    }

    /**
     * Secret Key를 Base64 인코딩하여 Authorization 헤더 값을 생성합니다.
     * 토스 페이먼츠 인증 방식: {@code {secretKey}:} 형식을 Base64 인코딩합니다.
     *
     * @param secretKey 토스 페이먼츠 Secret Key
     * @return {@code Basic {encoded}} 형식의 Authorization 헤더 값
     */
    static String buildAuthorizationHeader(String secretKey) {
        String credentials = (secretKey != null ? secretKey : "") + ":";
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
