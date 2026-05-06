package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.bread.service.BreadImageService;
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
import com.todaybread.server.domain.payment.service.PaymentService;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 주문 서비스 계층입니다.
 * 주문 생성, 취소, 확정, 조회를 담당합니다.
 */
@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final BreadRepository breadRepository;
    private final StoreRepository storeRepository;
    private final InventoryRestorer inventoryRestorer;
    private final OrderNumberGenerator orderNumberGenerator;
    private final BreadImageService breadImageService;
    private final Clock clock;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartService cartService,
                        BreadRepository breadRepository,
                        StoreRepository storeRepository,
                        InventoryRestorer inventoryRestorer,
                        OrderNumberGenerator orderNumberGenerator,
                        BreadImageService breadImageService,
                        Clock clock,
                        @Lazy PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartService = cartService;
        this.breadRepository = breadRepository;
        this.storeRepository = storeRepository;
        this.inventoryRestorer = inventoryRestorer;
        this.orderNumberGenerator = orderNumberGenerator;
        this.breadImageService = breadImageService;
        this.clock = clock;
        this.paymentService = paymentService;
    }

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
            if (bread == null || bread.isDeleted()) {
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

        // 6-1. 주문 번호 발급
        LocalDate orderDate = LocalDate.now(clock);
        order.setOrderDate(orderDate);
        String orderNumber = orderNumberGenerator.generate(order.getStoreId(), orderDate);
        order.assignOrderNumber(orderNumber);
        orderRepository.save(order);

        // 7. Cart 비우기
        cartService.clearCart(userId);

        // 8. 응답 생성
        StoreEntity store = storeRepository.findById(order.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        // 빵 대표 이미지 URL 조회
        Map<Long, String> breadImageUrlMap = breadImageService.getImageUrls(breadIds);

        return OrderDetailResponse.of(order, store.getName(),
                orderItems.stream()
                        .map(item -> OrderItemResponse.of(item, breadImageUrlMap.get(item.getBreadId())))
                        .toList());
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

        // 삭제된 빵 차단
        if (bread.isDeleted()) {
            throw new CustomException(ErrorCode.BREAD_NOT_FOUND);
        }

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

        // 주문 번호 발급
        LocalDate orderDate = LocalDate.now(clock);
        order.setOrderDate(orderDate);
        String orderNumber = orderNumberGenerator.generate(order.getStoreId(), orderDate);
        order.assignOrderNumber(orderNumber);
        orderRepository.save(order);

        StoreEntity store = storeRepository.findById(bread.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        // 빵 대표 이미지 URL 조회
        String breadImageUrl = breadImageService.getImageUrl(bread.getId());

        return OrderDetailResponse.of(order, store.getName(), List.of(OrderItemResponse.of(orderItem, breadImageUrl)));
    }

    /**
     * 주문을 취소합니다.
     * 비관적 락으로 주문을 조회한 뒤 상태에 따라 적절한 취소 경로를 선택합니다.
     * - CONFIRMED: PaymentService.cancelPayment()를 호출하여 결제 취소 + 주문 취소 + 재고 복원
     * - PENDING: 즉시 CANCELLED로 전환 + 재고 복원
     * - 그 외: ORDER_STATUS_CANNOT_CHANGE 예외
     *
     * <p>CONFIRMED 경로는 PaymentService의 2단계 취소 패턴을 사용하므로
     * 이 메서드 자체는 @Transactional을 선언하지 않습니다.
     * PENDING 경로는 내부에서 cancelPendingOrder()를 호출하여 트랜잭션을 처리합니다.
     *
     * @param userId  유저 ID
     * @param orderId 주문 ID
     */
    public void cancelOrder(Long userId, Long orderId) {
        // 먼저 소유자 검증 + 상태 확인 (짧은 트랜잭션)
        OrderStatus status = verifyCancelEligibility(userId, orderId);

        if (status == OrderStatus.CONFIRMED) {
            // CONFIRMED 상태: 결제 취소 필요 — PaymentService에 위임 (2단계 취소 패턴)
            paymentService.cancelPayment(userId, orderId);
        } else if (status == OrderStatus.PENDING) {
            // PENDING 상태: 결제 취소 불필요, 즉시 취소 + 재고 복원
            cancelPendingOrder(userId, orderId);
        } else {
            throw new CustomException(ErrorCode.ORDER_STATUS_CANNOT_CHANGE);
        }
    }

    /**
     * 주문 취소 가능 여부를 검증합니다.
     * 비관적 락으로 주문을 조회하고 소유자 검증 후 현재 상태를 반환합니다.
     *
     * @param userId  유저 ID
     * @param orderId 주문 ID
     * @return 현재 주문 상태
     */
    @Transactional
    public OrderStatus verifyCancelEligibility(Long userId, Long orderId) {
        OrderEntity order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        OrderStatus status = order.getStatus();
        if (status != OrderStatus.PENDING && status != OrderStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.ORDER_STATUS_CANNOT_CHANGE);
        }

        return status;
    }

    /**
     * PENDING 상태 주문을 취소하고 재고를 복원합니다.
     *
     * @param userId  유저 ID
     * @param orderId 주문 ID
     */
    @Transactional
    public void cancelPendingOrder(Long userId, Long orderId) {
        OrderEntity order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 상태가 이미 변경되었을 수 있으므로 재확인
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new CustomException(ErrorCode.ORDER_STATUS_CANNOT_CHANGE);
        }

        order.updateStatus(OrderStatus.CANCELLED);

        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);
        inventoryRestorer.restoreInventory(orderId, orderItems);

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
        OrderEntity order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
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
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        StoreEntity store = storeRepository.findById(order.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);

        // 빵 대표 이미지 URL 일괄 조회
        List<Long> breadIds = orderItems.stream()
                .map(OrderItemEntity::getBreadId)
                .filter(id -> id != null)
                .toList();
        Map<Long, String> breadImageUrlMap = breadImageService.getImageUrls(breadIds);

        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> OrderItemResponse.of(item, breadImageUrlMap.get(item.getBreadId())))
                .toList();

        return OrderDetailResponse.of(order, store.getName(), itemResponses);
    }

    /** 기존 주문이 있으면 상세 응답을 반환합니다. */
    private Optional<OrderDetailResponse> findExistingOrderDetail(Long userId, String idempotencyKey) {
        return orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(order -> getOrderDetail(userId, order.getId()));
    }
}
