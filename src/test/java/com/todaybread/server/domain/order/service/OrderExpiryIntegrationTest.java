package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderExpiryService 통합 테스트입니다.
 * 실제 H2 DB를 사용하여 만료 주문 처리의 전체 흐름을 검증합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderExpiryIntegrationTest {

    @Autowired
    private OrderExpiryService orderExpiryService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private BreadRepository breadRepository;

    @Autowired
    private Clock clock;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void processExpiredOrders_cancelsExpiredPendingAndRestoresInventory() {
        // Given: 현재 시각 기준 cutoffTime = now - 10분
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiredTime = now.minusMinutes(30);   // 만료된 시각 (30분 전)
        LocalDateTime recentTime = now.minusMinutes(5);     // 만료되지 않은 시각 (5분 전)

        // 트랜잭션 내에서 테스트 데이터 생성 및 createdAt 강제 설정
        Long[] orderIds = transactionTemplate.execute(status -> {
            // 빵 엔티티 생성 (재고 확인용)
            BreadEntity bread1 = BreadEntity.builder()
                    .storeId(1L)
                    .name("식빵")
                    .description("맛있는 식빵")
                    .originalPrice(3000)
                    .salePrice(2000)
                    .remainingQuantity(10)
                    .build();
            BreadEntity bread2 = BreadEntity.builder()
                    .storeId(1L)
                    .name("바게트")
                    .description("바삭한 바게트")
                    .originalPrice(4000)
                    .salePrice(3000)
                    .remainingQuantity(5)
                    .build();
            breadRepository.save(bread1);
            breadRepository.save(bread2);

            // 만료된 PENDING 주문 1
            OrderEntity expiredOrder1 = OrderEntity.builder()
                    .userId(1L)
                    .storeId(1L)
                    .status(OrderStatus.PENDING)
                    .totalAmount(4000)
                    .idempotencyKey("expired-1")
                    .build();
            orderRepository.save(expiredOrder1);

            OrderItemEntity item1 = OrderItemEntity.builder()
                    .orderId(expiredOrder1.getId())
                    .breadId(bread1.getId())
                    .breadName("식빵")
                    .breadPrice(2000)
                    .quantity(2)
                    .build();
            orderItemRepository.save(item1);

            // 만료된 PENDING 주문 2
            OrderEntity expiredOrder2 = OrderEntity.builder()
                    .userId(2L)
                    .storeId(1L)
                    .status(OrderStatus.PENDING)
                    .totalAmount(3000)
                    .idempotencyKey("expired-2")
                    .build();
            orderRepository.save(expiredOrder2);

            OrderItemEntity item2 = OrderItemEntity.builder()
                    .orderId(expiredOrder2.getId())
                    .breadId(bread2.getId())
                    .breadName("바게트")
                    .breadPrice(3000)
                    .quantity(1)
                    .build();
            orderItemRepository.save(item2);

            // 만료되지 않은 PENDING 주문
            OrderEntity recentOrder = OrderEntity.builder()
                    .userId(3L)
                    .storeId(1L)
                    .status(OrderStatus.PENDING)
                    .totalAmount(2000)
                    .idempotencyKey("recent-1")
                    .build();
            orderRepository.save(recentOrder);

            // CONFIRMED 주문 (만료 시각이지만 상태가 다름)
            OrderEntity confirmedOrder = OrderEntity.builder()
                    .userId(4L)
                    .storeId(1L)
                    .status(OrderStatus.CONFIRMED)
                    .totalAmount(5000)
                    .idempotencyKey("confirmed-1")
                    .build();
            orderRepository.save(confirmedOrder);

            // flush 후 native query로 createdAt 강제 설정 (JPA auditing 우회)
            entityManager.flush();

            entityManager.createNativeQuery("UPDATE orders SET created_at = :ts WHERE id = :id")
                    .setParameter("ts", expiredTime)
                    .setParameter("id", expiredOrder1.getId())
                    .executeUpdate();

            entityManager.createNativeQuery("UPDATE orders SET created_at = :ts WHERE id = :id")
                    .setParameter("ts", expiredTime)
                    .setParameter("id", expiredOrder2.getId())
                    .executeUpdate();

            entityManager.createNativeQuery("UPDATE orders SET created_at = :ts WHERE id = :id")
                    .setParameter("ts", recentTime)
                    .setParameter("id", recentOrder.getId())
                    .executeUpdate();

            entityManager.createNativeQuery("UPDATE orders SET created_at = :ts WHERE id = :id")
                    .setParameter("ts", expiredTime)
                    .setParameter("id", confirmedOrder.getId())
                    .executeUpdate();

            return new Long[]{
                    expiredOrder1.getId(), expiredOrder2.getId(),
                    recentOrder.getId(), confirmedOrder.getId(),
                    bread1.getId(), bread2.getId()
            };
        });

        Long expiredOrder1Id = orderIds[0];
        Long expiredOrder2Id = orderIds[1];
        Long recentOrderId = orderIds[2];
        Long confirmedOrderId = orderIds[3];
        Long bread1Id = orderIds[4];
        Long bread2Id = orderIds[5];

        // When
        int cancelledCount = orderExpiryService.processExpiredOrders();

        // Then: 만료된 PENDING 주문 2건만 취소됨
        assertThat(cancelledCount).isEqualTo(2);

        // 만료된 PENDING 주문들은 CANCELLED 상태
        OrderEntity updatedExpired1 = orderRepository.findById(expiredOrder1Id).orElseThrow();
        assertThat(updatedExpired1.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        OrderEntity updatedExpired2 = orderRepository.findById(expiredOrder2Id).orElseThrow();
        assertThat(updatedExpired2.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 만료되지 않은 PENDING 주문은 그대로 PENDING
        OrderEntity updatedRecent = orderRepository.findById(recentOrderId).orElseThrow();
        assertThat(updatedRecent.getStatus()).isEqualTo(OrderStatus.PENDING);

        // CONFIRMED 주문은 그대로 CONFIRMED
        OrderEntity updatedConfirmed = orderRepository.findById(confirmedOrderId).orElseThrow();
        assertThat(updatedConfirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // 재고 복원 확인: bread1은 +2, bread2는 +1
        BreadEntity updatedBread1 = breadRepository.findById(bread1Id).orElseThrow();
        assertThat(updatedBread1.getRemainingQuantity()).isEqualTo(12); // 10 + 2

        BreadEntity updatedBread2 = breadRepository.findById(bread2Id).orElseThrow();
        assertThat(updatedBread2.getRemainingQuantity()).isEqualTo(6);  // 5 + 1
    }

    /**
     * REQUIRES_NEW 트랜잭션의 실패 격리를 검증합니다.
     * 한 주문의 재고 복원이 실패하면 해당 주문만 롤백되고, 다른 주문은 정상 취소됩니다.
     *
     * 시나리오:
     * - 만료된 PENDING 주문 2건 생성
     * - 주문2의 order_item quantity를 native SQL로 0으로 변경 → increaseQuantity(0) 예외 유발
     * - processExpiredOrders() 호출
     * - 주문1: 정상 취소 (CANCELLED), 재고 복원됨
     * - 주문2: 롤백 (PENDING 유지), 재고 변경 없음
     */
    @Test
    void processExpiredOrders_isolatesFailureAndRollsBackOnlyFailedOrder() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiredTime = now.minusMinutes(30);

        Long[] ids = transactionTemplate.execute(status -> {
            // 빵 엔티티 생성
            BreadEntity bread1 = BreadEntity.builder()
                    .storeId(1L)
                    .name("식빵")
                    .description("맛있는 식빵")
                    .originalPrice(3000)
                    .salePrice(2000)
                    .remainingQuantity(10)
                    .build();
            BreadEntity bread2 = BreadEntity.builder()
                    .storeId(1L)
                    .name("바게트")
                    .description("바삭한 바게트")
                    .originalPrice(4000)
                    .salePrice(3000)
                    .remainingQuantity(5)
                    .build();
            breadRepository.save(bread1);
            breadRepository.save(bread2);

            // 정상 만료 주문
            OrderEntity order1 = OrderEntity.builder()
                    .userId(1L)
                    .storeId(1L)
                    .status(OrderStatus.PENDING)
                    .totalAmount(4000)
                    .idempotencyKey("iso-1")
                    .build();
            orderRepository.save(order1);

            OrderItemEntity item1 = OrderItemEntity.builder()
                    .orderId(order1.getId())
                    .breadId(bread1.getId())
                    .breadName("식빵")
                    .breadPrice(2000)
                    .quantity(2)
                    .build();
            orderItemRepository.save(item1);

            // 실패할 만료 주문 (quantity를 나중에 0으로 변경)
            OrderEntity order2 = OrderEntity.builder()
                    .userId(2L)
                    .storeId(1L)
                    .status(OrderStatus.PENDING)
                    .totalAmount(3000)
                    .idempotencyKey("iso-2")
                    .build();
            orderRepository.save(order2);

            OrderItemEntity item2 = OrderItemEntity.builder()
                    .orderId(order2.getId())
                    .breadId(bread2.getId())
                    .breadName("바게트")
                    .breadPrice(3000)
                    .quantity(1)
                    .build();
            orderItemRepository.save(item2);

            entityManager.flush();

            // createdAt을 만료 시각으로 설정
            entityManager.createNativeQuery("UPDATE orders SET created_at = :ts WHERE id = :id")
                    .setParameter("ts", expiredTime)
                    .setParameter("id", order1.getId())
                    .executeUpdate();

            entityManager.createNativeQuery("UPDATE orders SET created_at = :ts WHERE id = :id")
                    .setParameter("ts", expiredTime)
                    .setParameter("id", order2.getId())
                    .executeUpdate();

            // item2의 quantity를 0으로 변경 → increaseQuantity(0) 예외 유발
            entityManager.createNativeQuery("UPDATE order_item SET quantity = 0 WHERE id = :id")
                    .setParameter("id", item2.getId())
                    .executeUpdate();

            return new Long[]{order1.getId(), order2.getId(), bread1.getId(), bread2.getId()};
        });

        Long order1Id = ids[0];
        Long order2Id = ids[1];
        Long bread1Id = ids[2];
        Long bread2Id = ids[3];

        // When
        int cancelledCount = orderExpiryService.processExpiredOrders();

        // Then: 정상 주문 1건만 취소됨
        assertThat(cancelledCount).isEqualTo(1);

        // 주문1: 정상 취소 (CANCELLED)
        OrderEntity updatedOrder1 = orderRepository.findById(order1Id).orElseThrow();
        assertThat(updatedOrder1.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 주문2: 롤백으로 PENDING 유지
        OrderEntity updatedOrder2 = orderRepository.findById(order2Id).orElseThrow();
        assertThat(updatedOrder2.getStatus()).isEqualTo(OrderStatus.PENDING);

        // bread1: 재고 복원됨 (10 + 2 = 12)
        BreadEntity updatedBread1 = breadRepository.findById(bread1Id).orElseThrow();
        assertThat(updatedBread1.getRemainingQuantity()).isEqualTo(12);

        // bread2: 재고 변경 없음 (롤백, 5 유지)
        BreadEntity updatedBread2 = breadRepository.findById(bread2Id).orElseThrow();
        assertThat(updatedBread2.getRemainingQuantity()).isEqualTo(5);
    }
}
