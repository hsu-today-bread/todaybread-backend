package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.cart.entity.CartEntity;
import com.todaybread.server.domain.cart.entity.CartItemEntity;
import com.todaybread.server.domain.cart.repository.CartItemRepository;
import com.todaybread.server.domain.cart.repository.CartRepository;
import com.todaybread.server.domain.cart.service.CartService;
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

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartService cartService;

    @Mock
    private BreadRepository breadRepository;

    @Mock
    private StoreRepository storeRepository;

    // ── 헬퍼 메서드 ──

    private CartEntity createCartEntity(Long cartId, Long userId, Long storeId) {
        CartEntity cart = CartEntity.builder()
                .userId(userId)
                .storeId(storeId)
                .build();
        ReflectionTestUtils.setField(cart, "id", cartId);
        return cart;
    }

    private CartItemEntity createCartItemEntity(Long itemId, Long cartId, Long breadId, int quantity) {
        CartItemEntity item = CartItemEntity.builder()
                .cartId(cartId)
                .breadId(breadId)
                .quantity(quantity)
                .build();
        ReflectionTestUtils.setField(item, "id", itemId);
        return item;
    }

    private BreadEntity createBreadEntity(Long breadId, Long storeId, int remainingQuantity) {
        BreadEntity bread = BreadEntity.builder()
                .storeId(storeId)
                .name("소금빵")
                .description("겉바속촉")
                .originalPrice(5000)
                .salePrice(3500)
                .remainingQuantity(remainingQuantity)
                .build();
        ReflectionTestUtils.setField(bread, "id", breadId);
        return bread;
    }

    private OrderEntity createOrderEntity(Long orderId, Long userId, Long storeId, OrderStatus status, int totalAmount) {
        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .storeId(storeId)
                .status(status)
                .totalAmount(totalAmount)
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    private StoreEntity createStoreEntity(Long storeId) {
        StoreEntity store = StoreEntity.builder()
                .userId(1L)
                .name("오늘의빵집")
                .phoneNumber("02-1234-5678")
                .description("맛있는 빵집")
                .addressLine1("서울시 강남구")
                .addressLine2("1층")
                .latitude(BigDecimal.valueOf(37.5))
                .longitude(BigDecimal.valueOf(127.0))
                .build();
        ReflectionTestUtils.setField(store, "id", storeId);
        return store;
    }

    // ── 테스트 ──

    @Nested
    @DisplayName("createOrderFromCart — 빈 Cart 주문 거부")
    class CreateOrderFromCart_EmptyCart {

        @Test
        @DisplayName("Cart가 존재하지 않으면 CART_EMPTY 예외를 던진다")
        void noCartThrowsCartEmpty() {
            given(cartRepository.findByUserId(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrderFromCart(1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.CART_EMPTY));
        }

        @Test
        @DisplayName("Cart에 항목이 없으면 CART_EMPTY 예외를 던진다")
        void emptyCartItemsThrowsCartEmpty() {
            CartEntity cart = createCartEntity(1L, 1L, 10L);
            given(cartRepository.findByUserId(1L)).willReturn(Optional.of(cart));
            given(cartItemRepository.findByCartId(1L)).willReturn(List.of());

            assertThatThrownBy(() -> orderService.createOrderFromCart(1L))
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
            Long userId = 1L;
            Long storeId = 10L;
            Long breadId1 = 100L;
            Long breadId2 = 200L;

            CartEntity cart = createCartEntity(1L, userId, storeId);
            CartItemEntity item1 = createCartItemEntity(1L, 1L, breadId1, 2);
            CartItemEntity item2 = createCartItemEntity(2L, 1L, breadId2, 5);

            // breadId1: 재고 10 (충분), breadId2: 재고 3 (부족 — 요청 5)
            BreadEntity bread1 = createBreadEntity(breadId1, storeId, 10);
            BreadEntity bread2 = createBreadEntity(breadId2, storeId, 3);

            given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));
            given(cartItemRepository.findByCartId(1L)).willReturn(List.of(item1, item2));
            given(breadRepository.findAllByIdWithLock(List.of(breadId1, breadId2)))
                    .willReturn(List.of(bread1, bread2));

            assertThatThrownBy(() -> orderService.createOrderFromCart(userId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.BREAD_INSUFFICIENT_QUANTITY));

            // 재고가 차감되지 않았는지 확인
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
            Long ownerUserId = 1L;
            Long otherUserId = 2L;
            Long orderId = 100L;

            OrderEntity order = createOrderEntity(orderId, ownerUserId, 10L, OrderStatus.PENDING, 7000);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.getOrderDetail(otherUserId, orderId))
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
            Long userId = 1L;
            Long orderId = 100L;

            OrderEntity order = createOrderEntity(orderId, userId, 10L, OrderStatus.CONFIRMED, 7000);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(userId, orderId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_STATUS_CANNOT_CHANGE));
        }

        @Test
        @DisplayName("CANCELLED 상태의 Order 취소 시 ORDER_STATUS_CANNOT_CHANGE 예외를 던진다")
        void cancelledOrderCancelThrowsStatusCannotChange() {
            Long userId = 1L;
            Long orderId = 100L;

            OrderEntity order = createOrderEntity(orderId, userId, 10L, OrderStatus.CANCELLED, 7000);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(userId, orderId))
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
}
