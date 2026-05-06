package com.todaybread.server.domain.cart.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.bread.service.BreadImageService;
import com.todaybread.server.domain.cart.dto.CartAddRequest;
import com.todaybread.server.domain.cart.dto.CartItemResponse;
import com.todaybread.server.domain.cart.dto.CartResponse;
import com.todaybread.server.domain.cart.dto.CartUpdateRequest;
import com.todaybread.server.domain.cart.entity.CartEntity;
import com.todaybread.server.domain.cart.entity.CartItemEntity;
import com.todaybread.server.domain.cart.repository.CartItemRepository;
import com.todaybread.server.domain.cart.repository.CartRepository;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 장바구니 서비스 계층입니다.
 * 장바구니 CRUD 및 단일 매장 제약 검증을 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BreadRepository breadRepository;
    private final BreadImageService breadImageService;
    private final StoreRepository storeRepository;
    private final StoreBusinessHoursRepository storeBusinessHoursRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * 장바구니에 빵을 추가합니다.
     * 빵 존재 확인, 재고 검증, 단일 매장 제약 검증, 동일 빵 수량 누적을 수행합니다.
     *
     * @param userId  유저 ID
     * @param request 장바구니 추가 요청
     */
    @Transactional
    public void addToCart(Long userId, CartAddRequest request) {
        // 1. 빵 존재 확인 (삭제된 빵은 장바구니에 추가할 수 없음)
        BreadEntity bread = breadRepository.findByIdAndIsDeletedFalse(request.breadId())
                .orElseThrow(() -> new CustomException(ErrorCode.BREAD_NOT_FOUND));

        // 2. Cart를 락으로 조회 또는 생성
        CartEntity cart = getOrCreateCartByUserIdWithLock(userId);

        // 3. 단일 매장 제약 검증
        if (cart.getStoreId() != null && !cart.getStoreId().equals(bread.getStoreId())) {
            throw new CustomException(ErrorCode.CART_SINGLE_STORE_ONLY);
        }

        // 4. 동일 빵 수량 누적 또는 새 항목 추가
        Optional<CartItemEntity> existingItem = cartItemRepository
                .findByCartIdAndBreadId(cart.getId(), bread.getId());

        int totalQuantity;
        if (existingItem.isPresent()) {
            totalQuantity = existingItem.get().getQuantity() + request.quantity();
        } else {
            totalQuantity = request.quantity();
        }

        // 5. 재고 검증
        if (totalQuantity > bread.getRemainingQuantity()) {
            throw new CustomException(ErrorCode.BREAD_INSUFFICIENT_QUANTITY);
        }

        // 6. 수량 누적 또는 새 항목 저장
        if (existingItem.isPresent()) {
            existingItem.get().increaseQuantity(request.quantity());
        } else {
            CartItemEntity newItem = CartItemEntity.builder()
                    .cartId(cart.getId())
                    .breadId(bread.getId())
                    .quantity(request.quantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        // 7. 매장 ID 설정 (첫 번째 빵 추가 시)
        if (cart.getStoreId() == null) {
            cart.updateStoreId(bread.getStoreId());
        }
    }

    /**
     * 장바구니를 조회합니다.
     * Cart_Item 목록과 매장 이름, 오늘 라스트오더 시간을 반환합니다.
     * 삭제된 빵 항목은 자동으로 정리됩니다.
     *
     * @param userId 유저 ID
     * @return 장바구니 응답
     */
    @Transactional
    public CartResponse getCart(Long userId) {
        // 1. Cart 조회
        Optional<CartEntity> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isEmpty() || cartOpt.get().getStoreId() == null) {
            return new CartResponse(null, null, List.of());
        }

        CartEntity cart = cartOpt.get();
        List<CartItemEntity> cartItems = cartItemRepository.findByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            return new CartResponse(null, null, List.of());
        }

        // 2. 빵 ID 목록 추출 및 빵/이미지 일괄 조회
        List<Long> breadIds = new ArrayList<>();
        for (CartItemEntity item : cartItems) {
            breadIds.add(item.getBreadId());
        }
        Map<Long, String> imageUrlMap = breadImageService.getImageUrls(breadIds);
        Map<Long, BreadEntity> breadMap = breadRepository.findAllById(breadIds).stream()
                .collect(java.util.stream.Collectors.toMap(BreadEntity::getId, b -> b));

        // 3. 삭제된 빵 항목 자동 정리
        List<CartItemEntity> validItems = new ArrayList<>();
        List<CartItemEntity> deletedItems = new ArrayList<>();

        for (CartItemEntity item : cartItems) {
            BreadEntity bread = breadMap.get(item.getBreadId());
            if (bread == null || bread.isDeleted()) {
                deletedItems.add(item);
            } else {
                validItems.add(item);
            }
        }

        if (!deletedItems.isEmpty()) {
            cartItemRepository.deleteAll(deletedItems);

            if (validItems.isEmpty()) {
                cart.updateStoreId(null);
                return new CartResponse(null, null, List.of());
            }
        }

        // 4. 유효한 항목으로 응답 생성
        List<CartItemResponse> itemResponses = new ArrayList<>();
        for (CartItemEntity item : validItems) {
            BreadEntity bread = breadMap.get(item.getBreadId());
            String imageUrl = imageUrlMap.get(bread.getId());
            itemResponses.add(CartItemResponse.of(item, bread, imageUrl));
        }

        // 5. 매장 이름 조회
        StoreEntity store = storeRepository.findById(cart.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        // 6. 오늘 라스트오더 시간 조회
        int todayDow = LocalDate.now(clock).getDayOfWeek().getValue();
        LocalTime lastOrderTime = storeBusinessHoursRepository
                .findByStoreIdAndDayOfWeek(store.getId(), todayDow)
                .map(StoreBusinessHoursEntity::getLastOrderTime)
                .orElse(null);

        return new CartResponse(store.getName(), lastOrderTime, itemResponses);
    }

    /**
     * 장바구니 항목의 수량을 변경합니다.
     *
     * @param userId     유저 ID
     * @param cartItemId 장바구니 항목 ID
     * @param request    수량 변경 요청
     */
    @Transactional
    public void updateQuantity(Long userId, Long cartItemId, CartUpdateRequest request) {
        CartEntity cart = getCartByUserIdWithLock(userId);

        CartItemEntity cartItem = cartItemRepository.findById(cartItemId)
                .filter(item -> item.getCartId().equals(cart.getId()))
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        // 재고 검증 (삭제된 빵은 수량 변경 불가)
        BreadEntity bread = breadRepository.findByIdAndIsDeletedFalse(cartItem.getBreadId())
                .orElseThrow(() -> new CustomException(ErrorCode.BREAD_NOT_FOUND));

        if (request.quantity() > bread.getRemainingQuantity()) {
            throw new CustomException(ErrorCode.BREAD_INSUFFICIENT_QUANTITY);
        }

        cartItem.updateQuantity(request.quantity());
    }

    /**
     * 장바구니에서 항목을 삭제합니다.
     * 마지막 항목 삭제 시 Cart의 storeId를 null로 초기화합니다.
     *
     * @param userId     유저 ID
     * @param cartItemId 장바구니 항목 ID
     */
    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        CartEntity cart = getCartByUserIdWithLock(userId);

        CartItemEntity cartItem = cartItemRepository.findById(cartItemId)
                .filter(item -> item.getCartId().equals(cart.getId()))
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItemRepository.delete(cartItem);

        // 마지막 항목이었는지 확인
        List<CartItemEntity> remaining = cartItemRepository.findByCartId(cart.getId());
        if (remaining.isEmpty()) {
            cart.updateStoreId(null);
        }
    }

    /**
     * 장바구니를 비웁니다.
     * 모든 항목을 삭제하고 매장 제약을 해제합니다.
     *
     * @param userId 유저 ID
     */
    @Transactional
    public void clearCart(Long userId) {
        Optional<CartEntity> cartOpt = cartRepository.findByUserIdWithLock(userId);
        if (cartOpt.isEmpty()) {
            return;
        }

        CartEntity cart = cartOpt.get();
        cartItemRepository.deleteByCartId(cart.getId());
        cart.updateStoreId(null);
    }

    /**
     * 유저 ID로 Cart를 비관적 락으로 조회합니다. 없으면 CART_003 예외를 던집니다.
     */
    private CartEntity getCartByUserIdWithLock(Long userId) {
        return cartRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_EMPTY));
    }

    /**
     * checkout용: Cart와 CartItem을 비관적 락으로 조회합니다.
     * Cart가 없거나 항목이 비어 있으면 CART_EMPTY 예외를 던집니다.
     *
     * @param userId 유저 ID
     * @return Cart 엔티티와 CartItem 목록
     */
    public CartWithItems getCartWithItemsForCheckout(Long userId) {
        CartEntity cart = cartRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_EMPTY));

        List<CartItemEntity> items = cartItemRepository.findByCartIdWithLock(cart.getId());
        if (items.isEmpty()) {
            throw new CustomException(ErrorCode.CART_EMPTY);
        }

        return new CartWithItems(cart, items);
    }

    /**
     * checkout 결과를 담는 레코드입니다.
     *
     * @param cart  장바구니 엔티티
     * @param items 장바구니 항목 목록
     */
    public record CartWithItems(CartEntity cart, List<CartItemEntity> items) {
    }

    /**
     * 유저의 장바구니를 락으로 조회합니다. 없으면 유저 행을 잠근 뒤 안전하게 생성합니다.
     */
    private CartEntity getOrCreateCartByUserIdWithLock(Long userId) {
        Optional<CartEntity> existingCart = cartRepository.findByUserIdWithLock(userId);
        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        UserEntity user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return cartRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> cartRepository.save(
                        CartEntity.builder().userId(user.getId()).storeId(null).build()));
    }
}
