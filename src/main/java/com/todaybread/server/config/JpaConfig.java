package com.todaybread.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정 클래스입니다.
 * @WebMvcTest 등 슬라이스 테스트에서 JPA 관련 빈이 불필요하게 로드되지 않도록
 * 별도 Configuration 클래스로 분리합니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
