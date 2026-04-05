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
        given(breadRepository.findById(10L)).willReturn(Optional.of(bread));
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

        given(breadRepository.findById(10L)).willReturn(Optional.of(bread));
        given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartIdAndBreadId(50L, 10L)).willReturn(Optional.of(item));

        cartService.addToCart(1L, new CartAddRequest(10L, 2));

        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    void addToCart_rejectsBreadFromAnotherStore() {
        BreadEntity bread = TestFixtures.bread(10L, 100L, 5, 4_000, 2_000);
        CartEntity cart = TestFixtures.cart(50L, 1L, 200L);
        given(breadRepository.findById(10L)).willReturn(Optional.of(bread));
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
        given(breadRepository.findById(10L)).willReturn(Optional.of(bread));

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
}
