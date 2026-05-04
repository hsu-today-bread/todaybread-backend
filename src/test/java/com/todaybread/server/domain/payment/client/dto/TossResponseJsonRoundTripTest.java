package com.todaybread.server.domain.payment.client.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Feature: toss-payment-integration, Property 4: 토스 API 응답 JSON 직렬화 라운드트립

/**
 * Property 4: 토스 API 응답 JSON 직렬화 라운드트립
 *
 * 임의의 TossConfirmResponse, TossCancelResponse, TossErrorResponse 객체에 대해,
 * JSON으로 직렬화한 뒤 다시 역직렬화하면 원래 객체와 동일한 데이터를 포함해야 한다.
 *
 * **Validates: Requirements 4.6, 4.7**
 */
@Tag("Feature: toss-payment-integration, Property 4: 토스 API 응답 JSON 직렬화 라운드트립")
class TossResponseJsonRoundTripTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * TossConfirmResponse를 JSON으로 직렬화 → 역직렬화하면 원래 객체와 동일해야 한다.
     *
     * **Validates: Requirements 4.6, 4.7**
     */
    @Property(tries = 100)
    void tossConfirmResponse_jsonRoundTrip(
            @ForAll("confirmResponses") TossConfirmResponse original
    ) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(original);
        TossConfirmResponse deserialized = objectMapper.readValue(json, TossConfirmResponse.class);

        assertThat(deserialized).isEqualTo(original);
    }

    /**
     * TossCancelResponse를 JSON으로 직렬화 → 역직렬화하면 원래 객체와 동일해야 한다.
     *
     * **Validates: Requirements 4.6, 4.7**
     */
    @Property(tries = 100)
    void tossCancelResponse_jsonRoundTrip(
            @ForAll("cancelResponses") TossCancelResponse original
    ) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(original);
        TossCancelResponse deserialized = objectMapper.readValue(json, TossCancelResponse.class);

        assertThat(deserialized).isEqualTo(original);
    }

    /**
     * TossErrorResponse를 JSON으로 직렬화 → 역직렬화하면 원래 객체와 동일해야 한다.
     *
     * **Validates: Requirements 4.6, 4.7**
     */
    @Property(tries = 100)
    void tossErrorResponse_jsonRoundTrip(
            @ForAll("errorResponses") TossErrorResponse original
    ) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(original);
        TossErrorResponse deserialized = objectMapper.readValue(json, TossErrorResponse.class);

        assertThat(deserialized).isEqualTo(original);
    }

    @Provide
    Arbitrary<TossConfirmResponse> confirmResponses() {
        Arbitrary<String> paymentKeys = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50)
                .map(s -> "tgen_" + s);
        Arbitrary<String> orderIds = Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(30)
                .map(s -> "order_" + s);
        Arbitrary<String> statuses = Arbitraries.of("DONE", "WAITING_FOR_DEPOSIT", "IN_PROGRESS");
        Arbitrary<Integer> amounts = Arbitraries.integers().between(1, 10_000_000);
        Arbitrary<String> methods = Arbitraries.of("카드", "간편결제", "가상계좌", "계좌이체", "휴대폰");
        Arbitrary<String> approvedAts = Arbitraries.integers().between(2020, 2030).flatMap(year ->
                Arbitraries.integers().between(1, 12).flatMap(month ->
                        Arbitraries.integers().between(1, 28).map(day ->
                                String.format("%d-%02d-%02dT10:30:00+09:00", year, month, day)
                        )
                )
        );

        return Combinators.combine(paymentKeys, orderIds, statuses, amounts, methods, approvedAts)
                .as(TossConfirmResponse::new);
    }

    @Provide
    Arbitrary<TossCancelResponse> cancelResponses() {
        Arbitrary<String> paymentKeys = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50)
                .map(s -> "tgen_" + s);
        Arbitrary<String> orderIds = Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(30)
                .map(s -> "order_" + s);
        Arbitrary<String> statuses = Arbitraries.of("CANCELED", "PARTIAL_CANCELED");
        Arbitrary<List<TossCancelDetail>> cancelLists = cancelDetails().list().ofMinSize(1).ofMaxSize(3);

        return Combinators.combine(paymentKeys, orderIds, statuses, cancelLists)
                .as(TossCancelResponse::new);
    }

    @Provide
    Arbitrary<TossErrorResponse> errorResponses() {
        Arbitrary<String> codes = Arbitraries.of(
                "ALREADY_PROCESSED_PAYMENT",
                "PROVIDER_ERROR",
                "INVALID_CARD_COMPANY",
                "INVALID_STOPPED_CARD",
                "EXCEED_MAX_CARD_INSTALLMENT_PLAN",
                "NOT_FOUND_PAYMENT",
                "INVALID_REQUEST"
        );
        Arbitrary<String> messages = Arbitraries.of(
                "이미 처리된 결제입니다.",
                "일시적인 오류가 발생했습니다.",
                "유효하지 않은 카드사입니다.",
                "정지된 카드입니다.",
                "할부 개월 수가 초과되었습니다.",
                "결제를 찾을 수 없습니다.",
                "잘못된 요청입니다."
        );

        return Combinators.combine(codes, messages).as(TossErrorResponse::new);
    }

    private Arbitrary<TossCancelDetail> cancelDetails() {
        Arbitrary<Integer> cancelAmounts = Arbitraries.integers().between(1, 10_000_000);
        Arbitrary<String> cancelReasons = Arbitraries.of("고객 요청", "상품 불량", "배송 지연", "단순 변심");
        Arbitrary<String> canceledAts = Arbitraries.integers().between(2020, 2030).flatMap(year ->
                Arbitraries.integers().between(1, 12).flatMap(month ->
                        Arbitraries.integers().between(1, 28).map(day ->
                                String.format("%d-%02d-%02dT19:00:00+09:00", year, month, day)
                        )
                )
        );

        return Combinators.combine(cancelAmounts, cancelReasons, canceledAts)
                .as(TossCancelDetail::new);
    }
}
