package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Property 1: 주문 번호 형식
 * Property 2: 주문 번호 유일성 (재시도 포함)
 * Unit: 10회 모두 충돌 시 예외 발생
 */
@Tag("Feature: boss-order-management")
class OrderNumberGeneratorTest {

    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Set<Character> ALLOWED_CHARS;

    static {
        ALLOWED_CHARS = new java.util.HashSet<>();
        for (char c : CHARSET.toCharArray()) {
            ALLOWED_CHARS.add(c);
        }
    }

    @Mock
    private OrderRepository orderRepository;

    private OrderNumberGenerator orderNumberGenerator;

    @BeforeProperty
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderNumberGenerator = new OrderNumberGenerator(orderRepository);
    }

    @BeforeEach
    void setUpUnit() {
        MockitoAnnotations.openMocks(this);
        orderNumberGenerator = new OrderNumberGenerator(orderRepository);
    }

    /**
     * Property 1: 주문 번호 형식
     *
     * 임의의 storeId와 orderDate에 대해 생성된 주문 번호가
     * 정확히 4자리이고 허용된 문자셋(32자)에만 포함되는지 검증
     *
     * **Validates: Requirements 1.1, 1.2**
     */
    @Property(tries = 100)
    @Tag("Property 1: 주문 번호 형식")
    void generate_returnsExactly4CharsFromAllowedCharset(
            @ForAll("storeIds") Long storeId,
            @ForAll("orderDates") LocalDate orderDate
    ) {
        reset(orderRepository);
        given(orderRepository.existsByStoreIdAndOrderDateAndOrderNumber(
                eq(storeId), eq(orderDate), anyString()))
                .willReturn(false);

        String orderNumber = orderNumberGenerator.generate(storeId, orderDate);

        assertThat(orderNumber).hasSize(4);
        for (char c : orderNumber.toCharArray()) {
            assertThat(ALLOWED_CHARS).contains(c);
        }
    }

    /**
     * Property 2: 주문 번호 유일성 (재시도 포함)
     *
     * 이미 존재하는 주문 번호가 K개(K < MAX_RETRIES=10)일 때,
     * mock에서 처음 K번은 true, 그 이후 false를 반환하면
     * 반환된 주문 번호가 기존 번호와 다른 유일한 값인지 검증
     *
     * **Validates: Requirements 1.3, 1.4**
     */
    @Property(tries = 100)
    @Tag("Property 2: 주문 번호 유일성")
    void generate_retriesAndReturnsUniqueNumber(
            @ForAll("storeIds") Long storeId,
            @ForAll("orderDates") LocalDate orderDate,
            @ForAll("retryCountsBeforeSuccess") int k
    ) {
        reset(orderRepository);

        AtomicInteger callCount = new AtomicInteger(0);
        given(orderRepository.existsByStoreIdAndOrderDateAndOrderNumber(
                eq(storeId), eq(orderDate), anyString()))
                .willAnswer(invocation -> callCount.getAndIncrement() < k);

        String orderNumber = orderNumberGenerator.generate(storeId, orderDate);

        // The returned number should be valid format
        assertThat(orderNumber).hasSize(4);
        for (char c : orderNumber.toCharArray()) {
            assertThat(ALLOWED_CHARS).contains(c);
        }
    }

    /**
     * 10회 모두 충돌 시 ORDER_NUMBER_GENERATION_FAILED 예외가 발생하는지 검증합니다.
     */
    @Test
    void generate_throwsExceptionWhenAllRetriesExhausted() {
        Long storeId = 1L;
        LocalDate orderDate = LocalDate.of(2026, 4, 5);

        given(orderRepository.existsByStoreIdAndOrderDateAndOrderNumber(
                eq(storeId), eq(orderDate), anyString()))
                .willReturn(true);

        assertThatThrownBy(() -> orderNumberGenerator.generate(storeId, orderDate))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NUMBER_GENERATION_FAILED);

        verify(orderRepository, times(10))
                .existsByStoreIdAndOrderDateAndOrderNumber(eq(storeId), eq(orderDate), anyString());
    }

    @Provide
    Arbitrary<Long> storeIds() {
        return Arbitraries.longs().between(1, 10000);
    }

    @Provide
    Arbitrary<LocalDate> orderDates() {
        return Arbitraries.integers().between(2024, 2027).flatMap(year ->
                Arbitraries.integers().between(1, 12).flatMap(month ->
                        Arbitraries.integers().between(1, 28).map(day ->
                                LocalDate.of(year, month, day)
                        )
                )
        );
    }

    @Provide
    Arbitrary<Integer> retryCountsBeforeSuccess() {
        return Arbitraries.integers().between(0, 9);
    }
}
