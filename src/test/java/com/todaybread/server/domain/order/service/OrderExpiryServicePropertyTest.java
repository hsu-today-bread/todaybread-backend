package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.order.config.OrderExpiryProperties;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

/**
 * Property 1: 만료 대상 조회는 PENDING 상태이면서 기준 시각 이전에 생성된 주문만 반환하며 ID 오름차순으로 정렬된다
 *
 * Validates: Requirements 1.1, 1.3
 */
@Tag("Feature: pending-order-expiry, Property 1: 만료 대상 조회 필터링 및 정렬")
class OrderExpiryServicePropertyTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T03:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, SEOUL);
    private static final long EXPIRY_TIMEOUT_MINUTES = 10;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private BreadRepository breadRepository;

    @Mock
    private OrderExpiryCanceller orderExpiryCanceller;

    private OrderExpiryService orderExpiryService;
    private OrderExpiryCanceller realCanceller;

    @BeforeProperty
    void setUp() {
        MockitoAnnotations.openMocks(this);

        OrderExpiryProperties properties = new OrderExpiryProperties();
        properties.setTimeoutMinutes(EXPIRY_TIMEOUT_MINUTES);
        properties.setBatchSize(100);

        orderExpiryService = new OrderExpiryService(
                orderRepository, orderExpiryCanceller, FIXED_CLOCK, properties
        );

        // Create a real canceller for Properties 2-4 that test cancellation logic directly
        InventoryRestorer inventoryRestorer = new InventoryRestorer(breadRepository);
        realCanceller = new OrderExpiryCanceller(orderRepository, orderItemRepository, inventoryRestorer);
    }

    /**
     * Property 1: 만료 대상 조회는 PENDING 상태이면서 기준 시각 이전에 생성된 주문만 반환하며 ID 오름차순으로 정렬된다
     *
     * **Validates: Requirements 1.1, 1.3**
     */
    @Property(tries = 100)
    void findExpiredPendingOrders_returnsOnlyPendingBeforeCutoff_sortedById(
            @ForAll("orderLists") List<OrderEntity> allOrders
    ) {
        // cutoffTime = now - expiryTimeoutMinutes
        LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
        LocalDateTime cutoffTime = now.minusMinutes(EXPIRY_TIMEOUT_MINUTES);

        // Compute expected result: PENDING + createdAt < cutoffTime, sorted by ID ASC
        List<OrderEntity> expected = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .filter(o -> o.getCreatedAt().isBefore(cutoffTime))
                .sorted(Comparator.comparing(OrderEntity::getId))
                .collect(Collectors.toList());

        // Configure mock to return the expected filtered/sorted list
        given(orderRepository.findExpiredPendingOrders(any(OrderStatus.class), any(LocalDateTime.class), any()))
                .willReturn(expected);

        // Act
        List<OrderEntity> result = orderExpiryService.findExpiredPendingOrders();

        // Assert: all returned orders have PENDING status
        assertThat(result).allSatisfy(order ->
                assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING)
        );

        // Assert: all returned orders have createdAt before cutoffTime
        assertThat(result).allSatisfy(order ->
                assertThat(order.getCreatedAt()).isBefore(cutoffTime)
        );

        // Assert: returned list is sorted by ID ascending
        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i).getId())
                    .isGreaterThan(result.get(i - 1).getId());
        }

        // Assert: result matches expected
        assertThat(result).isEqualTo(expected);
    }

    @Provide
    Arbitrary<List<OrderEntity>> orderLists() {
        Arbitrary<OrderEntity> orderArbitrary = Combinators.combine(
                Arbitraries.longs().between(1, 10000),           // id
                Arbitraries.of(OrderStatus.values()),             // status
                Arbitraries.longs().between(-120, 120)            // minutesOffset from cutoffTime
        ).as((id, status, minutesOffset) -> {
            LocalDateTime cutoffTime = LocalDateTime.now(FIXED_CLOCK).minusMinutes(EXPIRY_TIMEOUT_MINUTES);
            LocalDateTime createdAt = cutoffTime.plusMinutes(minutesOffset);

            OrderEntity order = OrderEntity.builder()
                    .userId(1L)
                    .storeId(1L)
                    .status(status)
                    .totalAmount(1000)
                    .build();
            ReflectionTestUtils.setField(order, "id", id);
            ReflectionTestUtils.setField(order, "createdAt", createdAt);
            return order;
        });

        return orderArbitrary.list().ofMinSize(0).ofMaxSize(20)
                .map(orders -> orders.stream()
                        .collect(Collectors.toMap(OrderEntity::getId, o -> o, (a, b) -> a))
                        .values().stream().collect(Collectors.toList()));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Property 2: PENDING 주문 취소 시 상태가 CANCELLED로 변경된다
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Property 2: PENDING 주문 취소 시 상태가 CANCELLED로 변경된다
     *
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    @Tag("Feature: pending-order-expiry, Property 2: PENDING 주문 취소 시 상태 변경")
    void cancelExpiredOrder_changesPendingOrderStatusToCancelled(
            @ForAll("pendingOrderWithItems") PendingOrderTestData testData
    ) {
        Long orderId = testData.order.getId();

        // Reset mutable state for jqwik shrinking idempotency
        ReflectionTestUtils.setField(testData.order, "status", OrderStatus.PENDING);

        // Mock repository calls
        given(orderRepository.findByIdWithLock(orderId))
                .willReturn(Optional.of(testData.order));
        given(orderItemRepository.findByOrderId(orderId))
                .willReturn(testData.orderItems);

        List<Long> breadIds = testData.orderItems.stream()
                .map(OrderItemEntity::getBreadId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        given(breadRepository.findAllByIdWithLock(breadIds))
                .willReturn(testData.breads);

        // Act
        CancelResult result = realCanceller.cancelExpiredOrder(orderId);

        // Assert: order status is now CANCELLED
        assertThat(testData.order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(result).isEqualTo(CancelResult.CANCELLED);
    }

    /**
     * 테스트 데이터를 묶어서 전달하기 위한 내부 클래스.
     * 주문, 주문 항목, 빵 엔티티 간의 ID 정합성을 보장합니다.
     */
    static class PendingOrderTestData {
        final OrderEntity order;
        final List<OrderItemEntity> orderItems;
        final List<BreadEntity> breads;
        final Map<Long, Integer> originalRemainingQuantities;

        PendingOrderTestData(OrderEntity order, List<OrderItemEntity> orderItems, List<BreadEntity> breads) {
            this.order = order;
            this.orderItems = orderItems;
            this.breads = breads;
            this.originalRemainingQuantities = breads.stream()
                    .collect(Collectors.toMap(BreadEntity::getId, BreadEntity::getRemainingQuantity));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Property 3: 취소 시 재고 복원 round-trip
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Property 3: 취소 시 재고 복원 round-trip
     *
     * **Validates: Requirements 2.2**
     */
    @Property(tries = 100)
    @Tag("Feature: pending-order-expiry, Property 3: 취소 시 재고 복원 round-trip")
    void cancelExpiredOrder_restoresInventoryByOrderItemQuantity(
            @ForAll("pendingOrderWithItems") PendingOrderTestData testData
    ) {
        Long orderId = testData.order.getId();

        // Reset mutable state for jqwik shrinking idempotency:
        // jqwik may reuse the same object instance across tries/shrink steps
        ReflectionTestUtils.setField(testData.order, "status", OrderStatus.PENDING);
        for (BreadEntity bread : testData.breads) {
            ReflectionTestUtils.setField(bread, "remainingQuantity",
                    testData.originalRemainingQuantities.get(bread.getId()));
        }

        // Record each bread's remainingQuantity BEFORE cancellation
        Map<Long, Integer> beforeQuantities = testData.breads.stream()
                .collect(Collectors.toMap(BreadEntity::getId, BreadEntity::getRemainingQuantity));

        // Record each order item's quantity keyed by breadId
        Map<Long, Integer> orderItemQuantities = testData.orderItems.stream()
                .collect(Collectors.toMap(OrderItemEntity::getBreadId, OrderItemEntity::getQuantity));

        // Mock repository calls
        given(orderRepository.findByIdWithLock(orderId))
                .willReturn(Optional.of(testData.order));
        given(orderItemRepository.findByOrderId(orderId))
                .willReturn(testData.orderItems);

        List<Long> breadIds = testData.orderItems.stream()
                .map(OrderItemEntity::getBreadId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        given(breadRepository.findAllByIdWithLock(breadIds))
                .willReturn(testData.breads);

        // Act
        CancelResult result = realCanceller.cancelExpiredOrder(orderId);

        // Assert: result is CANCELLED
        assertThat(result).isEqualTo(CancelResult.CANCELLED);

        // Assert: for each bread, afterQuantity == beforeQuantity + orderItemQuantity
        for (BreadEntity bread : testData.breads) {
            int beforeQty = beforeQuantities.get(bread.getId());
            int itemQty = orderItemQuantities.get(bread.getId());
            assertThat(bread.getRemainingQuantity())
                    .as("Bread %d: expected %d + %d = %d", bread.getId(), beforeQty, itemQty, beforeQty + itemQty)
                    .isEqualTo(beforeQty + itemQty);
        }
    }

    @Provide
    Arbitrary<PendingOrderTestData> pendingOrderWithItems() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 10000),       // orderId
                Arbitraries.longs().between(1, 1000),         // userId
                Arbitraries.longs().between(1, 500),          // storeId
                Arbitraries.integers().between(100, 100000),  // totalAmount
                Arbitraries.integers().between(1, 5)          // number of items
        ).flatAs((orderId, userId, storeId, totalAmount, itemCount) -> {
            OrderEntity order = OrderEntity.builder()
                    .userId(userId)
                    .storeId(storeId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .build();
            ReflectionTestUtils.setField(order, "id", orderId);

            return Combinators.combine(
                    Arbitraries.integers().between(1, 10).list().ofSize(itemCount),       // quantities
                    Arbitraries.integers().between(100, 50000).list().ofSize(itemCount),   // bread prices
                    Arbitraries.integers().between(0, 1000).list().ofSize(itemCount)       // remaining quantities
            ).as((quantities, prices, remainingQtys) -> {
                List<OrderItemEntity> items = new java.util.ArrayList<>();
                List<BreadEntity> breads = new java.util.ArrayList<>();

                for (int i = 0; i < itemCount; i++) {
                    long breadId = 1000L + i; // unique breadId per item

                    OrderItemEntity item = OrderItemEntity.builder()
                            .orderId(orderId)
                            .breadId(breadId)
                            .breadName("빵-" + breadId)
                            .breadPrice(prices.get(i))
                            .quantity(quantities.get(i))
                            .build();
                    ReflectionTestUtils.setField(item, "id", (long) (i + 1));
                    items.add(item);

                    BreadEntity bread = BreadEntity.builder()
                            .storeId(storeId)
                            .name("빵-" + breadId)
                            .description("설명")
                            .originalPrice(prices.get(i))
                            .salePrice(prices.get(i))
                            .remainingQuantity(remainingQtys.get(i))
                            .build();
                    ReflectionTestUtils.setField(bread, "id", breadId);
                    breads.add(bread);
                }

                return new PendingOrderTestData(order, items, breads);
            });
        });
    }

    // ──────────────────────────────────────────────────────────────────────
    // Property 4: 비-PENDING 주문은 건너뛴다
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Property 4: 비-PENDING 주문은 건너뛴다
     *
     * **Validates: Requirements 2.3**
     */
    @Property(tries = 100)
    @Tag("Feature: pending-order-expiry, Property 4: 비-PENDING 주문 건너뛰기")
    void cancelExpiredOrder_skipsNonPendingOrders(
            @ForAll("nonPendingOrderWithItems") NonPendingOrderTestData testData
    ) {
        Long orderId = testData.order.getId();

        // Record status and quantities BEFORE calling cancelExpiredOrder
        OrderStatus statusBefore = testData.order.getStatus();
        Map<Long, Integer> quantitiesBefore = testData.breads.stream()
                .collect(Collectors.toMap(BreadEntity::getId, BreadEntity::getRemainingQuantity));

        // Mock: findByIdWithLock returns the non-PENDING order
        given(orderRepository.findByIdWithLock(orderId))
                .willReturn(Optional.of(testData.order));

        // Act
        CancelResult result = realCanceller.cancelExpiredOrder(orderId);

        // Assert: result is SKIPPED_STATUS_CHANGED
        assertThat(result).isEqualTo(CancelResult.SKIPPED_STATUS_CHANGED);

        // Assert: order status has NOT changed
        assertThat(testData.order.getStatus()).isEqualTo(statusBefore);

        // Assert: no bread's remainingQuantity has changed
        for (BreadEntity bread : testData.breads) {
            assertThat(bread.getRemainingQuantity())
                    .as("Bread %d: remainingQuantity should not change", bread.getId())
                    .isEqualTo(quantitiesBefore.get(bread.getId()));
        }

        // Assert: orderItemRepository.findByOrderId was NOT called (early return)
        verify(orderItemRepository, never()).findByOrderId(anyLong());
    }

    /**
     * 비-PENDING 주문 테스트 데이터를 묶어서 전달하기 위한 내부 클래스.
     */
    static class NonPendingOrderTestData {
        final OrderEntity order;
        final List<BreadEntity> breads;

        NonPendingOrderTestData(OrderEntity order, List<BreadEntity> breads) {
            this.order = order;
            this.breads = breads;
        }
    }

    @Provide
    Arbitrary<NonPendingOrderTestData> nonPendingOrderWithItems() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 10000),                          // orderId
                Arbitraries.longs().between(1, 1000),                            // userId
                Arbitraries.longs().between(1, 500),                             // storeId
                Arbitraries.integers().between(100, 100000),                     // totalAmount
                Arbitraries.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.CANCEL_PENDING),    // non-PENDING status
                Arbitraries.integers().between(1, 5)                             // number of breads
        ).flatAs((orderId, userId, storeId, totalAmount, status, breadCount) -> {
            OrderEntity order = OrderEntity.builder()
                    .userId(userId)
                    .storeId(storeId)
                    .status(status)
                    .totalAmount(totalAmount)
                    .build();
            ReflectionTestUtils.setField(order, "id", orderId);

            return Arbitraries.integers().between(0, 1000).list().ofSize(breadCount)
                    .map(remainingQtys -> {
                        List<BreadEntity> breads = new java.util.ArrayList<>();
                        for (int i = 0; i < breadCount; i++) {
                            long breadId = 3000L + i;
                            BreadEntity bread = BreadEntity.builder()
                                    .storeId(storeId)
                                    .name("빵-" + breadId)
                                    .description("설명")
                                    .originalPrice(1000)
                                    .salePrice(1000)
                                    .remainingQuantity(remainingQtys.get(i))
                                    .build();
                            ReflectionTestUtils.setField(bread, "id", breadId);
                            breads.add(bread);
                        }
                        return new NonPendingOrderTestData(order, breads);
                    });
        });
    }

    // ──────────────────────────────────────────────────────────────────────
    // Property 5: 예외 격리 — 하나의 실패가 다른 주문 처리에 영향을 주지 않는다
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Property 5: 예외 격리 — 하나의 실패가 다른 주문 처리에 영향을 주지 않는다
     *
     * N개의 만료 대상 중 임의의 k번째에서 예외가 발생해도 나머지 주문이 정상 취소되는지 검증한다.
     * OrderExpiryCanceller mock을 사용하여 개별 취소 동작을 제어한다.
     *
     * **Validates: Requirements 2.4, 2.5**
     */
    @Property(tries = 100)
    @Tag("Feature: pending-order-expiry, Property 5: 예외 격리")
    void processExpiredOrders_isolatesExceptionFromOneOrder(
            @ForAll("expiredOrderListWithFailIndex") ExceptionIsolationTestData testData
    ) {
        // Reset mocks between jqwik tries to avoid accumulated invocations
        reset(orderExpiryCanceller);

        List<OrderEntity> expiredOrders = testData.expiredOrders;
        int failIndex = testData.failIndex;
        Long failingOrderId = expiredOrders.get(failIndex).getId();

        // Mock findExpiredPendingOrders to return our generated list
        given(orderRepository.findExpiredPendingOrders(any(OrderStatus.class), any(LocalDateTime.class), any()))
                .willReturn(expiredOrders);

        // For the failing order: throw RuntimeException
        given(orderExpiryCanceller.cancelExpiredOrder(eq(failingOrderId)))
                .willThrow(new RuntimeException("simulated failure for order " + failingOrderId));

        // For all other orders: return CANCELLED
        for (int i = 0; i < expiredOrders.size(); i++) {
            if (i != failIndex) {
                given(orderExpiryCanceller.cancelExpiredOrder(eq(expiredOrders.get(i).getId())))
                        .willReturn(CancelResult.CANCELLED);
            }
        }

        // Act
        int cancelledCount = orderExpiryService.processExpiredOrders();

        // Assert: cancelled count should be N - 1 (all except the failing one)
        assertThat(cancelledCount)
                .as("Expected %d successful cancellations out of %d orders (1 failure at index %d)",
                        expiredOrders.size() - 1, expiredOrders.size(), failIndex)
                .isEqualTo(expiredOrders.size() - 1);

        // Assert: cancelExpiredOrder was called for ALL orders (including the failing one)
        for (OrderEntity order : expiredOrders) {
            verify(orderExpiryCanceller).cancelExpiredOrder(eq(order.getId()));
        }
    }

    /**
     * 예외 격리 테스트 데이터를 묶어서 전달하기 위한 내부 클래스.
     * N개의 만료 대상 주문과 예외가 발생할 인덱스 k를 포함합니다.
     */
    static class ExceptionIsolationTestData {
        final List<OrderEntity> expiredOrders;
        final int failIndex;

        ExceptionIsolationTestData(List<OrderEntity> expiredOrders, int failIndex) {
            this.expiredOrders = expiredOrders;
            this.failIndex = failIndex;
        }

        @Override
        public String toString() {
            return String.format("ExceptionIsolationTestData{orderCount=%d, failIndex=%d, failingOrderId=%d}",
                    expiredOrders.size(), failIndex, expiredOrders.get(failIndex).getId());
        }
    }

    @Provide
    Arbitrary<ExceptionIsolationTestData> expiredOrderListWithFailIndex() {
        return Arbitraries.integers().between(2, 10).flatMap(n ->
                Arbitraries.integers().between(0, n - 1).flatMap(failIndex -> {
                    // Generate N orders with unique IDs
                    Arbitrary<List<Long>> idsArbitrary = Arbitraries.longs().between(1, 100000)
                            .list().ofSize(n)
                            .map(ids -> ids.stream().distinct().collect(Collectors.toList()))
                            .filter(ids -> ids.size() == n);

                    return idsArbitrary.map(ids -> {
                        LocalDateTime cutoffTime = LocalDateTime.now(FIXED_CLOCK).minusMinutes(EXPIRY_TIMEOUT_MINUTES);
                        List<OrderEntity> orders = new java.util.ArrayList<>();

                        for (int i = 0; i < n; i++) {
                            OrderEntity order = OrderEntity.builder()
                                    .userId(1L)
                                    .storeId(1L)
                                    .status(OrderStatus.PENDING)
                                    .totalAmount(1000)
                                    .build();
                            ReflectionTestUtils.setField(order, "id", ids.get(i));
                            ReflectionTestUtils.setField(order, "createdAt", cutoffTime.minusMinutes(5));
                            orders.add(order);
                        }

                        return new ExceptionIsolationTestData(orders, failIndex);
                    });
                })
        );
    }
}
