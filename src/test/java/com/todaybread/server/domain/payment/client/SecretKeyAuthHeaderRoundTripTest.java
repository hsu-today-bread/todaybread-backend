package com.todaybread.server.domain.payment.client;

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.NumericChars;
import net.jqwik.api.constraints.StringLength;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

// Feature: toss-payment-integration, Property 1: Secret Key Authorization 헤더 라운드트립

/**
 * Property 1: Secret Key Authorization 헤더 라운드트립
 *
 * 임의의 Secret Key 문자열에 대해 TossPaymentClient.buildAuthorizationHeader()가
 * 생성하는 Authorization 헤더를 검증합니다.
 *
 * 1. Authorization 헤더는 "Basic " 접두사를 가져야 합니다.
 * 2. "Basic " 이후의 Base64 부분을 디코딩하면 "{secretKey}:" 형식의 원래 값을 복원할 수 있어야 합니다.
 *
 * <b>Validates: Requirements 1.4</b>
 */
class SecretKeyAuthHeaderRoundTripTest {

    /**
     * **Validates: Requirements 1.4**
     *
     * 임의의 영숫자 Secret Key에 대해:
     * 1. 헤더가 "Basic "으로 시작하는지 검증
     * 2. "Basic " 이후 Base64 부분을 추출
     * 3. Base64 디코딩
     * 4. 디코딩된 값이 "{secretKey}:"와 일치하는지 검증
     */
    @Property(tries = 100)
    void secretKeyAuthorizationHeaderRoundTrip(
            @ForAll @AlphaChars @StringLength(min = 1, max = 100) String secretKey) {

        // when: buildAuthorizationHeader로 Authorization 헤더 생성
        String header = TossPaymentClient.buildAuthorizationHeader(secretKey);

        // then 1: 헤더가 "Basic "으로 시작하는지 검증
        assertThat(header).startsWith("Basic ");

        // then 2: "Basic " 이후 Base64 부분 추출
        String base64Part = header.substring("Basic ".length());

        // then 3: Base64 디코딩
        byte[] decodedBytes = Base64.getDecoder().decode(base64Part);
        String decoded = new String(decodedBytes, StandardCharsets.UTF_8);

        // then 4: 디코딩된 값이 "{secretKey}:"와 일치하는지 검증
        assertThat(decoded).isEqualTo(secretKey + ":");
    }
}
