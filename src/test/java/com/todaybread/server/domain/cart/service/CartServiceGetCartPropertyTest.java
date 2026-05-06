package com.todaybread.server.domain.cart.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.bread.service.BreadImageService;
import com.todaybread.server.domain.cart.dto.CartResponse;
import com.todaybread.server.domain.cart.entity.CartEntity;
import com.todaybread.server.domain.cart.entity.CartItemEntity;
import com.todaybread.server.domain.cart.repository.CartItemRepository;
import com.todaybread.server.domain.cart.repository.CartRepository;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.support.TestFixtures;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Property 3: 장바구니 조회 시 삭제된 빵 자동 정리
 *
 * For any 장바구니 상태(일부 삭제된 빵 항목 포함)에서 getCart()를 호출하면,
 * 반환된 응답에는 isDeleted=true인 빵의 항목이 포함되지 않아야 하며,
 * 모든 항목이 삭제된 빵이었을 경우 장바구니의 storeId는 null이어야 한다.
 *
 * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
 */
@Tag("Feature: bread-soft-delete, Property 3: 장바구니 조회 시 삭제된 빵 자동 정리")
class CartServiceGetCartPropertyTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private BreadRepository breadRepository;

    @Mock
    private BreadImageService breadImageService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreBusinessHoursRepository storeBusinessHoursRepository;

    @Mock
    private UserRepository userRepository;

    private CartService cartService;

    @BeforeProperty
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cartService = new CartService(
                cartRepository,
                cartItemRepository,
                breadRepository,
                breadImageService,
                storeRepository,
                storeBusinessHoursRepository,
                userRepository,
                TestFixtures.FIXED_CLOCK
        );
    }

    /**
     * Property 3-1: getCart() 응답에는 삭제된 빵 항목이 포함되지 않는다.
     *
     * 임의의 장바구니 상태(삭제된 빵 포함)를 생성하여 getCart() 호출 후
     * 응답에 삭제된 빵이 포함되지 않음을 검증한다.
     *
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    void getCart_neverReturnsDeletedBreadItems(
            @ForAll("cartStatesWithDeletedBreads") CartState cartState
    ) {
        // Arrange
        Long userId = 1L;
        Long storeId = 100L;
        Long cartId = 50L;

        CartEntity cart = TestFixtures.cart(cartId, userId, storeId);
        StoreEntity store = TestFixtures.store(storeId, 2L);
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(
                storeId, 6, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)
        );

        // Build cart items and bread entities from the generated state
        List<CartItemEntity> cartItems = new ArrayList<>();
        List<BreadEntity> allBreads = new ArrayList<>();
        Map<Long, String> imageUrlMap = new HashMap<>();

        for (int i = 0; i < cartState.items().size(); i++) {
            CartItemSpec spec = cartState.items().get(i);
            Long breadId = (long) (i + 1);
            Long cartItemId = (long) (i + 1);

            CartItemEntity cartItem = TestFixtures.cartItem(cartItemId, cartId, breadId, spec.quantity());
            cartItems.add(cartItem);

            BreadEntity bread = TestFixtures.bread(breadId, storeId, spec.remainingQuantity(), 4000, 2000);
            if (spec.deleted()) {
                bread.softDelete(LocalDateTime.of(2026, 1, 1, 12, 0));
            }
            allBreads.add(bread);
            imageUrlMap.put(breadId, "https://cdn/bread-" + breadId + ".jpg");
        }

        List<Long> breadIds = allBreads.stream().map(BreadEntity::getId).collect(Collectors.toList());

        given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartId(cartId)).willReturn(cartItems);
        given(breadImageService.getImageUrls(breadIds)).willReturn(imageUrlMap);
        given(breadRepository.findAllById(breadIds)).willReturn(allBreads);
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(storeBusinessHoursRepository.findByStoreIdAndDayOfWeek(storeId, 6))
                .willReturn(Optional.of(hours));

        // Act
        CartResponse response = cartService.getCart(userId);

        // Assert: 응답에 삭제된 빵이 포함되지 않아야 한다
        Set<Long> deletedBreadIds = new HashSet<>();
        for (int i = 0; i < cartState.items().size(); i++) {
            if (cartState.items().get(i).deleted()) {
                deletedBreadIds.add((long) (i + 1));
            }
        }

        for (var item : response.items()) {
            assertThat(deletedBreadIds).doesNotContain(item.breadId());
        }

        // Assert: 응답 항목 수는 미삭제 빵 수와 동일해야 한다
        long expectedValidCount = cartState.items().stream().filter(s -> !s.deleted()).count();
        assertThat(response.items()).hasSize((int) expectedValidCount);

        // Assert: 모든 항목이 삭제된 경우 storeName은 null이어야 한다 (storeId = null)
        if (expectedValidCount == 0) {
            assertThat(response.storeName()).isNull();
            assertThat(response.items()).isEmpty();
            // cart.storeId가 null로 설정되었는지 확인
            assertThat(cart.getStoreId()).isNull();
        } else {
            // 정상 항목이 남아 있으면 storeId 유지
            assertThat(cart.getStoreId()).isEqualTo(storeId);
            assertThat(response.storeName()).isEqualTo(store.getName());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<CartState> cartStatesWithDeletedBreads() {
        Arbitrary<CartItemSpec> itemSpec = Combinators.combine(
                Arbitraries.integers().between(1, 5),       // quantity
                Arbitraries.integers().between(5, 100),     // remainingQuantity
                Arbitraries.of(true, false)                 // deleted
        ).as(CartItemSpec::new);

        // 최소 1개 항목, 최대 8개 항목. 삭제된 빵이 최소 1개 포함되도록 보장
        return itemSpec.list().ofMinSize(1).ofMaxSize(8)
                .filter(items -> items.stream().anyMatch(CartItemSpec::deleted))
                .map(CartState::new);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Internal records for test data specification
    // ──────────────────────────────────────────────────────────────────────

    record CartItemSpec(int quantity, int remainingQuantity, boolean deleted) {}

    record CartState(List<CartItemSpec> items) {}
}
