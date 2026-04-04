package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.cart.entity.CartEntity;
import com.todaybread.server.domain.cart.entity.CartItemEntity;
import com.todaybread.server.domain.cart.service.CartService;
import com.todaybread.server.domain.order.dto.DirectOrderRequest;
import com.todaybread.server.domain.order.dto.OrderDetailResponse;
import com.todaybread.server.domain.order.dto.OrderResponse;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * Order 도메인 속성 기반 테스트 (jqwik)
 * Feature: order-flow
 */
class OrderPropertyTest {

    private static final String IDEMPOTENCY_KEY = "order-key";

    private OrderService orderService;
    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private CartService cartService;
    private BreadRepository breadRepository;
    private StoreRepository storeRepository;

    @BeforeProperty
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        orderItemRepository = Mockito.mock(OrderItemRepository.class);
        cartService = Mockito.mock(CartService.class);
        breadRepository = Mockito.mock(BreadRepository.class);
        storeRepository = Mockito.mock(StoreRepository.class);
        orderService = new OrderService(
                orderRepository, orderItemRepository,
                cartService, breadRepository, storeRepository
        );
    }

    // ── Helper methods ──

    private BreadEntity createBread(Long breadId, Long storeId, String name, int salePrice, int stock) {
        BreadEntity bread = BreadEntity.builder()
                .storeId(storeId)
                .name(name)
                .description("설명-" + breadId)
                .originalPrice(salePrice + 1000)
                .salePrice(salePrice)
                .remainingQuantity(stock)
                .build();
        ReflectionTestUtils.setField(bread, "id", breadId);
        return bread;
    }

    private CartEntity createCart(Long cartId, Long userId, Long storeId) {
        CartEntity cart = CartEntity.builder().userId(userId).storeId(storeId).build();
        ReflectionTestUtils.setField(cart, "id", cartId);
        return cart;
    }

    private CartItemEntity createCartItem(Long itemId, Long cartId, Long breadId, int quantity) {
        CartItemEntity item = CartItemEntity.builder()
                .cartId(cartId)
                .breadId(breadId)
                .quantity(quantity)
                .build();
        ReflectionTestUtils.setField(item, "id", itemId);
        return item;
    }

    private StoreEntity createStore(Long storeId, String name) {
        StoreEntity store = StoreEntity.builder()
                .userId(99L)
                .name(name)
                .phoneNumber("010-0000-" + String.format("%04d", storeId))
                .description("매장 설명")
                .addressLine1("주소1")
                .addressLine2("주소2")
                .latitude(BigDecimal.valueOf(37.5))
                .longitude(BigDecimal.valueOf(127.0))
                .build();
        ReflectionTestUtils.setField(store, "id", storeId);
        return store;
    }

    private OrderEntity createOrder(Long orderId, Long userId, Long storeId, OrderStatus status, int totalAmount) {
        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .storeId(storeId)
                .status(status)
                .totalAmount(totalAmount)
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    private OrderItemEntity createOrderItem(Long itemId, Long orderId, Long breadId,
                                            String breadName, int breadPrice, int quantity) {
        OrderItemEntity item = OrderItemEntity.builder()
                .orderId(orderId)
                .breadId(breadId)
                .breadName(breadName)
                .breadPrice(breadPrice)
                .quantity(quantity)
                .build();
        ReflectionTestUtils.setField(item, "id", itemId);
        return item;
    }

    /**
     * Sets up common mocks for createOrderFromCart:
     * - cartService.getCartWithItemsForCheckout, breadRepository (findAllByIdWithLock)
     * - orderRepository.save (sets order ID via ReflectionTestUtils)
     * - storeRepository
     */
    private void setupCartOrderMocks(Long userId, Long cartId, Long storeId,
                                     List<CartItemEntity> cartItems,
                                     List<BreadEntity> breads) {
        CartEntity cart = createCart(cartId, userId, storeId);
        StoreEntity store = createStore(storeId, "테스트매장");

        given(orderRepository.findByUserIdAndIdempotencyKey(userId, IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(cartService.getCartWithItemsForCheckout(userId))
                .willReturn(new CartService.CartWithItems(cart, cartItems));

        List<Long> breadIds = cartItems.stream().map(CartItemEntity::getBreadId).toList();
        given(breadRepository.findAllByIdWithLock(breadIds)).willReturn(breads);

        // Mock orderRepository.save to set the order ID
        given(orderRepository.save(any(OrderEntity.class))).willAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 1000L);
            return order;
        });

        lenient().when(orderItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
    }


    // ── Property 9: 주문 생성 시 PENDING 상태 ──
    // Feature: order-flow, Property 9: 주문 생성 시 PENDING 상태
    // **Validates: Requirements 6.1, 7.1**
    @Property(tries = 100)
    void createdOrderFromCartHasPendingStatus(
            @ForAll @IntRange(min = 1, max = 5) int itemCount,
            @ForAll @IntRange(min = 1, max = 10) int quantity
    ) {
        Long userId = 1L, cartId = 1L, storeId = 100L;

        List<CartItemEntity> cartItems = new ArrayList<>();
        List<BreadEntity> breads = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            long breadId = 10L + i;
            cartItems.add(createCartItem((long) (i + 1), cartId, breadId, quantity));
            breads.add(createBread(breadId, storeId, "빵-" + breadId, 3000 + i * 100, quantity + 50));
        }

        setupCartOrderMocks(userId, cartId, storeId, cartItems, breads);

        OrderDetailResponse response = orderService.createOrderFromCart(userId, IDEMPOTENCY_KEY);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
    }

    // Feature: order-flow, Property 9: 주문 생성 시 PENDING 상태 (바로 구매)
    // **Validates: Requirements 7.1**
    @Property(tries = 100)
    void createdDirectOrderHasPendingStatus(
            @ForAll @IntRange(min = 1, max = 50) int quantity
    ) {
        Long userId = 1L, breadId = 10L, storeId = 100L;
        BreadEntity bread = createBread(breadId, storeId, "소금빵", 3500, quantity + 50);
        StoreEntity store = createStore(storeId, "테스트매장");

        given(orderRepository.findByUserIdAndIdempotencyKey(userId, IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(breadRepository.findAllByIdWithLock(List.of(breadId))).willReturn(List.of(bread));
        given(orderRepository.save(any(OrderEntity.class))).willAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 1000L);
            return order;
        });
        lenient().when(orderItemRepository.save(any(OrderItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

        DirectOrderRequest request = new DirectOrderRequest(breadId, quantity);
        OrderDetailResponse response = orderService.createDirectOrder(userId, request, IDEMPOTENCY_KEY);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
    }

    // ── Property 10: Order_Item 스냅샷 정합성 ──
    // Feature: order-flow, Property 10: Order_Item 스냅샷 정합성
    // **Validates: Requirements 6.2, 7.5**
    @Property(tries = 100)
    void orderItemSnapshotMatchesBreadData(
            @ForAll @IntRange(min = 1, max = 5) int itemCount,
            @ForAll @IntRange(min = 1, max = 10) int quantity
    ) {
        Long userId = 1L, cartId = 1L, storeId = 100L;

        List<CartItemEntity> cartItems = new ArrayList<>();
        List<BreadEntity> breads = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            long breadId = 10L + i;
            int salePrice = 3000 + i * 500;
            cartItems.add(createCartItem((long) (i + 1), cartId, breadId, quantity));
            breads.add(createBread(breadId, storeId, "빵-" + breadId, salePrice, quantity + 50));
        }

        setupCartOrderMocks(userId, cartId, storeId, cartItems, breads);

        // Capture saved OrderItems
        ArgumentCaptor<List<OrderItemEntity>> captor = ArgumentCaptor.forClass(List.class);
        given(orderItemRepository.saveAll(captor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrderFromCart(userId, IDEMPOTENCY_KEY);

        List<OrderItemEntity> savedItems = captor.getValue();
        assertThat(savedItems).hasSize(itemCount);

        for (int i = 0; i < itemCount; i++) {
            BreadEntity bread = breads.get(i);
            OrderItemEntity orderItem = savedItems.get(i);
            assertThat(orderItem.getBreadName()).isEqualTo(bread.getName());
            assertThat(orderItem.getBreadPrice()).isEqualTo(bread.getSalePrice());
            assertThat(orderItem.getQuantity()).isEqualTo(quantity);
        }
    }

    // ── Property 11: 주문 생성 시 재고 차감 ──
    // Feature: order-flow, Property 11: 주문 생성 시 재고 차감
    // **Validates: Requirements 6.3, 7.4**
    @Property(tries = 100)
    void orderCreationDecrementsStock(
            @ForAll @IntRange(min = 1, max = 5) int itemCount,
            @ForAll @IntRange(min = 1, max = 10) int quantity
    ) {
        Long userId = 1L, cartId = 1L, storeId = 100L;
        int initialStock = quantity + 50;

        List<CartItemEntity> cartItems = new ArrayList<>();
        List<BreadEntity> breads = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            long breadId = 10L + i;
            cartItems.add(createCartItem((long) (i + 1), cartId, breadId, quantity));
            breads.add(createBread(breadId, storeId, "빵-" + breadId, 3000, initialStock));
        }

        setupCartOrderMocks(userId, cartId, storeId, cartItems, breads);

        orderService.createOrderFromCart(userId, IDEMPOTENCY_KEY);

        for (BreadEntity bread : breads) {
            assertThat(bread.getRemainingQuantity()).isEqualTo(initialStock - quantity);
        }
    }


    // ── Property 12: 재고 부족 시 주문 원자적 거부 ──
    // Feature: order-flow, Property 12: 재고 부족 시 주문 원자적 거부
    // **Validates: Requirements 6.4**
    @Property(tries = 100)
    void insufficientStockRejectsOrderAndKeepsAllStockUnchanged(
            @ForAll @IntRange(min = 1, max = 5) int sufficientCount,
            @ForAll @IntRange(min = 1, max = 10) int quantity
    ) {
        Long userId = 1L, cartId = 1L, storeId = 100L;
        int totalItems = sufficientCount + 1; // last item has insufficient stock

        List<CartItemEntity> cartItems = new ArrayList<>();
        List<BreadEntity> breads = new ArrayList<>();
        int[] originalStocks = new int[totalItems];

        for (int i = 0; i < totalItems; i++) {
            long breadId = 10L + i;
            int stock;
            if (i < sufficientCount) {
                stock = quantity + 50; // sufficient
            } else {
                stock = Math.max(quantity - 1, 0); // insufficient
            }
            originalStocks[i] = stock;
            cartItems.add(createCartItem((long) (i + 1), cartId, breadId, quantity));
            breads.add(createBread(breadId, storeId, "빵-" + breadId, 3000, stock));
        }

        CartEntity cart = createCart(cartId, userId, storeId);
        given(orderRepository.findByUserIdAndIdempotencyKey(userId, IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(cartService.getCartWithItemsForCheckout(userId))
                .willReturn(new CartService.CartWithItems(cart, cartItems));
        List<Long> breadIds = cartItems.stream().map(CartItemEntity::getBreadId).toList();
        given(breadRepository.findAllByIdWithLock(breadIds)).willReturn(breads);

        assertThatThrownBy(() -> orderService.createOrderFromCart(userId, IDEMPOTENCY_KEY))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.BREAD_INSUFFICIENT_QUANTITY));

        // ALL bread stock remains unchanged
        for (int i = 0; i < totalItems; i++) {
            assertThat(breads.get(i).getRemainingQuantity()).isEqualTo(originalStocks[i]);
        }
    }

    // ── Property 13: 주문 성공 시 Cart 비우기 ──
    // Feature: order-flow, Property 13: 주문 성공 시 Cart 비우기
    // **Validates: Requirements 6.5**
    @Property(tries = 100)
    void successfulCartOrderClearsCart(
            @ForAll @IntRange(min = 1, max = 5) int itemCount,
            @ForAll @IntRange(min = 1, max = 10) int quantity
    ) {
        Mockito.reset(cartService);

        Long userId = 1L, cartId = 1L, storeId = 100L;

        List<CartItemEntity> cartItems = new ArrayList<>();
        List<BreadEntity> breads = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            long breadId = 10L + i;
            cartItems.add(createCartItem((long) (i + 1), cartId, breadId, quantity));
            breads.add(createBread(breadId, storeId, "빵-" + breadId, 3000, quantity + 50));
        }

        setupCartOrderMocks(userId, cartId, storeId, cartItems, breads);

        orderService.createOrderFromCart(userId, IDEMPOTENCY_KEY);

        verify(cartService).clearCart(userId);
    }

    // ── Property 14: 총 결제 금액 계산 ──
    // Feature: order-flow, Property 14: 총 결제 금액 계산
    // **Validates: Requirements 6.7**
    @Property(tries = 100)
    void totalAmountEqualsSumOfBreadPriceTimesQuantity(
            @ForAll @IntRange(min = 1, max = 5) int itemCount,
            @ForAll @IntRange(min = 1, max = 10) int quantity,
            @ForAll @IntRange(min = 100, max = 10000) int basePrice
    ) {
        Long userId = 1L, cartId = 1L, storeId = 100L;

        List<CartItemEntity> cartItems = new ArrayList<>();
        List<BreadEntity> breads = new ArrayList<>();
        int expectedTotal = 0;
        for (int i = 0; i < itemCount; i++) {
            long breadId = 10L + i;
            int salePrice = basePrice + i * 100;
            cartItems.add(createCartItem((long) (i + 1), cartId, breadId, quantity));
            breads.add(createBread(breadId, storeId, "빵-" + breadId, salePrice, quantity + 50));
            expectedTotal += salePrice * quantity;
        }

        setupCartOrderMocks(userId, cartId, storeId, cartItems, breads);

        OrderDetailResponse response = orderService.createOrderFromCart(userId, IDEMPOTENCY_KEY);

        assertThat(response.totalAmount()).isEqualTo(expectedTotal);
    }


    // ── Property 15: 취소 시 재고 복원 ──
    // Feature: order-flow, Property 15: 취소 시 재고 복원
    // **Validates: Requirements 8.2, 8.3**
    @Property(tries = 100)
    void cancellingPendingOrderRestoresStock(
            @ForAll @IntRange(min = 1, max = 5) int itemCount,
            @ForAll @IntRange(min = 1, max = 10) int quantity
    ) {
        Long userId = 1L, orderId = 100L, storeId = 100L;
        int currentStock = 20; // stock after order was placed (original - quantity)

        OrderEntity order = createOrder(orderId, userId, storeId, OrderStatus.PENDING, 7000);

        List<OrderItemEntity> orderItems = new ArrayList<>();
        List<BreadEntity> breads = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            long breadId = 10L + i;
            orderItems.add(createOrderItem((long) (i + 1), orderId, breadId, "빵-" + breadId, 3000, quantity));
            breads.add(createBread(breadId, storeId, "빵-" + breadId, 3000, currentStock));
        }

        given(orderRepository.findByIdWithLock(orderId)).willReturn(Optional.of(order));
        given(orderItemRepository.findByOrderId(orderId)).willReturn(orderItems);

        List<Long> breadIdsForLock = orderItems.stream()
                .map(OrderItemEntity::getBreadId)
                .filter(java.util.Objects::nonNull)
                .toList();
        given(breadRepository.findAllByIdWithLock(breadIdsForLock)).willReturn(breads);

        orderService.cancelOrder(userId, orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        for (BreadEntity bread : breads) {
            assertThat(bread.getRemainingQuantity()).isEqualTo(currentStock + quantity);
        }
    }

    // ── Property 16: PENDING 상태 가드 ──
    // Feature: order-flow, Property 16: PENDING 상태 가드
    // **Validates: Requirements 8.4, 10.6**
    @Property(tries = 100)
    void confirmedOrCancelledOrderRejectsStatusChange(
            @ForAll @IntRange(min = 0, max = 1) int statusIndex
    ) {
        Long userId = 1L, orderId = 100L, storeId = 100L;
        OrderStatus nonPendingStatus = statusIndex == 0 ? OrderStatus.CONFIRMED : OrderStatus.CANCELLED;

        OrderEntity order = createOrder(orderId, userId, storeId, nonPendingStatus, 7000);
        given(orderRepository.findByIdWithLock(orderId)).willReturn(Optional.of(order));

        // Cancel should be rejected
        assertThatThrownBy(() -> orderService.cancelOrder(userId, orderId))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_STATUS_CANNOT_CHANGE));

        // Status remains unchanged
        assertThat(order.getStatus()).isEqualTo(nonPendingStatus);
    }

    // Feature: order-flow, Property 16: PENDING 상태 가드 (confirmOrder)
    // **Validates: Requirements 8.4, 10.6**
    @Property(tries = 100)
    void confirmedOrCancelledOrderRejectsConfirm(
            @ForAll @IntRange(min = 0, max = 1) int statusIndex
    ) {
        Long orderId = 100L, storeId = 100L;
        OrderStatus nonPendingStatus = statusIndex == 0 ? OrderStatus.CONFIRMED : OrderStatus.CANCELLED;

        OrderEntity order = createOrder(orderId, 1L, storeId, nonPendingStatus, 7000);
        given(orderRepository.findByIdWithLock(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmOrder(orderId))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_STATUS_CANNOT_CHANGE));

        assertThat(order.getStatus()).isEqualTo(nonPendingStatus);
    }

    // ── Property 17: 주문 목록 조회 — 최신순 정렬 및 필수 필드 ──
    // Feature: order-flow, Property 17: 주문 목록 조회 — 최신순 정렬 및 필수 필드
    // **Validates: Requirements 9.1, 9.2**
    @Property(tries = 100)
    void getOrdersReturnsNItemsWithRequiredFieldsInDescOrder(
            @ForAll @IntRange(min = 1, max = 10) int n
    ) {
        Long userId = 1L, storeId = 100L;
        StoreEntity store = createStore(storeId, "테스트매장");

        List<OrderEntity> orders = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            OrderEntity order = createOrder((long) (i + 1), userId, storeId, OrderStatus.PENDING, 3000 * (i + 1));
            ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.of(2024, 1, 15, 12, 0).minusHours(i));
            orders.add(order);
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        given(orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .willReturn(new org.springframework.data.domain.PageImpl<>(orders, pageable, orders.size()));
        given(storeRepository.findAllById(List.of(storeId))).willReturn(List.of(store));

        org.springframework.data.domain.Page<OrderResponse> page = orderService.getOrders(userId, pageable);

        assertThat(page.getContent()).hasSize(n);

        for (int i = 0; i < n - 1; i++) {
            assertThat(page.getContent().get(i).createdAt())
                    .isAfterOrEqualTo(page.getContent().get(i + 1).createdAt());
        }

        for (OrderResponse response : page.getContent()) {
            assertThat(response.orderId()).isNotNull();
            assertThat(response.storeName()).isEqualTo("테스트매장");
            assertThat(response.status()).isNotNull();
            assertThat(response.totalAmount()).isGreaterThan(0);
            assertThat(response.createdAt()).isNotNull();
        }
    }
}
