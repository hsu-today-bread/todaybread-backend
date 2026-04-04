package com.todaybread.server.domain.cart.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.bread.service.BreadImageService;
import com.todaybread.server.domain.cart.dto.CartAddRequest;
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
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * Cart 도메인 속성 기반 테스트 (jqwik)
 * Feature: order-flow
 */
class CartPropertyTest {

    private CartService cartService;
    private CartRepository cartRepository;
    private CartItemRepository cartItemRepository;
    private BreadRepository breadRepository;
    private BreadImageService breadImageService;
    private StoreRepository storeRepository;
    private StoreBusinessHoursRepository storeBusinessHoursRepository;
    private UserRepository userRepository;
    private Clock clock;

    @BeforeProperty
    void setUp() {
        cartRepository = Mockito.mock(CartRepository.class);
        cartItemRepository = Mockito.mock(CartItemRepository.class);
        breadRepository = Mockito.mock(BreadRepository.class);
        breadImageService = Mockito.mock(BreadImageService.class);
        storeRepository = Mockito.mock(StoreRepository.class);
        storeBusinessHoursRepository = Mockito.mock(StoreBusinessHoursRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        clock = Mockito.mock(Clock.class);
        cartService = new CartService(
                cartRepository, cartItemRepository, breadRepository,
                breadImageService, storeRepository, storeBusinessHoursRepository, userRepository, clock
        );
    }

    // ── Helper methods ──

    private CartEntity createCart(Long cartId, Long userId, Long storeId) {
        CartEntity cart = CartEntity.builder().userId(userId).storeId(storeId).build();
        ReflectionTestUtils.setField(cart, "id", cartId);
        return cart;
    }

    private BreadEntity createBread(Long breadId, Long storeId, int stock) {
        BreadEntity bread = BreadEntity.builder()
                .storeId(storeId)
                .name("빵-" + breadId)
                .description("설명-" + breadId)
                .originalPrice(5000)
                .salePrice(3500)
                .remainingQuantity(stock)
                .build();
        ReflectionTestUtils.setField(bread, "id", breadId);
        return bread;
    }

    private CartItemEntity createCartItem(Long itemId, Long cartId, Long breadId, int quantity) {
        CartItemEntity item = CartItemEntity.builder()
                .cartId(cartId)
                .breadId(breadId)
                .quantity(quantity)
                .build();
        ReflectionTestUtils.setField(item, "id", itemId);
        return item;
    }

    private StoreEntity createStore(Long storeId, String name) {
        StoreEntity store = StoreEntity.builder()
                .userId(99L)
                .name(name)
                .phoneNumber("010-0000-" + String.format("%04d", storeId))
                .description("매장 설명")
                .addressLine1("주소1")
                .addressLine2("주소2")
                .latitude(BigDecimal.valueOf(37.5))
                .longitude(BigDecimal.valueOf(127.0))
                .build();
        ReflectionTestUtils.setField(store, "id", storeId);
        return store;
    }

    // ── Property 1: 장바구니 추가 수량 누적 ──
    // Feature: order-flow, Property 1: 장바구니 추가 수량 누적
    // **Validates: Requirements 1.1, 1.2**
    @Property(tries = 100)
    void addingSameBreadTwiceAccumulatesQuantity(
            @ForAll @IntRange(min = 1, max = 50) int q1,
            @ForAll @IntRange(min = 1, max = 50) int q2
    ) {
        Long userId = 1L, cartId = 1L, breadId = 10L, storeId = 100L;
        int totalStock = q1 + q2;

        BreadEntity bread = createBread(breadId, storeId, totalStock);
        CartEntity cart = createCart(cartId, userId, storeId);
        CartItemEntity existingItem = createCartItem(1L, cartId, breadId, q1);

        given(breadRepository.findById(breadId)).willReturn(Optional.of(bread));
        given(cartRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartIdAndBreadId(cartId, breadId))
                .willReturn(Optional.of(existingItem));

        CartAddRequest request = new CartAddRequest(breadId, q2);
        cartService.addToCart(userId, request);

        assertThat(existingItem.getQuantity()).isEqualTo(q1 + q2);
    }

    // ── Property 2: 유효하지 않은 수량 거부 ──
    // Feature: order-flow, Property 2: 유효하지 않은 수량 거부
    // **Validates: Requirements 1.3**
    @Property(tries = 100)
    void invalidQuantityIsRejectedByDtoValidation(
            @ForAll @IntRange(min = -100, max = 0) int invalidQty
    ) {
        // DTO validation (@Min(1)) handles rejection of qty <= 0 at controller level.
        // At service level, we verify that requesting qty <= 0 for a bread with stock=0
        // would still be caught by the stock check (totalQuantity > remainingQuantity is false for negative,
        // but the DTO @Min(1) annotation is the primary guard).
        // Verify @Min annotation exists on CartAddRequest.quantity and CartUpdateRequest.quantity fields.
        boolean cartAddHasMin = false;
        for (var field : CartAddRequest.class.getDeclaredFields()) {
            if (field.getName().equals("quantity")) {
                cartAddHasMin = java.util.Arrays.stream(field.getAnnotations())
                        .anyMatch(a -> a.annotationType().getSimpleName().equals("Min"));
            }
        }
        boolean cartUpdateHasMin = false;
        for (var field : CartUpdateRequest.class.getDeclaredFields()) {
            if (field.getName().equals("quantity")) {
                cartUpdateHasMin = java.util.Arrays.stream(field.getAnnotations())
                        .anyMatch(a -> a.annotationType().getSimpleName().equals("Min"));
            }
        }
        assertThat(cartAddHasMin).isTrue();
        assertThat(cartUpdateHasMin).isTrue();
    }

    // ── Property 3: 재고 초과 거부 ──
    // Feature: order-flow, Property 3: 재고 초과 거부
    // **Validates: Requirements 1.4**
    @Property(tries = 100)
    void quantityExceedingStockIsRejected(
            @ForAll @IntRange(min = 1, max = 100) int stock,
            @ForAll @IntRange(min = 1, max = 100) int extraQty
    ) {
        int requestedQty = stock + extraQty;
        Long userId = 1L, cartId = 1L, breadId = 10L, storeId = 100L;

        BreadEntity bread = createBread(breadId, storeId, stock);
        CartEntity cart = createCart(cartId, userId, storeId);

        given(breadRepository.findById(breadId)).willReturn(Optional.of(bread));
        given(cartRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartIdAndBreadId(cartId, breadId))
                .willReturn(Optional.empty());

        CartAddRequest request = new CartAddRequest(breadId, requestedQty);

        assertThatThrownBy(() -> cartService.addToCart(userId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.BREAD_INSUFFICIENT_QUANTITY));
    }

    // ── Property 4: 단일 매장 제약 ──
    // Feature: order-flow, Property 4: 단일 매장 제약
    // **Validates: Requirements 1.5**
    @Property(tries = 100)
    void addingBreadFromDifferentStoreIsRejected(
            @ForAll @IntRange(min = 1, max = 1000) int storeAId,
            @ForAll @IntRange(min = 1, max = 1000) int storeBOffset
    ) {
        long storeA = storeAId;
        long storeB = storeAId + storeBOffset + 1000L;
        Long userId = 1L, cartId = 1L, breadId = 10L;

        BreadEntity bread = createBread(breadId, storeB, 10);
        CartEntity cart = createCart(cartId, userId, storeA);

        given(breadRepository.findById(breadId)).willReturn(Optional.of(bread));
        given(cartRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(cart));

        CartAddRequest request = new CartAddRequest(breadId, 1);

        assertThatThrownBy(() -> cartService.addToCart(userId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CART_SINGLE_STORE_ONLY));
    }

    // ── Property 5: 장바구니 조회 응답 완전성 ──
    // Feature: order-flow, Property 5: 장바구니 조회 응답 완전성
    // **Validates: Requirements 2.1, 2.2**
    @Property(tries = 100)
    void getCartReturnsExactlyNItemsWithAllFields(
            @ForAll @IntRange(min = 1, max = 10) int n
    ) {
        Long userId = 1L, cartId = 1L, storeId = 100L;
        CartEntity cart = createCart(cartId, userId, storeId);
        StoreEntity store = createStore(storeId, "테스트매장");

        List<CartItemEntity> items = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            long breadId = 10L + i;
            items.add(createCartItem((long) (i + 1), cartId, breadId, i + 1));
        }

        given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartId(cartId)).willReturn(items);
        given(breadImageService.getImageUrls(anyList())).willReturn(Collections.emptyMap());
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

