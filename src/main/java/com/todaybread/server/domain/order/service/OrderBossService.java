package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.order.dto.BossOrderResponse;
import com.todaybread.server.domain.order.dto.DailySalesEntry;
import com.todaybread.server.domain.order.dto.DailySalesProjection;
import com.todaybread.server.domain.order.dto.MonthlySalesResponse;
import com.todaybread.server.domain.order.dto.OrderItemResponse;
import com.todaybread.server.domain.order.dto.SalesAggregateProjection;
import com.todaybread.server.domain.order.dto.SalesItemResponse;
import com.todaybread.server.domain.order.dto.SalesSummaryResponse;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 사장님 주문/매출 관리 서비스입니다.
 * 픽업 대기 주문 조회, 픽업 완료 처리, 일별/월별 매출 조회를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderBossService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StoreRepository storeRepository;

    /**
     * 픽업 대기 주문 목록을 페이지네이션으로 조회합니다.
     * CONFIRMED 상태의 주문만 생성 시각 내림차순으로 반환합니다.
     *
     * @param userId   사장님 유저 ID
     * @param pageable 페이지 정보
     * @return 사장님 주문내역 응답 페이지
     */
    public Page<BossOrderResponse> getConfirmedOrders(Long userId, Pageable pageable) {
        Long storeId = getStoreIdByUserId(userId);

        Page<OrderEntity> orderPage = orderRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(
                storeId, OrderStatus.CONFIRMED, pageable);

        // batch 조회: 주문 ID 목록으로 한 번에 item 조회 후 메모리에서 그룹핑
        List<Long> orderIds = orderPage.getContent().stream()
                .map(OrderEntity::getId)
                .toList();

        Map<Long, List<OrderItemEntity>> itemsByOrderId = orderIds.isEmpty()
                ? Collections.emptyMap()
                : orderItemRepository.findByOrderIdIn(orderIds).stream()
                        .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));

        return orderPage.map(order -> {
            List<OrderItemResponse> itemResponses = itemsByOrderId
                    .getOrDefault(order.getId(), Collections.emptyList())
                    .stream()
                    .map(OrderItemResponse::of)
                    .toList();
            return new BossOrderResponse(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getTotalAmount(),
                    order.getCreatedAt(),
                    itemResponses
            );
        });
    }

    /**
     * 픽업 완료 처리를 수행합니다.
     * CONFIRMED 상태의 주문을 PICKED_UP으로 변경합니다.
     *
     * @param userId  사장님 유저 ID
     * @param orderId 주문 ID
     */
    @Transactional
    public void pickupOrder(Long userId, Long orderId) {
        Long storeId = getStoreIdByUserId(userId);

        OrderEntity order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getStoreId().equals(storeId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        order.updateStatus(OrderStatus.PICKED_UP);
    }

    /**
     * 일별 매출을 조회합니다.
     * 해당 날짜의 CONFIRMED + PICKED_UP 주문에 대해 메뉴별 판매 수량과 매출을 집계합니다.
     *
     * @param userId 사장님 유저 ID
     * @param date   조회 날짜
     * @return 매출 요약 응답 (총 매출 + 메뉴별 항목)
     */
    public SalesSummaryResponse getDailySales(Long userId, LocalDate date) {
        Long storeId = getStoreIdByUserId(userId);

        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();

        List<SalesAggregateProjection> projections = orderItemRepository.aggregateSales(
                storeId, List.of(OrderStatus.CONFIRMED, OrderStatus.PICKED_UP),
                startDateTime, endDateTime);

        List<SalesItemResponse> items = projections.stream()
                .map(SalesItemResponse::of)
                .toList();
        return SalesSummaryResponse.of(items);
    }

    /**
     * 월별 매출을 조회합니다.
     * 해당 월의 CONFIRMED + PICKED_UP 주문에 대해 일별 매출 합산과 메뉴별 판매 수량/매출을 집계합니다.
     *
     * @param userId 사장님 유저 ID
     * @param year   조회 연도
     * @param month  조회 월
     * @return 월별 매출 응답 (총 매출 + 일별 합산 + 메뉴별 항목)
     */
    public MonthlySalesResponse getMonthlySales(Long userId, int year, int month) {
        Long storeId = getStoreIdByUserId(userId);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = startDate.plusMonths(1).atStartOfDay();

        List<OrderStatus> statuses = List.of(OrderStatus.CONFIRMED, OrderStatus.PICKED_UP);

        // 메뉴별 집계
        List<SalesAggregateProjection> projections = orderItemRepository.aggregateSales(
                storeId, statuses, startDateTime, endDateTime);

        List<SalesItemResponse> items = projections.stream()
                .map(SalesItemResponse::of)
                .toList();

        // 일별 매출 합산
        List<DailySalesProjection> dailyProjections = orderRepository.aggregateDailySales(
                storeId, statuses, startDateTime, endDateTime);

        List<DailySalesEntry> dailySales = dailyProjections.stream()
                .map(p -> new DailySalesEntry(p.getSalesDate(), p.getTotalSales()))
                .toList();

        long totalSales = items.stream().mapToLong(SalesItemResponse::totalSales).sum();
        long totalQuantity = items.stream().mapToLong(SalesItemResponse::totalQuantity).sum();

        return new MonthlySalesResponse(totalSales, totalQuantity, dailySales, items);
    }

    /**
     * userId로 가게 ID를 조회합니다.
     * 가게가 등록되지 않은 경우 STORE_NOT_FOUND 예외를 발생시킵니다.
     *
     * @param userId 유저 ID
     * @return 가게 ID
     */
    private Long getStoreIdByUserId(Long userId) {
        StoreEntity store = storeRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
        return store.getId();
    }
}
