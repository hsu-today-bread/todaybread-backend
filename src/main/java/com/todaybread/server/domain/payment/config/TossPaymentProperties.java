package com.todaybread.server.domain.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 토스 페이먼츠 API 연동 설정 프로퍼티입니다.
 * {@code application.properties}의 {@code toss.payment.*} 접두사로 바인딩됩니다.
 *
 * @param secretKey 토스 페이먼츠 Secret Key (서버 인증용, 환경 변수 TOSS_SECRET_KEY)
 * @param clientKey 토스 페이먼츠 Client Key (프론트엔드 SDK용, 환경 변수 TOSS_CLIENT_KEY)
 * @param baseUrl   토스 페이먼츠 API 기본 URL
 */
@ConfigurationProperties(prefix = "toss.payment")
public record TossPaymentProperties(
        String secretKey,
        String clientKey,
        String baseUrl
) {
}