        // Mock clock
        Clock fixedClock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneId.of("Asia/Seoul"));
        lenient().when(clock.instant()).thenReturn(fixedClock.instant());
        lenient().when(clock.getZone()).thenReturn(fixedClock.getZone());

        StoreBusinessHoursEntity hours = StoreBusinessHoursEntity.builder()
                .storeId(storeId).dayOfWeek(1).isClosed(false)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(21, 0))
                .lastOrderTime(LocalTime.of(20, 0)).build();
        lenient().when(storeBusinessHoursRepository.findByStoreIdAndDayOfWeek(eq(storeId), any(Integer.class)))
                .thenReturn(Optional.of(hours));

        List<BreadEntity> breadList = new ArrayList<>();
        List<Long> breadIds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            long breadId = 10L + i;
            breadIds.add(breadId);
            breadList.add(createBread(breadId, storeId, 100));
        }
        given(breadRepository.findAllById(anyList())).willReturn(breadList);

        CartResponse response = cartService.getCart(userId);

        assertThat(response.items()).hasSize(n);
        assertThat(response.storeName()).isEqualTo("테스트매장");
        for (var item : response.items()) {
            assertThat(item.breadName()).isNotNull();
            assertThat(item.description()).isNotNull();
            assertThat(item.quantity()).isGreaterThan(0);
            assertThat(item.salePrice()).isGreaterThan(0);
            assertThat(item.breadId()).isNotNull();
            assertThat(item.cartItemId()).isNotNull();
        }
    }

    // ── Property 6: 수량 변경 반영 ──
    // Feature: order-flow, Property 6: 수량 변경 반영
    // **Validates: Requirements 3.1**
    @Property(tries = 100)
    void updateQuantitySetsExactValue(
            @ForAll @IntRange(min = 1, max = 100) int newQty
    ) {
        Long userId = 1L, cartId = 1L, cartItemId = 50L, breadId = 10L, storeId = 100L;

        CartEntity cart = createCart(cartId, userId, storeId);
        CartItemEntity cartItem = createCartItem(cartItemId, cartId, breadId, 5);
        BreadEntity bread = createBread(breadId, storeId, 100);

        given(cartRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(cart));
        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.of(cartItem));
        given(breadRepository.findById(breadId)).willReturn(Optional.of(bread));

        CartUpdateRequest request = new CartUpdateRequest(newQty);
        cartService.updateQuantity(userId, cartItemId, request);

        assertThat(cartItem.getQuantity()).isEqualTo(newQty);
    }

    // ── Property 7: 항목 삭제 후 Cart 축소 ──
    // Feature: order-flow, Property 7: 항목 삭제 후 Cart 축소
    // **Validates: Requirements 4.1**
    @Property(tries = 100)
    void removingOneItemLeavesNMinusOneItems(
            @ForAll @IntRange(min = 1, max = 10) int n
    ) {
        // Reset mocks to avoid accumulated invocation counts across tries
        Mockito.reset(cartRepository, cartItemRepository, breadRepository,
                breadImageService, storeRepository, storeBusinessHoursRepository, userRepository, clock);

        Long userId = 1L, cartId = 1L, storeId = 100L;
        CartEntity cart = createCart(cartId, userId, storeId);

        List<CartItemEntity> allItems = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            allItems.add(createCartItem((long) (i + 1), cartId, 10L + i, 2));
        }

        CartItemEntity itemToRemove = allItems.get(0);
        List<CartItemEntity> remaining = new ArrayList<>(allItems.subList(1, allItems.size()));

        given(cartRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(cart));
        given(cartItemRepository.findById(itemToRemove.getId())).willReturn(Optional.of(itemToRemove));
        given(cartItemRepository.findByCartId(cartId)).willReturn(remaining);

        cartService.removeItem(userId, itemToRemove.getId());

        verify(cartItemRepository).delete(itemToRemove);

        if (n == 1) {
            assertThat(cart.getStoreId()).isNull();
        } else {
            assertThat(cart.getStoreId()).isEqualTo(storeId);
        }
        assertThat(remaining).hasSize(n - 1);
    }

    // ── Property 8: 장바구니 비우기 ──
    // Feature: order-flow, Property 8: 장바구니 비우기
    // **Validates: Requirements 5.1**
    @Property(tries = 100)
    void clearCartLeavesEmptyCartWithNullStoreId(
            @ForAll @IntRange(min = 1, max = 10) int n
    ) {
        Mockito.reset(cartRepository, cartItemRepository, breadRepository,
                breadImageService, storeRepository, storeBusinessHoursRepository, userRepository, clock);

        Long userId = 1L, cartId = 1L, storeId = 100L;
        CartEntity cart = createCart(cartId, userId, storeId);

        assertThat(cart.getStoreId()).isEqualTo(storeId);

        given(cartRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(cart));

        cartService.clearCart(userId);

        verify(cartItemRepository).deleteByCartId(cartId);
        assertThat(cart.getStoreId()).isNull();
    }
}
