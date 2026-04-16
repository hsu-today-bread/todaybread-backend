package com.todaybread.server.domain.order.config;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "order.expiry")
public class OrderExpiryProperties {

    @Positive
    private long timeoutMinutes = 10;

    @Positive
    private long schedulerIntervalMs = 60000;

    @Positive
    private int batchSize = 100;

    private boolean schedulerEnabled = true;
}
