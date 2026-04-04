package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.cart.entity.CartEntity;
import com.todaybread.server.domain.cart.entity.CartItemEntity;
import com.todaybread.server.domain.cart.service.CartService;
import com.todaybread.server.domain.order.dto.OrderDetailResponse;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String IDEMPOTENCY_KEY = "order-key";

    @InjectMocks
    private OrderService orderService;

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

    private CartEntity createCartEntity(Long cartId, Long userId, Long storeId) {
        CartEntity cart = CartEntity.builder().userId(userId).storeId(storeId).build();
        ReflectionTestUtils.setField(cart, "id", cartId);
        return cart;
    }

    private BreadEntity createBreadEntity(Long breadId, Long storeId, int remainingQuantity) {
        BreadEntity bread = BreadEntity.builder()
                .storeId(storeId).name("소금빵").description("겉바속촉")
                .originalPrice(5000).salePrice(3500).remainingQuantity(remainingQuantity).build();
        ReflectionTestUtils.setField(bread, "id", breadId);
        return bread;
    }

    private OrderEntity createOrderEntity(Long orderId, Long userId, Long storeId, OrderStatus status, int totalAmount) {
        OrderEntity order = OrderEntity.builder()
                .userId(userId).storeId(storeId).status(status).totalAmount(totalAmount).build();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    private StoreEntity createStoreEntity(Long storeId) {
        StoreEntity store = StoreEntity.builder()
                .userId(1L).name("오늘의빵집").phoneNumber("02-1234-5678").description("맛있는 빵집")
                .addressLine1("서울시 강남구").addressLine2("1층")
                .latitude(BigDecimal.valueOf(37.5)).longitude(BigDecimal.valueOf(127.0)).build();
        ReflectionTestUtils.setField(store, "id", storeId);
        return store;
    }

    @Nested
    @DisplayName("createOrderFromCart — 빈 Cart 주문 거부")
    class CreateOrderFromCart_EmptyCart {

        @Test
        @DisplayName("CartService가 CART_EMPTY를 던지면 그대로 전파된다")
        void emptyCartThrowsCartEmpty() {
            given(orderRepository.findByUserIdAndIdempotencyKey(1L, IDEMPOTENCY_KEY)).willReturn(Optional.empty());
            given(cartService.getCartWithItemsForCheckout(1L))
                    .willThrow(new CustomException(ErrorCode.CART_EMPTY));

            assertThatThrownBy(() -> orderService.createOrderFromCart(1L, IDEMPOTENCY_KEY))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.CART_EMPTY));
        }
    }

    @Nested
    @DisplayName("createOrderFromCart — 재고 부족 시 원자적 거부")
    class CreateOrderFromCart_InsufficientStock {

        @Test
        @DisplayName("재고 부족 시 BREAD_INSUFFICIENT_QUANTITY 예외를 던지고 재고를 차감하지 않는다")
        void insufficientStockThrowsAndNoDeduction() {
            Long userId = 1L, storeId = 10L, breadId1 = 100L, breadId2 = 200L;

            CartEntity cart = createCartEntity(1L, userId, storeId);
            CartItemEntity item1 = CartItemEntity.builder().cartId(1L).breadId(breadId1).quantity(2).build();
            ReflectionTestUtils.setField(item1, "id", 1L);
            CartItemEntity item2 = CartItemEntity.builder().cartId(1L).breadId(breadId2).quantity(5).build();
            ReflectionTestUtils.setField(item2, "id", 2L);

            BreadEntity bread1 = createBreadEntity(breadId1, storeId, 10);
            BreadEntity bread2 = createBreadEntity(breadId2, storeId, 3);

            given(orderRepository.findByUserIdAndIdempotencyKey(userId, IDEMPOTENCY_KEY)).willReturn(Optional.empty());
            given(cartService.getCartWithItemsForCheckout(userId))
                    .willReturn(new CartService.CartWithItems(cart, List.of(item1, item2)));
            given(breadRepository.findAllByIdWithLock(List.of(breadId1, breadId2)))
                    .willReturn(List.of(bread1, bread2));

            assertThatThrownBy(() -> orderService.createOrderFromCart(userId, IDEMPOTENCY_KEY))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.BREAD_INSUFFICIENT_QUANTITY));

            assertThat(bread1.getRemainingQuantity()).isEqualTo(10);
            assertThat(bread2.getRemainingQuantity()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("getOrderDetail — 다른 유저 Order 접근 거부")
    class GetOrderDetail_AccessDenied {

        @Test
        @DisplayName("다른 유저의 Order 상세 조회 시 ORDER_ACCESS_DENIED 예외를 던진다")
        void otherUserAccessThrowsAccessDenied() {
            OrderEntity order = createOrderEntity(100L, 1L, 10L, OrderStatus.PENDING, 7000);
            given(orderRepository.findById(100L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.getOrderDetail(2L, 100L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED));
        }
    }

    @Nested
    @DisplayName("cancelOrder — CONFIRMED/CANCELLED 상태 변경 거부")
    class CancelOrder_StatusCannotChange {

        @Test
        @DisplayName("CONFIRMED 상태의 Order 취소 시 ORDER_STATUS_CANNOT_CHANGE 예외를 던진다")
        void confirmedOrderCancelThrowsStatusCannotChange() {
            OrderEntity order = createOrderEntity(100L, 1L, 10L, OrderStatus.CONFIRMED, 7000);
            given(orderRepository.findByIdWithLock(100L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 100L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_STATUS_CANNOT_CHANGE));
        }

        @Test
        @DisplayName("CANCELLED 상태의 Order 취소 시 ORDER_STATUS_CANNOT_CHANGE 예외를 던진다")
        void cancelledOrderCancelThrowsStatusCannotChange() {
            OrderEntity order = createOrderEntity(100L, 1L, 10L, OrderStatus.CANCELLED, 7000);
            given(orderRepository.findByIdWithLock(100L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 100L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_STATUS_CANNOT_CHANGE));
        }
    }

    @Nested
    @DisplayName("getOrderDetail — 존재하지 않는 Order 조회")
    class GetOrderDetail_NotFound {

        @Test
        @DisplayName("존재하지 않는 Order ID로 조회 시 ORDER_NOT_FOUND 예외를 던진다")
        void nonExistentOrderThrowsOrderNotFound() {
            given(orderRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderDetail(1L, 999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("createOrderFromCart — idempotency replay")
    class CreateOrderFromCart_IdempotencyReplay {

        @Test
        @DisplayName("같은 key의 재요청이면 기존 주문 상세를 반환한다")
        void sameKeyReturnsExistingOrderDetail() {
            Long userId = 1L, orderId = 100L;
            OrderEntity order = createOrderEntity(orderId, userId, 10L, OrderStatus.PENDING, 7000);
            StoreEntity store = createStoreEntity(10L);

            given(orderRepository.findByUserIdAndIdempotencyKey(userId, IDEMPOTENCY_KEY)).willReturn(Optional.of(order));
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(storeRepository.findById(10L)).willReturn(Optional.of(store));
            given(orderItemRepository.findByOrderId(orderId)).willReturn(List.of());

            OrderDetailResponse response = orderService.createOrderFromCart(userId, IDEMPOTENCY_KEY);

            assertThat(response.orderId()).isEqualTo(orderId);
            verify(cartService, never()).getCartWithItemsForCheckout(userId);
        }
    }
}
