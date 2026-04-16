package com.todaybread.server.domain.order.config;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 만료 주문 스케줄러 설정 프로퍼티입니다.
 * {@code application.properties}의 {@code order.expiry.*} 접두사로 바인딩됩니다.
 * 모든 숫자 설정에 {@code @Positive} 검증이 적용되어 시작 시점에 잘못된 값을 차단합니다.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "order.expiry")
public class OrderExpiryProperties {

    /** PENDING 주문이 자동 취소되기까지의 경과 시간 (분). 기본값: 10분 */
    @Positive
    private long timeoutMinutes = 10;

    /** 스케줄러 실행 주기 (밀리초). fixedDelay 방식. 기본값: 60000ms (1분) */
    @Positive
    private long schedulerIntervalMs = 60000;

    /** 한 번의 스케줄러 실행에서 처리할 최대 만료 주문 건수. 기본값: 100 */
    @Positive
    private int batchSize = 100;

    /** 스케줄러 활성화 여부. 테스트나 특정 인스턴스에서 비활성화할 때 사용. 기본값: true */
    private boolean schedulerEnabled = true;
}
