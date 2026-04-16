package com.todaybread.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 설정입니다.
 * 테스트 환경에서 스케줄러를 비활성화할 수 있도록 조건부로 활성화합니다.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "order.expiry.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
