package com.todaybread.server.domain.order.entity;

import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import net.jqwik.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property 5: 상태 전이 규칙
 *
 * For any OrderStatus 값의 (현재상태, 목표상태) 조합에 대해,
 * updateStatus()는 허용된 전환(PENDING→CONFIRMED, PENDING→CANCELLED, CONFIRMED→PICKED_UP)만 성공하고,
 * 그 외의 모든 전환에서는 ORDER_STATUS_CANNOT_CHANGE 예외를 발생시켜야 한다.
 *
 * **Validates: Requirements 3.5, 3.7, 7.4**
 */
@Tag("Feature: boss-order-management, Property 5: 상태 전이 규칙")
class OrderEntityStatusTransitionTest {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PICKED_UP, OrderStatus.CANCELLED, OrderStatus.CANCEL_PENDING),
            OrderStatus.CANCEL_PENDING, Set.of(OrderStatus.CANCELLED, OrderStatus.CONFIRMED)
    );

    /**
     * Property 5: 모든 (현재상태, 목표상태) 조합에 대해 허용된 전환만 성공하고 나머지는 예외를 발생시킨다.
     *
     * **Validates: Requirements 3.5, 3.7, 7.4**
     */
    @Property(tries = 100)
    void updateStatus_allowsOnlyValidTransitions(
            @ForAll("allStatuses") OrderStatus currentStatus,
            @ForAll("allStatuses") OrderStatus targetStatus
    ) {
        OrderEntity order = OrderEntity.builder()
                .userId(1L)
                .storeId(1L)
                .status(currentStatus)
                .totalAmount(1000)
                .build();
        ReflectionTestUtils.setField(order, "id", 1L);

        boolean isAllowed = ALLOWED_TRANSITIONS
                .getOrDefault(currentStatus, Set.of())
                .contains(targetStatus);

        if (isAllowed) {
            order.updateStatus(targetStatus);
            assertThat(order.getStatus()).isEqualTo(targetStatus);
        } else {
            assertThatThrownBy(() -> order.updateStatus(targetStatus))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_STATUS_CANNOT_CHANGE);
        }
    }

    @Provide
    Arbitrary<OrderStatus> allStatuses() {
        return Arbitraries.of(OrderStatus.values());
    }
}
