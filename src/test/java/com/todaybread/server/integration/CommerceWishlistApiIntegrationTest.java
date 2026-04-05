package com.todaybread.server.integration;

import com.todaybread.server.domain.cart.entity.CartEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommerceWishlistApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void userCanManageWishlistAndCompleteCheckoutFlow() throws Exception {
        UserEntity boss = saveUser(
                "shop@example.com",
                "shop-owner",
                "Shop Owner",
                "password1234",
                "010-6666-6666",
                true
        );
        UserEntity user = saveUser(
                "buyer@example.com",
                "buyer-user",
                "Buyer User",
                "password1234",
                "010-7777-7777",
                false
        );
        StoreEntity store = saveStore(boss, "02-2222-3333", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0));
        saveStandardBusinessHours(store.getId());
        saveStoreImage(store.getId(), "seed-store.jpg", 0);
        Long breadId = saveBread(store.getId(), "Bagel", 4000, 2500, 5).getId();
        saveBreadImage(breadId, "seed-bread.jpg");

        String userToken = bearerToken(user);

        mockMvc.perform(post("/api/keywords")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keyword": "bagel"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/favourite-stores")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": %d
                                }
                                """.formatted(store.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.added").value(true));

        mockMvc.perform(get("/api/wishlist")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords[0].displayText").value("bagel"))
                .andExpect(jsonPath("$.favouriteStores[0].storeId").value(store.getId()))
                .andExpect(jsonPath("$.favouriteStores[0].isSelling").value(true));

        mockMvc.perform(post("/api/cart")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "breadId": %d,
                                  "quantity": 2
                                }
                                """.formatted(breadId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value(store.getName()))
                .andExpect(jsonPath("$.items[0].breadId").value(breadId))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        MvcResult orderResult = mockMvc.perform(post("/api/orders/cart")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", "order-key-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(5000))
                .andReturn();

        long orderId = json(orderResult).get("orderId").asLong();

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(orderId));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", "payment-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": %d,
                                  "amount": 5000
                                }
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(breadRepository.findById(breadId).orElseThrow().getRemainingQuantity()).isEqualTo(3);

        CartEntity cart = cartRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(cart.getStoreId()).isNull();
        assertThat(cartItemRepository.findByCartId(cart.getId())).isEmpty();
    }
}
