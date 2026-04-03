package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.cart.entity.CartEntity;
import com.todaybread.server.domain.cart.entity.CartItemEntity;
import com.todaybread.server.domain.cart.repository.CartItemRepository;
import com.todaybread.server.domain.cart.repository.CartRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 주문 서비스 계층입니다.
 * 주문 생성, 취소, 확정, 조회를 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final BreadRepository breadRepository;
    private final StoreRepository storeRepository;

    /**
     * 장바구니 기반 주문을 생성합니다.
     * 비관적 락으로 빵을 조회하고, 재고 확인 및 차감, Order/OrderItem 생성, Cart 비우기를 수행합니다.
     *
     * @param userId 유저 ID
     * @return 주문 상세 응답
     */
    @Transactional
    public OrderDetailResponse createOrderFromCart(Long userId) {
        // 1. Cart 조회 및 비어있는지 확인
        CartEntity cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_EMPTY));

        List<CartItemEntity> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new CustomException(ErrorCode.CART_EMPTY);
        }

        // 2. 비관적 락으로 빵 일괄 조회
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

        // 4. 재고 차감 (모든 검증 통과 후)
        for (CartItemEntity cartItem : cartItems) {
            BreadEntity bread = breadMap.get(cartItem.getBreadId());
            bread.decreaseQuantity(cartItem.getQuantity());
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
                .build();
        orderRepository.save(order);

        // 6. OrderItem 생성 (스냅샷)
        List<OrderItemEntity> orderItems = new ArrayList<>();
        for (CartItemEntity cartItem : cartItems) {
            BreadEntity bread = breadMap.get(cartItem.getBreadId());
            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .orderId(order.getId())
                    .breadId(bread.getId())
                    .breadName(bread.getName())
                    .breadPrice(bread.getSalePrice())
                    .quantity(cartItem.getQuantity())
                    .build();
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);

        // 7. Cart 비우기
        cartService.clearCart(userId);

        // 8. 응답 생성
        StoreEntity store = storeRepository.findById(order.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(OrderItemResponse::of)
                .toList();

        return OrderDetailResponse.of(order, store.getName(), itemResponses);
    }

    /**
     * 바로 구매 주문을 생성합니다.
     * 비관적 락으로 빵을 조회하고, 재고 확인 및 차감, Order/OrderItem 생성을 수행합니다.
     *
     * @param userId  유저 ID
     * @param request 바로 구매 요청
     * @return 주문 상세 응답
     */
    @Transactional
    public OrderDetailResponse createDirectOrder(Long userId, DirectOrderRequest request) {
        // 1. 비관적 락으로 빵 조회
        List<BreadEntity> breads = breadRepository.findAllByIdWithLock(List.of(request.breadId()));
        if (breads.isEmpty()) {
            throw new CustomException(ErrorCode.BREAD_NOT_FOUND);
        }
        BreadEntity bread = breads.get(0);

        // 2. 재고 확인 및 차감
        if (bread.getRemainingQuantity() < request.quantity()) {
            throw new CustomException(ErrorCode.BREAD_INSUFFICIENT_QUANTITY);
        }
        bread.decreaseQuantity(request.quantity());

        // 3. Order 생성
        int totalAmount = bread.getSalePrice() * request.quantity();

        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .storeId(bread.getStoreId())
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .build();
        orderRepository.save(order);

        // 4. OrderItem 생성 (스냅샷)
        OrderItemEntity orderItem = OrderItemEntity.builder()
                .orderId(order.getId())
                .breadId(bread.getId())
                .breadName(bread.getName())
                .breadPrice(bread.getSalePrice())
                .quantity(request.quantity())
                .build();
        orderItemRepository.save(orderItem);

        // 5. 응답 생성
        StoreEntity store = storeRepository.findById(bread.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        return OrderDetailResponse.of(order, store.getName(), List.of(OrderItemResponse.of(orderItem)));
    }

    /**
     * 주문을 취소합니다.
     * PENDING 상태 확인, 소유자 확인, CANCELLED 변경, 재고 복원을 수행합니다.
     *
     * @param userId  유저 ID
     * @param orderId 주문 ID
     */
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        // 1. 주문 조회
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 소유자 확인
        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 3. PENDING → CANCELLED (updateStatus가 PENDING 가드 처리)
        order.updateStatus(OrderStatus.CANCELLED);

        // 4. 재고 복원
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItemEntity item : orderItems) {
            if (item.getBreadId() != null) {
                BreadEntity bread = breadRepository.findById(item.getBreadId())
                        .orElse(null);
                if (bread != null) {
                    bread.increaseQuantity(item.getQuantity());
                }
            }
        }
    }

    /**
     * 주문을 확정합니다. (PaymentService에서 호출)
     * PENDING → CONFIRMED 상태 변경을 수행합니다.
     *
     * @param orderId 주문 ID
     */
    @Transactional
    public void confirmOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        order.updateStatus(OrderStatus.CONFIRMED);
    }

    /**
     * 유저의 주문 목록을 최신순으로 조회합니다.
     *
     * @param userId 유저 ID
     * @return 주문 응답 목록
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(Long userId) {
        List<OrderEntity> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // 매장 ID 일괄 조회
        List<Long> storeIds = orders.stream()
                .map(OrderEntity::getStoreId)
                .distinct()
                .toList();

        Map<Long, StoreEntity> storeMap = storeRepository.findAllById(storeIds).stream()
                .collect(Collectors.toMap(StoreEntity::getId, Function.identity()));

        return orders.stream()
                .map(order -> {
                    StoreEntity store = storeMap.get(order.getStoreId());
                    String storeName = store != null ? store.getName() : null;
                    return OrderResponse.of(order, storeName);
                })
                .toList();
    }

    /**
     * 주문 상세를 조회합니다. 소유자 확인을 수행합니다.
     *
     * @param userId  유저 ID
     * @param orderId 주문 ID
     * @return 주문 상세 응답
     */
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        // 1. 주문 조회
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 소유자 확인
        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 3. 매장 이름 조회
        StoreEntity store = storeRepository.findById(order.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        // 4. 주문 항목 조회
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(OrderItemResponse::of)
                .toList();

        return OrderDetailResponse.of(order, store.getName(), itemResponses);
    }
}
