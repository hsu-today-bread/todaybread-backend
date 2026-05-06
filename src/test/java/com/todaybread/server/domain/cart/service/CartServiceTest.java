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
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CartServiceTest {

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

    @BeforeEach
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

    @Test
    void addToCart_createsCartAndNewItemForFirstBread() {
        BreadEntity bread = TestFixtures.bread(10L, 100L, 5, 4_000, 2_000);
        UserEntity user = TestFixtures.user(1L, false);
        given(breadRepository.findByIdAndIsDeletedFalse(10L)).willReturn(Optional.of(bread));
        given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.empty(), Optional.empty());
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
        given(cartRepository.save(any(CartEntity.class))).willAnswer(invocation -> {
            CartEntity cart = invocation.getArgument(0);
            ReflectionTestUtils.setField(cart, "id", 50L);
            return cart;
        });
        given(cartItemRepository.findByCartIdAndBreadId(50L, 10L)).willReturn(Optional.empty());

        cartService.addToCart(1L, new CartAddRequest(10L, 2));

        verify(cartItemRepository).save(any(CartItemEntity.class));
    }

    @Test
    void addToCart_increasesExistingItemQuantity() {
        BreadEntity bread = TestFixtures.bread(10L, 100L, 5, 4_000, 2_000);
        CartEntity cart = TestFixtures.cart(50L, 1L, 100L);
        CartItemEntity item = TestFixtures.cartItem(1L, 50L, 10L, 1);

        given(breadRepository.findByIdAndIsDeletedFalse(10L)).willReturn(Optional.of(bread));
        given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartIdAndBreadId(50L, 10L)).willReturn(Optional.of(item));

        cartService.addToCart(1L, new CartAddRequest(10L, 2));

        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    void addToCart_rejectsBreadFromAnotherStore() {
        BreadEntity bread = TestFixtures.bread(10L, 100L, 5, 4_000, 2_000);
        CartEntity cart = TestFixtures.cart(50L, 1L, 200L);
        given(breadRepository.findByIdAndIsDeletedFalse(10L)).willReturn(Optional.of(bread));
        given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.addToCart(1L, new CartAddRequest(10L, 1)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CART_SINGLE_STORE_ONLY);
    }

    @Test
    void getCart_returnsEmptyResponseWhenCartMissing() {
        given(cartRepository.findByUserId(1L)).willReturn(Optional.empty());

        CartResponse response = cartService.getCart(1L);

        assertThat(response.storeName()).isNull();
        assertThat(response.items()).isEmpty();
        verifyNoInteractions(cartItemRepository, breadImageService);
    }

    @Test
    void getCart_returnsMappedResponse() {
        CartEntity cart = TestFixtures.cart(50L, 1L, 100L);
        CartItemEntity item = TestFixtures.cartItem(1L, 50L, 10L, 2);
        BreadEntity bread = TestFixtures.bread(10L, 100L, 5, 4_000, 2_000);
        StoreEntity store = TestFixtures.store(100L, 1L);
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(
                100L, 7, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)
        );

        given(cartRepository.findByUserId(1L)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartId(50L)).willReturn(List.of(item));
        given(breadImageService.getImageUrls(List.of(10L))).willReturn(Map.of(10L, "https://cdn/bread.jpg"));
        given(breadRepository.findAllById(List.of(10L))).willReturn(List.of(bread));
        given(storeRepository.findById(100L)).willReturn(Optional.of(store));
        given(storeBusinessHoursRepository.findByStoreIdAndDayOfWeek(100L, 7)).willReturn(Optional.of(hours));

        CartResponse response = cartService.getCart(1L);

        assertThat(response.storeName()).isEqualTo(store.getName());
        assertThat(response.lastOrderTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().breadName()).isEqualTo(bread.getName());
    }

    @Test
    void updateQuantity_updatesItemWhenStockIsEnough() {
        CartEntity cart = TestFixtures.cart(50L, 1L, 100L);
        CartItemEntity item = TestFixtures.cartItem(1L, 50L, 10L, 2);
        BreadEntity bread = TestFixtures.bread(10L, 100L, 5, 4_000, 2_000);

        given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.of(cart));
        given(cartItemRepository.findById(1L)).willReturn(Optional.of(item));
        given(breadRepository.findByIdAndIsDeletedFalse(10L)).willReturn(Optional.of(bread));

        cartService.updateQuantity(1L, 1L, new CartUpdateRequest(4));

        assertThat(item.getQuantity()).isEqualTo(4);
    }

    @Test
    void removeItem_clearsStoreIdWhenLastItemIsRemoved() {
        CartEntity cart = TestFixtures.cart(50L, 1L, 100L);
        CartItemEntity item = TestFixtures.cartItem(1L, 50L, 10L, 2);

        given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.of(cart));
        given(cartItemRepository.findById(1L)).willReturn(Optional.of(item));
        given(cartItemRepository.findByCartId(50L)).willReturn(List.of());

        cartService.removeItem(1L, 1L);

        assertThat(cart.getStoreId()).isNull();
        verify(cartItemRepository).delete(item);
    }

    @Test
    void clearCart_deletesItemsAndResetsStoreId() {
        CartEntity cart = TestFixtures.cart(50L, 1L, 100L);
        given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.of(cart));

        cartService.clearCart(1L);

        assertThat(cart.getStoreId()).isNull();
        verify(cartItemRepository).deleteByCartId(50L);
    }

    @Test
    void getCartWithItemsForCheckout_rejectsEmptyCartItems() {
        CartEntity cart = TestFixtures.cart(50L, 1L, 100L);
        given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartIdWithLock(50L)).willReturn(List.of());

        assertThatThrownBy(() -> cartService.getCartWithItemsForCheckout(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CART_EMPTY);
    }

    @Test
    void addToCart_삭제된빵_BREAD_NOT_FOUND() {
        given(breadRepository.findByIdAndIsDeletedFalse(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(1L, new CartAddRequest(10L, 1)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BREAD_NOT_FOUND);
    }

    @Test
    void getCart_삭제된빵항목_자동제거_정상항목유지() {
        CartEntity cart = TestFixtures.cart(50L, 1L, 100L);
        CartItemEntity validItem = TestFixtures.cartItem(1L, 50L, 10L, 2);
        CartItemEntity deletedItem = TestFixtures.cartItem(2L, 50L, 20L, 1);

        BreadEntity validBread = TestFixtures.bread(10L, 100L, 5, 4_000, 2_000);
        BreadEntity deletedBread = TestFixtures.bread(20L, 100L, 3, 3_000, 1_500);
        deletedBread.softDelete(java.time.LocalDateTime.now());

        StoreEntity store = TestFixtures.store(100L, 1L);
        StoreBusinessHoursEntity hours = TestFixtures.businessHours(
                100L, 7, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)
        );

        given(cartRepository.findByUserId(1L)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartId(50L)).willReturn(List.of(validItem, deletedItem));
        given(breadImageService.getImageUrls(List.of(10L, 20L))).willReturn(Map.of(10L, "https://cdn/bread10.jpg"));
        given(breadRepository.findAllById(List.of(10L, 20L))).willReturn(List.of(validBread, deletedBread));
        given(storeRepository.findById(100L)).willReturn(Optional.of(store));
        given(storeBusinessHoursRepository.findByStoreIdAndDayOfWeek(100L, 7)).willReturn(Optional.of(hours));

        CartResponse response = cartService.getCart(1L);

        assertThat(response.storeName()).isEqualTo(store.getName());
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().breadName()).isEqualTo(validBread.getName());
        assertThat(cart.getStoreId()).isEqualTo(100L);
        verify(cartItemRepository).deleteAll(List.of(deletedItem));
    }

    @Test
    void getCart_모든항목삭제됨_storeId_null() {
        CartEntity cart = TestFixtures.cart(50L, 1L, 100L);
        CartItemEntity deletedItem1 = TestFixtures.cartItem(1L, 50L, 10L, 2);
        CartItemEntity deletedItem2 = TestFixtures.cartItem(2L, 50L, 20L, 1);

        BreadEntity deletedBread1 = TestFixtures.bread(10L, 100L, 5, 4_000, 2_000);
        deletedBread1.softDelete(java.time.LocalDateTime.now());
        BreadEntity deletedBread2 = TestFixtures.bread(20L, 100L, 3, 3_000, 1_500);
        deletedBread2.softDelete(java.time.LocalDateTime.now());

        given(cartRepository.findByUserId(1L)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartId(50L)).willReturn(List.of(deletedItem1, deletedItem2));
        given(breadImageService.getImageUrls(List.of(10L, 20L))).willReturn(Map.of());
        given(breadRepository.findAllById(List.of(10L, 20L))).willReturn(List.of(deletedBread1, deletedBread2));

        CartResponse response = cartService.getCart(1L);

        assertThat(response.storeName()).isNull();
        assertThat(response.items()).isEmpty();
        assertThat(cart.getStoreId()).isNull();
        verify(cartItemRepository).deleteAll(List.of(deletedItem1, deletedItem2));
    }

    @Test
    void updateQuantity_삭제된빵_BREAD_NOT_FOUND() {
        CartEntity cart = TestFixtures.cart(50L, 1L, 100L);
        CartItemEntity item = TestFixtures.cartItem(1L, 50L, 10L, 2);

        given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.of(cart));
        given(cartItemRepository.findById(1L)).willReturn(Optional.of(item));
        given(breadRepository.findByIdAndIsDeletedFalse(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity(1L, 1L, new CartUpdateRequest(3)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BREAD_NOT_FOUND);
    }

    @Test
    void updateQuantity_재고초과_BREAD_INSUFFICIENT_QUANTITY() {
        CartEntity cart = TestFixtures.cart(50L, 1L, 100L);
        CartItemEntity item = TestFixtures.cartItem(1L, 50L, 10L, 2);
        BreadEntity bread = TestFixtures.bread(10L, 100L, 5, 4_000, 2_000);

        given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.of(cart));
        given(cartItemRepository.findById(1L)).willReturn(Optional.of(item));
        given(breadRepository.findByIdAndIsDeletedFalse(10L)).willReturn(Optional.of(bread));

        assertThatThrownBy(() -> cartService.updateQuantity(1L, 1L, new CartUpdateRequest(10)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BREAD_INSUFFICIENT_QUANTITY);
    }

    @Test
    void addToCart_재고초과_BREAD_INSUFFICIENT_QUANTITY() {
        BreadEntity bread = TestFixtures.bread(10L, 100L, 3, 4_000, 2_000);
        CartEntity cart = TestFixtures.cart(50L, 1L, 100L);

        given(breadRepository.findByIdAndIsDeletedFalse(10L)).willReturn(Optional.of(bread));
        given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartIdAndBreadId(50L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(1L, new CartAddRequest(10L, 5)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BREAD_INSUFFICIENT_QUANTITY);
    }

    @Test
    void getCartWithItemsForCheckout_정상경로_CartWithItems반환() {
        CartEntity cart = TestFixtures.cart(50L, 1L, 100L);
        CartItemEntity item = TestFixtures.cartItem(1L, 50L, 10L, 2);

        given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartIdWithLock(50L)).willReturn(List.of(item));

        CartService.CartWithItems result = cartService.getCartWithItemsForCheckout(1L);

        assertThat(result.cart()).isEqualTo(cart);
        assertThat(result.items()).containsExactly(item);
    }
}
