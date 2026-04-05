package com.todaybread.server.support;

import com.todaybread.server.domain.auth.entity.RefreshTokenEntity;
import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.entity.BreadImageEntity;
import com.todaybread.server.domain.cart.entity.CartEntity;
import com.todaybread.server.domain.cart.entity.CartItemEntity;
import com.todaybread.server.domain.keyword.entity.KeywordEntity;
import com.todaybread.server.domain.keyword.entity.UserKeywordEntity;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.payment.entity.PaymentEntity;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import com.todaybread.server.domain.store.entity.FavouriteStoreEntity;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.entity.StoreImageEntity;
import com.todaybread.server.domain.store.repository.StoreDistanceProjection;
import com.todaybread.server.domain.user.entity.UserEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class TestFixtures {

    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    public static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-05T03:00:00Z"), SEOUL);

    private TestFixtures() {
    }

    public static UserEntity user(Long id, boolean isBoss) {
        UserEntity user = UserEntity.builder()
                .email("user" + id + "@example.com")
                .passwordHash("encoded-password")
                .name("user-" + id)
                .nickname("nick-" + id)
                .phoneNumber("0101234" + String.format("%04d", id.intValue()))
                .build();
        setId(user, id);
        if (isBoss) {
            user.approveBoss();
        }
        return user;
    }

    public static StoreEntity store(Long id, Long userId) {
        StoreEntity store = StoreEntity.builder()
                .userId(userId)
                .name("store-" + id)
                .phoneNumber("02-1234-" + String.format("%04d", id.intValue()))
                .description("desc-" + id)
                .addressLine1("address1-" + id)
                .addressLine2("address2-" + id)
                .latitude(BigDecimal.valueOf(37.5))
                .longitude(BigDecimal.valueOf(127.0))
                .build();
        setId(store, id);
        return store;
    }

    public static StoreBusinessHoursEntity businessHours(
            Long storeId,
            int dayOfWeek,
            boolean isClosed,
            LocalTime startTime,
            LocalTime endTime,
            LocalTime lastOrderTime
    ) {
        return StoreBusinessHoursEntity.builder()
                .storeId(storeId)
                .dayOfWeek(dayOfWeek)
                .isClosed(isClosed)
                .startTime(startTime)
                .endTime(endTime)
                .lastOrderTime(lastOrderTime)
                .build();
    }

    public static StoreImageEntity storeImage(Long id, Long storeId, String storedFilename, int displayOrder) {
        StoreImageEntity image = StoreImageEntity.builder()
                .storeId(storeId)
                .originalFilename("store-" + id + ".jpg")
                .storedFilename(storedFilename)
                .displayOrder(displayOrder)
                .build();
        setId(image, id);
        return image;
    }

    public static FavouriteStoreEntity favouriteStore(Long id, Long userId, Long storeId) {
        FavouriteStoreEntity favourite = FavouriteStoreEntity.builder()
                .userId(userId)
                .storeId(storeId)
                .build();
        setId(favourite, id);
        return favourite;
    }

    public static BreadEntity bread(Long id, Long storeId, int remainingQuantity, int originalPrice, int salePrice) {
        BreadEntity bread = BreadEntity.builder()
                .storeId(storeId)
                .name("bread-" + id)
                .description("bread-desc-" + id)
                .originalPrice(originalPrice)
                .salePrice(salePrice)
                .remainingQuantity(remainingQuantity)
                .build();
        setId(bread, id);
        return bread;
    }

    public static BreadImageEntity breadImage(Long id, Long breadId, String storedFilename) {
        BreadImageEntity image = BreadImageEntity.builder()
                .breadId(breadId)
                .originalFilename("bread-" + id + ".jpg")
                .storedFilename(storedFilename)
                .build();
        setId(image, id);
        return image;
    }

    public static CartEntity cart(Long id, Long userId, Long storeId) {
        CartEntity cart = CartEntity.builder()
                .userId(userId)
                .storeId(storeId)
                .build();
        setId(cart, id);
        return cart;
    }

    public static CartItemEntity cartItem(Long id, Long cartId, Long breadId, int quantity) {
        CartItemEntity item = CartItemEntity.builder()
                .cartId(cartId)
                .breadId(breadId)
                .quantity(quantity)
                .build();
        setId(item, id);
        return item;
    }

    public static OrderEntity order(Long id, Long userId, Long storeId, OrderStatus status, int totalAmount, String idempotencyKey) {
        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .storeId(storeId)
                .status(status)
                .totalAmount(totalAmount)
                .idempotencyKey(idempotencyKey)
                .build();
        setId(order, id);
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.of(2026, 4, 5, 12, 0));
        return order;
    }

    public static OrderItemEntity orderItem(Long id, Long orderId, Long breadId, int breadPrice, int quantity) {
        OrderItemEntity item = OrderItemEntity.builder()
                .orderId(orderId)
                .breadId(breadId)
                .breadName("ordered-bread-" + id)
                .breadPrice(breadPrice)
                .quantity(quantity)
                .build();
        setId(item, id);
        return item;
    }

    public static PaymentEntity payment(
            Long id,
            Long orderId,
            int amount,
            PaymentStatus status,
            LocalDateTime paidAt,
            String idempotencyKey
    ) {
        PaymentEntity payment = PaymentEntity.builder()
                .orderId(orderId)
                .amount(amount)
                .status(status)
                .paidAt(paidAt)
                .idempotencyKey(idempotencyKey)
                .build();
        setId(payment, id);
        return payment;
    }

    public static RefreshTokenEntity refreshToken(Long id, Long userId, String token, LocalDateTime expiresAt) {
        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        setId(refreshToken, id);
        return refreshToken;
    }

    public static KeywordEntity keyword(Long id, String normalisedText) {
        KeywordEntity keyword = KeywordEntity.builder()
                .normalisedText(normalisedText)
                .build();
        setId(keyword, id);
        return keyword;
    }

    public static UserKeywordEntity userKeyword(Long id, Long userId, Long keywordId, String displayText) {
        UserKeywordEntity userKeyword = UserKeywordEntity.builder()
                .userId(userId)
                .keywordId(keywordId)
                .displayText(displayText)
                .build();
        setId(userKeyword, id);
        return userKeyword;
    }

    public static MockMultipartFile imageFile(String originalFilename) {
        return new MockMultipartFile(
                "file",
                originalFilename,
                "image/jpeg",
                "image-bytes".getBytes(StandardCharsets.UTF_8)
        );
    }

    public static StoreDistanceProjection projection(Long storeId, double distance) {
        return new StoreDistanceProjection() {
            @Override
            public Long getStoreId() {
                return storeId;
            }

            @Override
            public Double getDistance() {
                return distance;
            }
        };
    }

    private static void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}
