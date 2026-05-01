package com.todaybread.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 HTTP 요청을 위한 RestTemplate 설정 클래스입니다.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate Bean을 생성합니다.
     * 연결 타임아웃 3초, 읽기 타임아웃 5초로 설정됩니다.
     *
     * @return RestTemplate 객체
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }
}