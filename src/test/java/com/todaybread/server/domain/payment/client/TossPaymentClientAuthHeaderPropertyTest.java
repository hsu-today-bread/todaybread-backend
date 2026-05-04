package com.todaybread.server.domain.payment.client;

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

// Feature: toss-payment-integration, Property 1: Secret Key Authorization 헤더 라운드트립

/**
 * Property 1: Secret Key Authorization 헤더 라운드트립
 *
 * 임의의 Secret Key 문자열에 대해 TossPaymentClient.buildAuthorizationHeader()가
 * 생성하는 Authorization 헤더가 올바른 형식인지 검증합니다.
 *
 * - Authorization 헤더는 "Basic " 접두사를 가져야 합니다.
 * - Base64 디코딩하면 "{secretKey}:" 형식의 원래 값을 복원할 수 있어야 합니다.
 *
 * <b>Validates: Requirements 1.4</b>
 */
class TossPaymentClientAuthHeaderPropertyTest {

    /**
     * **Validates: Requirements 1.4**
     *
     * 임의의 영숫자 Secret Key에 대해 Authorization 헤더 라운드트립을 검증합니다.
     * - 헤더가 "Basic " 접두사를 가지는지 확인
     * - Base64 디코딩 결과가 "{secretKey}:" 와 일치하는지 확인
     */
    @Property(tries = 100)
    void authorizationHeaderRoundTrip(
            @ForAll @AlphaChars @StringLength(min = 1, max = 100) String secretKey) {

        // when: buildAuthorizationHeader로 Authorization 헤더 생성
        String header = TossPaymentClient.buildAuthorizationHeader(secretKey);

        // then: "Basic " 접두사 확인
        assertThat(header).startsWith("Basic ");

        // then: Base64 디코딩하여 원래 값 복원 확인
        String encodedPart = header.substring("Basic ".length());
        byte[] decodedBytes = Base64.getDecoder().decode(encodedPart);
        String decoded = new String(decodedBytes, StandardCharsets.UTF_8);

        assertThat(decoded).isEqualTo(secretKey + ":");
    }

    /**
     * **Validates: Requirements 1.4**
     *
     * 다양한 길이와 문자 조합의 Secret Key에 대해 Authorization 헤더 라운드트립을 검증합니다.
     * 토스 API 키 형식(test_sk_..., live_sk_...)을 포함한 다양한 문자열을 테스트합니다.
     */
    @Property(tries = 100)
    void authorizationHeaderRoundTripWithVariousKeys(
            @ForAll("secretKeyProvider") String secretKey) {

        // when: buildAuthorizationHeader로 Authorization 헤더 생성
        String header = TossPaymentClient.buildAuthorizationHeader(secretKey);

        // then: "Basic " 접두사 확인
        assertThat(header).startsWith("Basic ");

        // then: Base64 디코딩하여 원래 값 복원 확인
        String encodedPart = header.substring("Basic ".length());
        byte[] decodedBytes = Base64.getDecoder().decode(encodedPart);
        String decoded = new String(decodedBytes, StandardCharsets.UTF_8);

        assertThat(decoded).isEqualTo(secretKey + ":");
    }

    @Provide
    Arbitrary<String> secretKeyProvider() {
        return Arbitraries.oneOf(
                // 영숫자 문자열 (다양한 길이)
                Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(100),
                // 토스 테스트 키 형식
                Arbitraries.strings().alpha().numeric().ofMinLength(5).ofMaxLength(50)
                        .map(s -> "test_sk_" + s),
                // 토스 라이브 키 형식
                Arbitraries.strings().alpha().numeric().ofMinLength(5).ofMaxLength(50)
                        .map(s -> "live_sk_" + s),
                // 언더스코어 포함 문자열
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .withCharRange('A', 'Z')
                        .withCharRange('0', '9')
                        .withChars('_', '-')
                        .ofMinLength(1).ofMaxLength(80)
        );
    }
}
