package com.todaybread.server.integration;

import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.user.entity.UserEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration 테스트 - 매출 집계 쿼리 및 API 권한
 *
 * _Requirements: 4.1, 4.3, 4.4, 5.1, 5.3, 5.4, 2.4, 4.6, 5.6, 6.1, 6.4, 6.5_
 */
class BossOrderSalesApiIntegrationTest extends ApiIntegrationTestSupport {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // ──────────────────────────────────────────────────────────────────────
    // Task 9.1: 매출 집계 쿼리 Integration 테스트
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void dailySales_aggregatesOnlyConfirmedAndPickedUpOrders() throws Exception {
        // Setup: boss user + store
        UserEntity boss = saveUser("boss@test.com", "boss-nick", "Boss", "password1234", "010-9999-0001", true);
        StoreEntity store = saveStore(boss, "02-9999-0001", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0));
        String bossToken = bearerToken(boss);

        // Create orders with different statuses
        // The fixed clock is 2026-04-05T12:00:00 KST (2026-04-05T03:00:00Z)
        LocalDateTime orderTime = LocalDateTime.of(2026, 4, 5, 10, 0);

        OrderEntity confirmedOrder = saveOrderWithStatus(store.getId(), 1L, OrderStatus.CONFIRMED, 5000, orderTime);
        OrderEntity pickedUpOrder = saveOrderWithStatus(store.getId(), 2L, OrderStatus.PICKED_UP, 3000, orderTime.plusHours(1));
        OrderEntity pendingOrder = saveOrderWithStatus(store.getId(), 3L, OrderStatus.PENDING, 2000, orderTime.plusHours(2));
        OrderEntity cancelledOrder = saveOrderWithStatus(store.getId(), 4L, OrderStatus.CANCELLED, 1000, orderTime.plusHours(3));

        // Create order items
        saveOrderItem(confirmedOrder.getId(), "소보로빵", 2500, 2);   // 5000
        saveOrderItem(pickedUpOrder.getId(), "소보로빵", 2500, 1);    // 2500
        saveOrderItem(pickedUpOrder.getId(), "크로와상", 3000, 1);     // 3000 (different bread)
        saveOrderItem(pendingOrder.getId(), "소보로빵", 2500, 3);     // should be excluded
        saveOrderItem(cancelledOrder.getId(), "크로와상", 3000, 2);    // should be excluded

        // Act: daily sales for 2026-04-05
        mockMvc.perform(get("/api/boss/sales/daily")
                        .header("Authorization", "Bearer " + bossToken)
                        .param("date", "2026-04-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // 소보로빵: confirmed(2) + pickedUp(1) = 3 qty, 2500*2 + 2500*1 = 7500
                .andExpect(jsonPath("$[?(@.breadName == '소보로빵')].totalQuantity").value(3))
                .andExpect(jsonPath("$[?(@.breadName == '소보로빵')].totalSales").value(7500))
                // 크로와상: pickedUp(1) = 1 qty, 3000*1 = 3000
                .andExpect(jsonPath("$[?(@.breadName == '크로와상')].totalQuantity").value(1))
                .andExpect(jsonPath("$[?(@.breadName == '크로와상')].totalSales").value(3000));
    }

    @Test
    void monthlySales_aggregatesOnlyConfirmedAndPickedUpOrders() throws Exception {
        UserEntity boss = saveUser("boss2@test.com", "boss-nick2", "Boss2", "password1234", "010-9999-0002", true);
        StoreEntity store = saveStore(boss, "02-9999-0002", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0));
        String bossToken = bearerToken(boss);

        // Orders across different days in April 2026
        LocalDateTime day1 = LocalDateTime.of(2026, 4, 1, 10, 0);
        LocalDateTime day15 = LocalDateTime.of(2026, 4, 15, 10, 0);

        OrderEntity order1 = saveOrderWithStatus(store.getId(), 1L, OrderStatus.CONFIRMED, 5000, day1);
        OrderEntity order2 = saveOrderWithStatus(store.getId(), 2L, OrderStatus.PICKED_UP, 3000, day15);
        OrderEntity pendingOrder = saveOrderWithStatus(store.getId(), 3L, OrderStatus.PENDING, 2000, day1);

        saveOrderItem(order1.getId(), "식빵", 1000, 5);    // 5000
        saveOrderItem(order2.getId(), "식빵", 1000, 3);    // 3000
        saveOrderItem(pendingOrder.getId(), "식빵", 1000, 10); // excluded

        mockMvc.perform(get("/api/boss/sales/monthly")
                        .header("Authorization", "Bearer " + bossToken)
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].breadName").value("식빵"))
                .andExpect(jsonPath("$[0].totalQuantity").value(8))
                .andExpect(jsonPath("$[0].totalSales").value(8000));
    }

    @Test
    void dailySales_excludesPendingAndCancelledOrders() throws Exception {
        UserEntity boss = saveUser("boss3@test.com", "boss-nick3", "Boss3", "password1234", "010-9999-0003", true);
        StoreEntity store = saveStore(boss, "02-9999-0003", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0));
        String bossToken = bearerToken(boss);

        LocalDateTime orderTime = LocalDateTime.of(2026, 4, 5, 10, 0);

        // Only PENDING and CANCELLED orders
        OrderEntity pendingOrder = saveOrderWithStatus(store.getId(), 1L, OrderStatus.PENDING, 2000, orderTime);
        OrderEntity cancelledOrder = saveOrderWithStatus(store.getId(), 2L, OrderStatus.CANCELLED, 1000, orderTime);

