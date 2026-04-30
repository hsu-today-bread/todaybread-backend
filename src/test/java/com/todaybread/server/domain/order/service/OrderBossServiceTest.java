package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.order.entity.OrderEntity;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * OrderBossService 단위 테스트
 *
 * _Requirements: 2.5, 3.3_
 */
class OrderBossServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private StoreRepository storeRepository;

    private OrderBossService orderBossService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderBossService = new OrderBossService(orderRepository, orderItemRepository, storeRepository);
    }

    @Test
    void getConfirmedOrders_throwsStoreNotFound_whenNoStoreRegistered() {
        given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderBossService.getConfirmedOrders(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    @Test
    void pickupOrder_throwsOrderNotFound_whenOrderDoesNotExist() {
        StoreEntity store = TestFixtures.store(100L, 1L);
        given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(store));
        given(orderRepository.findByIdWithLock(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderBossService.pickupOrder(1L, 999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void pickupOrder_throwsOrderAccessDenied_whenOrderBelongsToDifferentStore() {
        StoreEntity store = TestFixtures.store(100L, 1L);
        OrderEntity order = TestFixtures.order(1L, 10L, 200L, OrderStatus.CONFIRMED, 5000, "key");

        given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(store));
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderBossService.pickupOrder(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
    }

    @Test
    void pickupOrder_throwsOrderStatusCannotChange_whenOrderIsNotConfirmed() {
        StoreEntity store = TestFixtures.store(100L, 1L);
        OrderEntity order = TestFixtures.order(1L, 10L, 100L, OrderStatus.PENDING, 5000, "key");

        given(storeRepository.findByUserIdAndIsActiveTrue(1L)).willReturn(Optional.of(store));
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderBossService.pickupOrder(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_STATUS_CANNOT_CHANGE);
    }
}
