package com.todaybread.server.domain.order.service;

import com.todaybread.server.support.TestFixtures;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    public Clock fixedClock() {
        return TestFixtures.FIXED_CLOCK;
    }
}
