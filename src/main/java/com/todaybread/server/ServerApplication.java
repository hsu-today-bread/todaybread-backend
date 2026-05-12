package com.todaybread.server;

import com.todaybread.server.domain.order.config.OrderExpiryProperties;
import com.todaybread.server.domain.payment.config.TossPaymentProperties;
import com.todaybread.server.domain.user.config.NtsBusinessProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * TodayBread 서버 애플리케이션 진입점입니다.
 */
@SpringBootApplication
@EnableConfigurationProperties({OrderExpiryProperties.class, TossPaymentProperties.class, NtsBusinessProperties.class})
public class ServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);
	}

	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}
}
