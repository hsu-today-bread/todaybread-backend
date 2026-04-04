package com.todaybread.server.domain.cart.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.bread.service.BreadImageService;
import com.todaybread.server.domain.cart.dto.CartAddRequest;
import com.todaybread.server.domain.cart.dto.CartResponse;
import com.todaybread.server.domain.cart.entity.CartEntity;
import com.todaybread.server.domain.cart.entity.CartItemEntity;
import com.todaybread.server.domain.cart.repository.CartItemRepository;
import com.todaybread.server.domain.cart.repository.CartRepository;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @InjectMocks
    private CartService cartService;

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
    private Clock clock;

    @Mock
    private UserRepository userRepository;

    private CartEntity createCartEntity(Long cartId, Long userId, Long storeId) {
        CartEntity cart = CartEntity.builder()
                .userId(userId)
                .storeId(storeId)
                .build();
        ReflectionTestUtils.setField(cart, "id", cartId);
        return cart;
    }

    private BreadEntity createBreadEntity(Long breadId, Long storeId) {
        BreadEntity bread = BreadEntity.builder()
                .storeId(storeId)
                .name("소금빵")
                .description("겉바속촉")
                .originalPrice(5000)
                .salePrice(3500)
                .remainingQuantity(10)
                .build();
        ReflectionTestUtils.setField(bread, "id", breadId);
        return bread;
    }

    private CartItemEntity createCartItemEntity(Long itemId, Long cartId, Long breadId, int quantity) {
        CartItemEntity item = CartItemEntity.builder()
                .cartId(cartId)
                .breadId(breadId)
                .quantity(quantity)
                .build();
        ReflectionTestUtils.setField(item, "id", itemId);
        return item;
    }

    @Nested
    @DisplayName("getCart")
    class GetCart {

        @Test
        @DisplayName("Cart가 없으면 빈 응답을 반환한다")
        void emptyCartReturnsEmptyResponse() {
            given(cartRepository.findByUserId(1L)).willReturn(Optional.empty());

            CartResponse result = cartService.getCart(1L);

            assertThat(result.storeName()).isNull();
            assertThat(result.lastOrderTime()).isNull();
            assertThat(result.items()).isEmpty();
        }

        @Test
        @DisplayName("Cart의 storeId가 null이면 빈 응답을 반환한다")
        void cartWithNullStoreIdReturnsEmptyResponse() {
            CartEntity cart = createCartEntity(1L, 1L, null);
            given(cartRepository.findByUserId(1L)).willReturn(Optional.of(cart));

            CartResponse result = cartService.getCart(1L);

            assertThat(result.storeName()).isNull();
            assertThat(result.lastOrderTime()).isNull();
            assertThat(result.items()).isEmpty();
        }
    }

    @Nested
    @DisplayName("addToCart")
    class AddToCart {

        @Test
        @DisplayName("존재하지 않는 빵 ID로 추가하면 BREAD_NOT_FOUND 예외를 던진다")
        void nonExistentBreadThrowsBreadNotFound() {
            CartAddRequest request = new CartAddRequest(999L, 1);
            given(breadRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addToCart(1L, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.BREAD_NOT_FOUND));
        }

        @Test
        @DisplayName("다른 매장의 빵을 추가하면 CART_SINGLE_STORE_ONLY 예외를 던진다")
        void differentStoreBreadThrowsSingleStoreOnly() {
            Long storeA = 10L;
            Long storeB = 20L;
            CartEntity cart = createCartEntity(1L, 1L, storeA);
            BreadEntity bread = createBreadEntity(100L, storeB);
            CartAddRequest request = new CartAddRequest(100L, 1);

            given(breadRepository.findById(100L)).willReturn(Optional.of(bread));
            given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.of(cart));

            assertThatThrownBy(() -> cartService.addToCart(1L, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.CART_SINGLE_STORE_ONLY));
        }
    }

    @Nested
    @DisplayName("removeItem")
    class RemoveItem {

        @Test
        @DisplayName("마지막 항목 삭제 시 Cart의 storeId를 null로 초기화한다")
        void lastItemRemovalResetsStoreId() {
            CartEntity cart = createCartEntity(1L, 1L, 10L);
            CartItemEntity item = createCartItemEntity(100L, 1L, 50L, 2);

            given(cartRepository.findByUserIdWithLock(1L)).willReturn(Optional.of(cart));
            given(cartItemRepository.findById(100L)).willReturn(Optional.of(item));
            given(cartItemRepository.findByCartId(1L)).willReturn(List.of());

            cartService.removeItem(1L, 100L);

            verify(cartItemRepository).delete(item);
            assertThat(cart.getStoreId()).isNull();
        }
    }
}