        saveOrderItem(pendingOrder.getId(), "바게트", 2000, 1);
        saveOrderItem(cancelledOrder.getId(), "바게트", 2000, 1);

        mockMvc.perform(get("/api/boss/sales/daily")
                        .header("Authorization", "Bearer " + bossToken)
                        .param("date", "2026-04-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Task 9.2: API 엔드포인트 권한 Integration 테스트
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void bossOrders_returnsConfirmedOrdersWithItems() throws Exception {
        UserEntity boss = saveUser("boss-orders@test.com", "boss-orders-nick", "BossOrders", "password1234", "010-9999-0010", true);
        StoreEntity store = saveStore(boss, "02-9999-0010", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0));
        String bossToken = bearerToken(boss);

        LocalDateTime orderTime = LocalDateTime.of(2026, 4, 5, 10, 0);

        OrderEntity confirmedOrder = saveOrderWithStatus(store.getId(), 1L, OrderStatus.CONFIRMED, 5000, orderTime);
        OrderEntity pendingOrder = saveOrderWithStatus(store.getId(), 2L, OrderStatus.PENDING, 3000, orderTime.plusHours(1));

        saveOrderItem(confirmedOrder.getId(), "소보로빵", 2500, 2);
        saveOrderItem(pendingOrder.getId(), "크로와상", 3000, 1);

        mockMvc.perform(get("/api/boss/orders")
                        .header("Authorization", "Bearer " + bossToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].orderId").value(confirmedOrder.getId()))
                .andExpect(jsonPath("$.content[0].totalAmount").value(5000))
                .andExpect(jsonPath("$.content[0].items.length()").value(1))
                .andExpect(jsonPath("$.content[0].items[0].breadName").value("소보로빵"));
    }

    @Test
    void pickupOrder_changesStatusToPickedUp() throws Exception {
        UserEntity boss = saveUser("boss-pickup@test.com", "boss-pickup-nick", "BossPickup", "password1234", "010-9999-0011", true);
        StoreEntity store = saveStore(boss, "02-9999-0011", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0));
        String bossToken = bearerToken(boss);

        LocalDateTime orderTime = LocalDateTime.of(2026, 4, 5, 10, 0);
        OrderEntity confirmedOrder = saveOrderWithStatus(store.getId(), 1L, OrderStatus.CONFIRMED, 5000, orderTime);

        mockMvc.perform(post("/api/boss/orders/" + confirmedOrder.getId() + "/pickup")
                        .header("Authorization", "Bearer " + bossToken))
                .andExpect(status().isOk());

        // Verify the order status changed to PICKED_UP
        OrderEntity updated = orderRepository.findById(confirmedOrder.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
    }

    @Test
    void bossOrdersEndpoint_returns401_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/boss/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bossSalesDailyEndpoint_returns401_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/boss/sales/daily")
                        .param("date", "2026-04-05"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bossSalesMonthlyEndpoint_returns401_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/boss/sales/monthly")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bossOrdersEndpoint_returns403_forUserRole() throws Exception {
        UserEntity user = saveUser("user@test.com", "user-nick", "User", "password1234", "010-8888-0001", false);
        String userToken = bearerToken(user);

        mockMvc.perform(get("/api/boss/orders")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void bossSalesDailyEndpoint_returns403_forUserRole() throws Exception {
        UserEntity user = saveUser("user2@test.com", "user-nick2", "User2", "password1234", "010-8888-0002", false);
        String userToken = bearerToken(user);

        mockMvc.perform(get("/api/boss/sales/daily")
                        .header("Authorization", "Bearer " + userToken)
                        .param("date", "2026-04-05"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bossSalesMonthlyEndpoint_returns403_forUserRole() throws Exception {
        UserEntity user = saveUser("user3@test.com", "user-nick3", "User3", "password1234", "010-8888-0003", false);
        String userToken = bearerToken(user);

        mockMvc.perform(get("/api/boss/sales/monthly")
                        .header("Authorization", "Bearer " + userToken)
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pickupEndpoint_returns401_withoutJwt() throws Exception {
        mockMvc.perform(post("/api/boss/orders/1/pickup"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pickupEndpoint_returns403_forUserRole() throws Exception {
        UserEntity user = saveUser("user4@test.com", "user-nick4", "User4", "password1234", "010-8888-0004", false);
        String userToken = bearerToken(user);

        mockMvc.perform(post("/api/boss/orders/1/pickup")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    private OrderEntity saveOrderWithStatus(Long storeId, Long userId, OrderStatus status, int totalAmount, LocalDateTime createdAt) {
        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .storeId(storeId)
                .status(status)
                .totalAmount(totalAmount)
                .build();
        OrderEntity saved = orderRepository.save(order);
        orderRepository.flush();

        // Use TransactionTemplate + native query to override @CreatedDate managed createdAt
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(txStatus -> {
            entityManager.createNativeQuery("UPDATE orders SET created_at = :createdAt WHERE id = :id")
                    .setParameter("createdAt", createdAt)
                    .setParameter("id", saved.getId())
                    .executeUpdate();
            return null;
        });
        entityManager.clear();
        return orderRepository.findById(saved.getId()).orElseThrow();
    }

    private OrderItemEntity saveOrderItem(Long orderId, String breadName, int breadPrice, int quantity) {
        OrderItemEntity item = OrderItemEntity.builder()
                .orderId(orderId)
                .breadId(1L)
                .breadName(breadName)
                .breadPrice(breadPrice)
                .quantity(quantity)
                .build();
        return orderItemRepository.save(item);
    }
}
