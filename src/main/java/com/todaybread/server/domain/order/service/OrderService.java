package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.cart.entity.CartEntity;
import com.todaybread.server.domain.cart.entity.CartItemEntity;
import com.todaybread.server.domain.cart.service.CartService;
import com.todaybread.server.domain.order.dto.DirectOrderRequest;
import com.todaybread.server.domain.order.dto.OrderDetailResponse;
import com.todaybread.server.domain.order.dto.OrderItemResponse;
import com.todaybread.server.domain.order.dto.OrderResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 주문 서비스 계층입니다.
 * 주문 생성, 취소, 확정, 조회를 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final BreadRepository breadRepository;
    private final StoreRepository storeRepository;

    /**
     * 장바구니 기반 주문을 생성합니다.
     * 장바구니 항목을 비관적 락으로 조회하고, 재고 차감 후 주문을 생성합니다.
     *
     * @param userId         유저 ID
     * @param idempotencyKey 멱등성 키
     * @return 주문 상세 응답
     */
    @Transactional
    public OrderDetailResponse createOrderFromCart(Long userId, String idempotencyKey) {
        Optional<OrderDetailResponse> existingOrder = findExistingOrderDetail(userId, idempotencyKey);
        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }

        // 1. Cart + CartItem을 비관적 락으로 조회 (CartService에 위임)
        CartService.CartWithItems cartWithItems;
        try {
            cartWithItems = cartService.getCartWithItemsForCheckout(userId);
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.CART_EMPTY) {
                // 동시 same-key 요청이 먼저 완료하고 Cart를 비웠을 수 있음
                existingOrder = findExistingOrderDetail(userId, idempotencyKey);
                if (existingOrder.isPresent()) {
                    return existingOrder.get();
                }
            }
            throw e;
        }

        // 2. 락 획득 후 idempotency 재확인
        existingOrder = findExistingOrderDetail(userId, idempotencyKey);
        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }

        CartEntity cart = cartWithItems.cart();
        List<CartItemEntity> cartItems = cartWithItems.items();

        // 2. 비관적 락으로 빵 일괄 조회 (ORDER BY id로 데드락 방지)
        List<Long> breadIds = cartItems.stream()
                .map(CartItemEntity::getBreadId)
                .toList();

        List<BreadEntity> breads = breadRepository.findAllByIdWithLock(breadIds);
        Map<Long, BreadEntity> breadMap = breads.stream()
                .collect(Collectors.toMap(BreadEntity::getId, Function.identity()));

        // 3. 모든 빵 존재 확인 및 재고 확인 (차감 전 전체 검증)
        for (CartItemEntity cartItem : cartItems) {
            BreadEntity bread = breadMap.get(cartItem.getBreadId());
            if (bread == null) {
                throw new CustomException(ErrorCode.BREAD_NOT_FOUND);
            }
            if (bread.getRemainingQuantity() < cartItem.getQuantity()) {
                throw new CustomException(ErrorCode.BREAD_INSUFFICIENT_QUANTITY);
            }
        }

        // 4. 재고 차감
        for (CartItemEntity cartItem : cartItems) {
            breadMap.get(cartItem.getBreadId()).decreaseQuantity(cartItem.getQuantity());
        }

        // 5. Order 생성
        int totalAmount = cartItems.stream()
                .mapToInt(item -> breadMap.get(item.getBreadId()).getSalePrice() * item.getQuantity())
                .sum();

        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .storeId(cart.getStoreId())
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .idempotencyKey(idempotencyKey)
                .build();
        orderRepository.save(order);

        log.info("장바구니 기반 주문 생성: orderId={}, userId={}, totalAmount={}", order.getId(), userId, totalAmount);

        // 6. OrderItem 생성 (스냅샷)
        List<OrderItemEntity> orderItems = new ArrayList<>();
        for (CartItemEntity cartItem : cartItems) {
            BreadEntity bread = breadMap.get(cartItem.getBreadId());
            orderItems.add(OrderItemEntity.builder()
                    .orderId(order.getId())
                    .breadId(bread.getId())
                    .breadName(bread.getName())
                    .breadPrice(bread.getSalePrice())
                    .quantity(cartItem.getQuantity())
                    .build());
        }
        orderItemRepository.saveAll(orderItems);

        // 7. Cart 비우기
        cartService.clearCart(userId);

        // 8. 응답 생성
        Optional<StoreEntity> storeOpt = storeRepository.findById(order.getStoreId());
        if (storeOpt.isEmpty()) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }
        StoreEntity store = storeOpt.get();

        return OrderDetailResponse.of(order, store.getName(),
                orderItems.stream().map(OrderItemResponse::of).toList());
    }

    /**
     * 바로 구매 주문을 생성합니다.
     * 단일 빵에 대해 재고 차감 후 주문을 생성합니다.
     *
     * @param userId         유저 ID
     * @param request        바로 구매 요청
     * @param idempotencyKey 멱등성 키
     * @return 주문 상세 응답
     */
    @Transactional
    public OrderDetailResponse createDirectOrder(Long userId, DirectOrderRequest request, String idempotencyKey) {
        Optional<OrderDetailResponse> existingOrder = findExistingOrderDetail(userId, idempotencyKey);
        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }

        List<BreadEntity> breads = breadRepository.findAllByIdWithLock(List.of(request.breadId()));
        if (breads.isEmpty()) {
            throw new CustomException(ErrorCode.BREAD_NOT_FOUND);
        }
        BreadEntity bread = breads.get(0);

        existingOrder = findExistingOrderDetail(userId, idempotencyKey);
        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }

        if (bread.getRemainingQuantity() < request.quantity()) {
            throw new CustomException(ErrorCode.BREAD_INSUFFICIENT_QUANTITY);
        }
        bread.decreaseQuantity(request.quantity());

        int totalAmount = bread.getSalePrice() * request.quantity();

        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .storeId(bread.getStoreId())
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .idempotencyKey(idempotencyKey)
                .build();
        orderRepository.save(order);

        log.info("바로 구매 주문 생성: orderId={}, userId={}, breadId={}, totalAmount={}",
                order.getId(), userId, bread.getId(), totalAmount);

        OrderItemEntity orderItem = OrderItemEntity.builder()
                .orderId(order.getId())
                .breadId(bread.getId())
                .breadName(bread.getName())
                .breadPrice(bread.getSalePrice())
                .quantity(request.quantity())
                .build();
        orderItemRepository.save(orderItem);

        Optional<StoreEntity> storeOpt = storeRepository.findById(bread.getStoreId());
        if (storeOpt.isEmpty()) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }
        StoreEntity store = storeOpt.get();

        return OrderDetailResponse.of(order, store.getName(), List.of(OrderItemResponse.of(orderItem)));
    }

    /**
     * 주문을 취소합니다.
     * 비관적 락으로 주문을 조회하고, 재고를 복원합니다.
     *
     * @param userId  유저 ID
     * @param orderId 주문 ID
     */
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Optional<OrderEntity> orderOpt = orderRepository.findByIdWithLock(orderId);
        if (orderOpt.isEmpty()) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }
        OrderEntity order = orderOpt.get();

        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        order.updateStatus(OrderStatus.CANCELLED);

        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);
        List<Long> breadIds = orderItems.stream()
                .map(OrderItemEntity::getBreadId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (!breadIds.isEmpty()) {
            Map<Long, BreadEntity> breadMap = breadRepository.findAllByIdWithLock(breadIds).stream()
                    .collect(Collectors.toMap(BreadEntity::getId, Function.identity()));

            for (OrderItemEntity item : orderItems) {
                if (item.getBreadId() == null) continue;
                BreadEntity bread = breadMap.get(item.getBreadId());
                if (bread != null) {
                    bread.increaseQuantity(item.getQuantity());
                } else {
                    log.warn("주문 취소 시 빵을 찾을 수 없어 재고 복원 건너뜀: orderId={}, breadId={}", orderId, item.getBreadId());
                }
            }
        }

        log.info("주문 취소 완료: orderId={}, userId={}", orderId, userId);
    }

    /**
     * 주문을 확정합니다.
     * 결제 승인 후 호출되어 주문 상태를 CONFIRMED로 변경합니다.
     *
     * @param orderId 주문 ID
     */
    @Transactional
    public void confirmOrder(Long orderId) {
        Optional<OrderEntity> orderOpt = orderRepository.findByIdWithLock(orderId);
        if (orderOpt.isEmpty()) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }
        OrderEntity order = orderOpt.get();
        order.updateStatus(OrderStatus.CONFIRMED);
        log.info("주문 확정: orderId={}", orderId);
    }

    /**
     * 주문 내역 목록을 페이지네이션으로 조회합니다.
     *
     * @param userId   유저 ID
     * @param pageable 페이지 정보
     * @return 주문 응답 페이지
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(Long userId, Pageable pageable) {
        Page<OrderEntity> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<Long> storeIds = orders.getContent().stream()
                .map(OrderEntity::getStoreId)
                .distinct()
                .toList();

        Map<Long, StoreEntity> storeMap = storeRepository.findAllById(storeIds).stream()
                .collect(Collectors.toMap(StoreEntity::getId, Function.identity()));

        return orders.map(order -> {
            StoreEntity store = storeMap.get(order.getStoreId());
            String storeName = store != null ? store.getName() : null;
            return OrderResponse.of(order, storeName);
        });
    }

    /**
     * 주문 상세를 조회합니다.
     *
     * @param userId  유저 ID
     * @param orderId 주문 ID
     * @return 주문 상세 응답
     */
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }
        OrderEntity order = orderOpt.get();

        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        Optional<StoreEntity> storeOpt = storeRepository.findById(order.getStoreId());
        if (storeOpt.isEmpty()) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }
        StoreEntity store = storeOpt.get();

        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);
        return OrderDetailResponse.of(order, store.getName(),
                orderItems.stream().map(OrderItemResponse::of).toList());
    }

    /** 기존 주문이 있으면 상세 응답을 반환합니다. */
    private Optional<OrderDetailResponse> findExistingOrderDetail(Long userId, String idempotencyKey) {
        return orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(order -> getOrderDetail(userId, order.getId()));
    }
}
