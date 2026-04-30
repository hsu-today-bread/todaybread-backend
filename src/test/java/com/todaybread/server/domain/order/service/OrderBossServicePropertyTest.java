package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.order.dto.BossOrderResponse;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;

/**
 * Property 3: 주문내역 조회 필터링 및 정렬
 * Property 4: 픽업 완료 상태 전환
 * Property 6: 가게 간 데이터 격리
 */
@Tag("Feature: boss-order-management")
class OrderBossServicePropertyTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private StoreRepository storeRepository;

    private OrderBossService orderBossService;

    @BeforeProperty
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderBossService = new OrderBossService(orderRepository, orderItemRepository, storeRepository);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Property 3: 주문내역 조회 필터링 및 정렬
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Property 3: 주문내역 조회 필터링 및 정렬
     *
     * 다양한 상태의 주문 목록에서 CONFIRMED만 반환되고,
     * 필수 필드가 존재하며, createdAt 내림차순 정렬인지 검증
     *
     * **Validates: Requirements 2.1, 2.2, 2.3**
     */
    @Property(tries = 100)
    @Tag("Property 3: 주문내역 조회 필터링 및 정렬")
    void getConfirmedOrders_returnsOnlyConfirmedOrdersSortedByCreatedAtDesc(
            @ForAll("orderListsWithMixedStatuses") OrderListTestData testData
    ) {
        reset(orderRepository, orderItemRepository, storeRepository);

        Long userId = testData.userId;
        Long storeId = testData.storeId;

        // Mock store lookup
        StoreEntity store = buildStore(storeId, userId);
        given(storeRepository.findByUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(store));

        // Filter CONFIRMED orders and sort by createdAt DESC (simulating repository behavior)
        List<OrderEntity> confirmedOrders = testData.allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED)
                .sorted(Comparator.comparing(OrderEntity::getCreatedAt).reversed())
                .collect(Collectors.toList());

        given(orderRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(storeId, OrderStatus.CONFIRMED))
                .willReturn(confirmedOrders);

        // Mock order items for each confirmed order (batch)
        List<OrderItemEntity> allItems = confirmedOrders.stream().map(order -> {
            OrderItemEntity item = OrderItemEntity.builder()
                    .orderId(order.getId())
                    .breadId(1L)
                    .breadName("빵-" + order.getId())
                    .breadPrice(1000)
                    .quantity(1)
                    .build();
            ReflectionTestUtils.setField(item, "id", order.getId() * 100);
            return item;
        }).collect(Collectors.toList());

        List<Long> confirmedOrderIds = confirmedOrders.stream()
                .map(OrderEntity::getId)
                .toList();
        given(orderItemRepository.findByOrderIdIn(confirmedOrderIds)).willReturn(allItems);

        // Act
        List<BossOrderResponse> result = orderBossService.getConfirmedOrders(userId);

        // Assert: all returned orders have required fields
        assertThat(result).allSatisfy(response -> {
            assertThat(response.orderId()).isNotNull();
            assertThat(response.totalAmount()).isGreaterThanOrEqualTo(0);
            assertThat(response.items()).isNotEmpty();
        });

        // Assert: result count matches confirmed orders count
        assertThat(result).hasSize(confirmedOrders.size());

        // Assert: createdAt is in descending order
        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i).createdAt())
                    .isBeforeOrEqualTo(result.get(i - 1).createdAt());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Property 4: 픽업 완료 상태 전환
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Property 4: 픽업 완료 상태 전환
     *
     * CONFIRMED 상태 주문에 대해 pickupOrder() 호출 시 PICKED_UP으로 변경되는지 검증
     *
     * **Validates: Requirements 3.1**
     */
    @Property(tries = 100)
    @Tag("Property 4: 픽업 완료 상태 전환")
    void pickupOrder_changesConfirmedOrderToPickedUp(
            @ForAll("confirmedOrderData") ConfirmedOrderTestData testData
    ) {
        reset(orderRepository, storeRepository);

        // Reset mutable state for jqwik shrinking
        ReflectionTestUtils.setField(testData.order, "status", OrderStatus.CONFIRMED);

        Long userId = testData.userId;
        Long storeId = testData.storeId;
        Long orderId = testData.order.getId();

        StoreEntity store = buildStore(storeId, userId);
        given(storeRepository.findByUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(store));
        given(orderRepository.findByIdWithLock(orderId)).willReturn(Optional.of(testData.order));

        // Act
        orderBossService.pickupOrder(userId, orderId);

        // Assert: status changed to PICKED_UP
        assertThat(testData.order.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Property 6: 가게 간 데이터 격리
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Property 6: 가게 간 데이터 격리
     *
     * storeA 사장님이 storeB 주문에 pickupOrder() 시도 시 ORDER_ACCESS_DENIED 예외 확인
     *
     * **Validates: Requirements 3.4, 6.3**
     */
    @Property(tries = 100)
    @Tag("Property 6: 가게 간 데이터 격리")
    void pickupOrder_deniesAccessToOtherStoreOrder(
            @ForAll("crossStoreData") CrossStoreTestData testData
    ) {
        reset(orderRepository, storeRepository);

        Long userA = testData.userIdA;
        Long storeA = testData.storeIdA;
        Long storeB = testData.storeIdB;
        Long orderId = testData.orderOfStoreB.getId();

        StoreEntity storeEntityA = buildStore(storeA, userA);
        given(storeRepository.findByUserIdAndIsActiveTrue(userA)).willReturn(Optional.of(storeEntityA));
        given(orderRepository.findByIdWithLock(orderId)).willReturn(Optional.of(testData.orderOfStoreB));

        // Act & Assert: storeA boss cannot access storeB order
        assertThatThrownBy(() -> orderBossService.pickupOrder(userA, orderId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<OrderListTestData> orderListsWithMixedStatuses() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 1000),   // userId
                Arbitraries.longs().between(1, 500),    // storeId
                Arbitraries.integers().between(1, 10)   // order count
        ).flatAs((userId, storeId, count) -> {
            Arbitrary<OrderEntity> orderArb = Combinators.combine(
                    Arbitraries.longs().between(1, 100000),
                    Arbitraries.of(OrderStatus.values()),
                    Arbitraries.integers().between(100, 50000),
                    Arbitraries.longs().between(0, 1440)  // minutes offset for createdAt
            ).as((id, status, amount, minutesOffset) -> {
                OrderEntity order = OrderEntity.builder()
                        .userId(10L)
                        .storeId(storeId)
                        .status(status)
                        .totalAmount(amount)
                        .build();
                ReflectionTestUtils.setField(order, "id", id);
                ReflectionTestUtils.setField(order, "createdAt",
                        LocalDateTime.of(2026, 4, 5, 0, 0).plusMinutes(minutesOffset));
                return order;
            });

            return orderArb.list().ofSize(count)
                    .map(orders -> {
                        // Ensure unique IDs
                        List<OrderEntity> uniqueOrders = orders.stream()
                                .collect(Collectors.toMap(OrderEntity::getId, o -> o, (a, b) -> a))
                                .values().stream().collect(Collectors.toList());
                        return new OrderListTestData(userId, storeId, uniqueOrders);
                    });
        });
    }

    @Provide
    Arbitrary<ConfirmedOrderTestData> confirmedOrderData() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 1000),   // userId
                Arbitraries.longs().between(1, 500),    // storeId
                Arbitraries.longs().between(1, 100000), // orderId
                Arbitraries.integers().between(100, 50000) // totalAmount
        ).as((userId, storeId, orderId, amount) -> {
            OrderEntity order = OrderEntity.builder()
                    .userId(10L)
                    .storeId(storeId)
                    .status(OrderStatus.CONFIRMED)
                    .totalAmount(amount)
                    .build();
            ReflectionTestUtils.setField(order, "id", orderId);
            return new ConfirmedOrderTestData(userId, storeId, order);
        });
    }

    @Provide
    Arbitrary<CrossStoreTestData> crossStoreData() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 1000),    // userIdA
                Arbitraries.longs().between(1, 500),     // storeIdA
                Arbitraries.longs().between(501, 1000),  // storeIdB (different from storeA)
                Arbitraries.longs().between(1, 100000),  // orderId
                Arbitraries.integers().between(100, 50000) // totalAmount
        ).as((userIdA, storeIdA, storeIdB, orderId, amount) -> {
            OrderEntity orderOfStoreB = OrderEntity.builder()
                    .userId(999L)
                    .storeId(storeIdB)
                    .status(OrderStatus.CONFIRMED)
                    .totalAmount(amount)
                    .build();
            ReflectionTestUtils.setField(orderOfStoreB, "id", orderId);
            return new CrossStoreTestData(userIdA, storeIdA, storeIdB, orderOfStoreB);
        });
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test data classes
    // ──────────────────────────────────────────────────────────────────────

    static class OrderListTestData {
        final Long userId;
        final Long storeId;
        final List<OrderEntity> allOrders;

        OrderListTestData(Long userId, Long storeId, List<OrderEntity> allOrders) {
            this.userId = userId;
            this.storeId = storeId;
            this.allOrders = allOrders;
        }
    }

    static class ConfirmedOrderTestData {
        final Long userId;
        final Long storeId;
        final OrderEntity order;

        ConfirmedOrderTestData(Long userId, Long storeId, OrderEntity order) {
            this.userId = userId;
            this.storeId = storeId;
            this.order = order;
        }
    }

    static class CrossStoreTestData {
        final Long userIdA;
        final Long storeIdA;
        final Long storeIdB;
        final OrderEntity orderOfStoreB;

        CrossStoreTestData(Long userIdA, Long storeIdA, Long storeIdB, OrderEntity orderOfStoreB) {
            this.userIdA = userIdA;
            this.storeIdA = storeIdA;
            this.storeIdB = storeIdB;
            this.orderOfStoreB = orderOfStoreB;
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────────────────

    private StoreEntity buildStore(Long storeId, Long userId) {
        StoreEntity store = StoreEntity.builder()
                .userId(userId)
                .name("store-" + storeId)
                .phoneNumber("02-1234-" + String.format("%04d", storeId.intValue() % 10000))
                .description("desc")
                .addressLine1("addr1")
                .addressLine2("addr2")
                .latitude(BigDecimal.valueOf(37.5))
                .longitude(BigDecimal.valueOf(127.0))
                .build();
        ReflectionTestUtils.setField(store, "id", storeId);
        return store;
    }
}
