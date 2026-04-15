package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class OrderExpiryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private BreadRepository breadRepository;

    private OrderExpiryService orderExpiryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderExpiryService = new OrderExpiryService(
                orderRepository,
                orderItemRepository,
                breadRepository,
                TestFixtures.FIXED_CLOCK
        );
        ReflectionTestUtils.setField(orderExpiryService, "expiryTimeoutMinutes", 10L);
    }

    @Test
    void findExpiredPendingOrders_returnsEmptyListWhenNoExpiredOrders() {
        given(orderRepository.findExpiredPendingOrders(any())).willReturn(Collections.emptyList());

        List<OrderEntity> result = orderExpiryService.findExpiredPendingOrders();

        assertThat(result).isEmpty();
    }

    @Test
    void processExpiredOrders_returnsZeroWhenNoExpiredOrders() {
        OrderExpiryService serviceSpy = spy(orderExpiryService);
        given(orderRepository.findExpiredPendingOrders(any())).willReturn(Collections.emptyList());

        int result = serviceSpy.processExpiredOrders();

        assertThat(result).isZero();
        verify(serviceSpy, never()).cancelExpiredOrder(anyLong());
    }

    @Test
    void cancelExpiredOrder_skipsWhenOrderNotFound() {
        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.empty());

        orderExpiryService.cancelExpiredOrder(1L);

        verify(orderItemRepository, never()).findByOrderId(anyLong());
    }

    @Test
    void cancelExpiredOrder_cancelsOrderAndSkipsMissingBreads() {
        OrderEntity order = TestFixtures.order(1L, 10L, 100L, OrderStatus.PENDING, 5000, "key-1");
        OrderItemEntity item1 = TestFixtures.orderItem(1L, 1L, 50L, 2000, 2);
        OrderItemEntity item2 = TestFixtures.orderItem(2L, 1L, 60L, 3000, 1);

        given(orderRepository.findByIdWithLock(1L)).willReturn(Optional.of(order));
        given(orderItemRepository.findByOrderId(1L)).willReturn(List.of(item1, item2));
        // Return empty list — no breads found at all
        given(breadRepository.findAllByIdWithLock(List.of(50L, 60L))).willReturn(Collections.emptyList());

        orderExpiryService.cancelExpiredOrder(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        // No bread quantity restoration happened since breads were not found
    }
}
