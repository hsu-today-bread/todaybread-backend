package com.todaybread.server.domain.order.scheduler;

import com.todaybread.server.domain.order.service.OrderExpiryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PendingOrderExpirySchedulerTest {

    @Mock
    private OrderExpiryService orderExpiryService;

    private PendingOrderExpiryScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduler = new PendingOrderExpiryScheduler(orderExpiryService);
    }

    /**
     * run() 메서드가 orderExpiryService.processExpiredOrders()를 정확히 한 번 호출하는지 검증한다.
     * Validates: Requirements 3.1, 3.4
     */
    @Test
    void run_callsProcessExpiredOrdersExactlyOnce() {
        given(orderExpiryService.processExpiredOrders()).willReturn(3);

        scheduler.run();

        verify(orderExpiryService, times(1)).processExpiredOrders();
    }
}
