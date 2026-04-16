package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.order.config.OrderExpiryProperties;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OrderExpiryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderExpiryCanceller orderExpiryCanceller;

    private OrderExpiryService orderExpiryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        OrderExpiryProperties properties = new OrderExpiryProperties();
        properties.setTimeoutMinutes(10L);
        properties.setBatchSize(100);

        orderExpiryService = new OrderExpiryService(
                orderRepository,
                orderExpiryCanceller,
                TestFixtures.FIXED_CLOCK,
                properties
        );
    }

    @Test
    void findExpiredPendingOrders_returnsEmptyListWhenNoExpiredOrders() {
        given(orderRepository.findExpiredPendingOrders(any(), any(), any())).willReturn(Collections.emptyList());

        List<OrderEntity> result = orderExpiryService.findExpiredPendingOrders();

        assertThat(result).isEmpty();
    }

    @Test
    void processExpiredOrders_returnsZeroWhenNoExpiredOrders() {
        given(orderRepository.findExpiredPendingOrders(any(), any(), any())).willReturn(Collections.emptyList());

        int result = orderExpiryService.processExpiredOrders();

        assertThat(result).isZero();
        verify(orderExpiryCanceller, never()).cancelExpiredOrder(anyLong());
    }

    @Test
    void processExpiredOrders_countsCancelledAndSkippedCorrectly() {
        OrderEntity order1 = TestFixtures.order(1L, 10L, 100L, OrderStatus.PENDING, 5000, "key-1");
        OrderEntity order2 = TestFixtures.order(2L, 10L, 100L, OrderStatus.PENDING, 3000, "key-2");
        OrderEntity order3 = TestFixtures.order(3L, 10L, 100L, OrderStatus.PENDING, 4000, "key-3");

        given(orderRepository.findExpiredPendingOrders(any(), any(), any()))
                .willReturn(List.of(order1, order2, order3));
        given(orderExpiryCanceller.cancelExpiredOrder(1L)).willReturn(CancelResult.CANCELLED);
        given(orderExpiryCanceller.cancelExpiredOrder(2L)).willReturn(CancelResult.SKIPPED_STATUS_CHANGED);
        given(orderExpiryCanceller.cancelExpiredOrder(3L)).willReturn(CancelResult.CANCELLED);

        int result = orderExpiryService.processExpiredOrders();

        assertThat(result).isEqualTo(2);
    }
}
