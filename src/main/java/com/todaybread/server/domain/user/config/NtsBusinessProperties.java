package com.todaybread.server.domain.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 국세청 사업자등록정보 API 연동 설정입니다.
 *
 * @param serviceKey       공공데이터포털 인증키
 * @param baseUrl          국세청 사업자등록정보 API 기본 URL
 * @param connectTimeoutMs 연결 타임아웃(ms)
 * @param readTimeoutMs    읽기 타임아웃(ms)
 */
@ConfigurationProperties(prefix = "nts.business")
public record NtsBusinessProperties(
        String serviceKey,
        String baseUrl,
        Integer connectTimeoutMs,
        Integer readTimeoutMs
) {
    public int connectTimeoutOrDefault() {
        return connectTimeoutMs == null || connectTimeoutMs <= 0 ? 3_000 : connectTimeoutMs;
    }

    public int readTimeoutOrDefault() {
        return readTimeoutMs == null || readTimeoutMs <= 0 ? 5_000 : readTimeoutMs;
    }
}
