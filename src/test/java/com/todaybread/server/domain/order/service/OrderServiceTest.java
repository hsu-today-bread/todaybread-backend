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
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartService cartService;

    @Mock
    private BreadRepository breadRepository;

    @Mock
    private StoreRepository storeRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        InventoryRestorer inventoryRestorer = new InventoryRestorer(breadRepository);
        orderService = new OrderService(
                orderRepository,
                orderItemRepository,
                cartService,
                breadRepository,
                storeRepository,
                inventoryRestorer
        );
    }

    @Test
    void createOrderFromCart_returnsExistingOrderForSameIdempotencyKey() {
        OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 3_000, "same-key");
        StoreEntity store = TestFixtures.store(100L, 10L);
        OrderItemEntity item = TestFixtures.orderItem(1L, 1L, 10L, 3_000, 1);

        given(orderRepository.findByUserIdAndIdempotencyKey(1L, "same-key")).willReturn(Optional.of(order));
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(storeRepository.findById(100L)).willReturn(Optional.of(store));
        given(orderItemRepository.findByOrderId(1L)).willReturn(List.of(item));

        OrderDetailResponse response = orderService.createOrderFromCart(1L, "same-key");

        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.storeName()).isEqualTo(store.getName());
        verify(cartService, never()).getCartWithItemsForCheckout(any());
    }

    @Test
    void createOrderFromCart_createsOrderAndClearsCart() {
        CartEntity cart = TestFixtures.cart(50L, 1L, 100L);
        CartItemEntity cartItem = TestFixtures.cartItem(1L, 50L, 10L, 2);
        BreadEntity bread = TestFixtures.bread(10L, 100L, 5, 4_000, 2_000);
        StoreEntity store = TestFixtures.store(100L, 10L);

        given(orderRepository.findByUserIdAndIdempotencyKey(1L, "order-key"))
                .willReturn(Optional.empty(), Optional.empty());
        given(cartService.getCartWithItemsForCheckout(1L))
                .willReturn(new CartService.CartWithItems(cart, List.of(cartItem)));
        given(breadRepository.findAllByIdWithLock(List.of(10L))).willReturn(List.of(bread));
        given(orderRepository.save(any(OrderEntity.class))).willAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 500L);
            return order;
        });
        given(storeRepository.findById(100L)).willReturn(Optional.of(store));

        OrderDetailResponse response = orderService.createOrderFromCart(1L, "order-key");

        assertThat(response.orderId()).isEqualTo(500L);
        assertThat(response.totalAmount()).isEqualTo(4_000);
        assertThat(bread.getRemainingQuantity()).isEqualTo(3);
        verify(orderItemRepository).saveAll(any());
        verify(cartService).clearCart(1L);
    }

    @Test
    void createOrderFromCart_returnsExistingOrderAfterCartEmptyRace() {
        OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 3_000, "race-key");
        StoreEntity store = TestFixtures.store(100L, 10L);
        OrderItemEntity item = TestFixtures.orderItem(1L, 1L, 10L, 3_000, 1);

        given(orderRepository.findByUserIdAndIdempotencyKey(1L, "race-key"))
                .willReturn(Optional.empty(), Optional.of(order));
        given(cartService.getCartWithItemsForCheckout(1L))
                .willThrow(new CustomException(ErrorCode.CART_EMPTY));
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(storeRepository.findById(100L)).willReturn(Optional.of(store));
        given(orderItemRepository.findByOrderId(1L)).willReturn(List.of(item));

        OrderDetailResponse response = orderService.createOrderFromCart(1L, "race-key");

        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void createDirectOrder_createsPendingOrder() {
        BreadEntity bread = TestFixtures.bread(10L, 100L, 5, 4_000, 2_000);
        StoreEntity store = TestFixtures.store(100L, 10L);

        given(orderRepository.findByUserIdAndIdempotencyKey(1L, "direct-key"))
                .willReturn(Optional.empty(), Optional.empty());
        given(breadRepository.findAllByIdWithLock(List.of(10L))).willReturn(List.of(bread));
        given(orderRepository.save(any(OrderEntity.class))).willAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 500L);
            return order;
        });
        given(storeRepository.findById(100L)).willReturn(Optional.of(store));

        OrderDetailResponse response = orderService.createDirectOrder(1L, new DirectOrderRequest(10L, 2), "direct-key");

        assertThat(response.orderId()).isEqualTo(500L);
        assertThat(response.totalAmount()).isEqualTo(4_000);
        assertThat(bread.getRemainingQuantity()).isEqualTo(3);
        verify(orderItemRepository).save(any(OrderItemEntity.class));
    }

    @Test
    void cancelOrder_restoresBreadStockForOwnedOrder() {
        OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 3_000, "order-key");
        OrderItemEntity item = TestFixtures.orderItem(1L, 1L, 10L, 3_000, 2);
        BreadEntity bread = TestFixtures.bread(10L, 100L, 3, 4_000, 2_000);

        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
        given(orderItemRepository.findByOrderId(1L)).willReturn(List.of(item));
        given(breadRepository.findAllByIdWithLock(List.of(10L))).willReturn(List.of(bread));

        orderService.cancelOrder(1L, 1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(bread.getRemainingQuantity()).isEqualTo(5);
    }

    @Test
    void confirmOrder_updatesStatus() {
        OrderEntity order = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 3_000, "order-key");
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));

        orderService.confirmOrder(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void getOrders_mapsStoreNames() {
        OrderEntity order1 = TestFixtures.order(1L, 1L, 100L, OrderStatus.PENDING, 3_000, "key-1");
        OrderEntity order2 = TestFixtures.order(2L, 1L, 200L, OrderStatus.CONFIRMED, 5_000, "key-2");
        StoreEntity store1 = TestFixtures.store(100L, 10L);
        StoreEntity store2 = TestFixtures.store(200L, 20L);

        given(orderRepository.findByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of(order1, order2)));
        given(storeRepository.findAllById(List.of(100L, 200L))).willReturn(List.of(store1, store2));

        Page<OrderResponse> response = orderService.getOrders(1L, PageRequest.of(0, 10));

        assertThat(response.getContent()).extracting(OrderResponse::storeName)
                .containsExactly(store1.getName(), store2.getName());
    }

    @Test
    void getOrderDetail_rejectsOtherUsersOrder() {
        OrderEntity order = TestFixtures.order(1L, 2L, 100L, OrderStatus.PENDING, 3_000, "key");
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderDetail(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
    }
}
