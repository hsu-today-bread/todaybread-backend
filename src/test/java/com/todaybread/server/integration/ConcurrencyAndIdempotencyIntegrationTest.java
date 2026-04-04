package com.todaybread.server.integration;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.cart.dto.CartAddRequest;
import com.todaybread.server.domain.cart.entity.CartEntity;
import com.todaybread.server.domain.cart.entity.CartItemEntity;
import com.todaybread.server.domain.cart.repository.CartItemRepository;
import com.todaybread.server.domain.cart.repository.CartRepository;
import com.todaybread.server.domain.cart.service.CartService;
import com.todaybread.server.domain.order.dto.DirectOrderRequest;
import com.todaybread.server.domain.order.dto.OrderDetailResponse;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.order.service.OrderService;
import com.todaybread.server.domain.payment.dto.PaymentRequest;
import com.todaybread.server.domain.payment.dto.PaymentResponse;
import com.todaybread.server.domain.payment.entity.PaymentStatus;
import com.todaybread.server.domain.payment.processor.PaymentProcessor;
import com.todaybread.server.domain.payment.processor.PaymentResult;
import com.todaybread.server.domain.payment.repository.PaymentRepository;
import com.todaybread.server.domain.payment.service.PaymentService;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyAndIdempotencyIntegrationTest {

    private static final AtomicLong SEQUENCE = new AtomicLong(1_000L);

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private BreadRepository breadRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private PaymentProcessor paymentProcessor;

    @Test
    void concurrentFirstAddToCartCreatesSingleCartAndAccumulatesQuantity() throws Exception {
        UserStoreBreadFixture fixture = createUserStoreBreadFixture(10, 3_000);
        CartAddRequest request = new CartAddRequest(fixture.breadId(), 1);

        List<AsyncResult<Void>> results = runConcurrently(List.of(
                () -> {
                    cartService.addToCart(fixture.userId(), request);
                    return null;
                },
                () -> {
                    cartService.addToCart(fixture.userId(), request);
                    return null;
                }
        ));

        assertThat(results).allSatisfy(result -> assertThat(result.error()).isNull());

        CartEntity cart = cartRepository.findByUserId(fixture.userId()).orElseThrow();
        long cartCount = cartRepository.findAll().stream()
                .filter(savedCart -> savedCart.getUserId().equals(fixture.userId()))
                .count();

        Optional<CartItemEntity> cartItem = cartItemRepository.findByCartIdAndBreadId(cart.getId(), fixture.breadId());

        assertThat(cartCount).isEqualTo(1L);
        assertThat(cartItem).isPresent();
        assertThat(cartItem.orElseThrow().getQuantity()).isEqualTo(2);
        assertThat(cart.getStoreId()).isEqualTo(fixture.storeId());
    }

    @Test
    void concurrentCheckoutWithDifferentKeysCreatesOnlyOneOrder() throws Exception {
        CartFixture fixture = createCartFixture(10, 2, 4_000);

        List<AsyncResult<OrderDetailResponse>> results = runConcurrently(List.of(
                () -> orderService.createOrderFromCart(fixture.userId(), newIdempotencyKey("order-a")),
                () -> orderService.createOrderFromCart(fixture.userId(), newIdempotencyKey("order-b"))
        ));

        List<AsyncResult<OrderDetailResponse>> successes = results.stream()
                .filter(result -> result.error() == null)
                .toList();
        List<AsyncResult<OrderDetailResponse>> failures = results.stream()
                .filter(result -> result.error() != null)
                .toList();

        assertThat(successes).hasSize(1);
        assertThat(failures).hasSize(1);
        assertThat(failures.getFirst().error()).isInstanceOf(CustomException.class);
        assertThat(((CustomException) failures.getFirst().error()).getErrorCode())
                .isEqualTo(ErrorCode.CART_EMPTY);

        assertThat(orderRepository.findByUserIdOrderByCreatedAtDesc(fixture.userId())).hasSize(1);
        assertThat(breadRepository.findById(fixture.breadId()).orElseThrow().getRemainingQuantity()).isEqualTo(8);
        assertThat(cartItemRepository.findByCartId(fixture.cartId())).isEmpty();
    }

    @Test
    void concurrentCheckoutWithSameKeyReturnsTheSameOrder() throws Exception {
        CartFixture fixture = createCartFixture(10, 2, 5_000);
        String idempotencyKey = newIdempotencyKey("order-same");

        List<AsyncResult<OrderDetailResponse>> results = runConcurrently(List.of(
                () -> orderService.createOrderFromCart(fixture.userId(), idempotencyKey),
                () -> orderService.createOrderFromCart(fixture.userId(), idempotencyKey)
        ));

        assertThat(results).allSatisfy(result -> assertThat(result.error()).isNull());

        List<OrderDetailResponse> responses = results.stream()
                .map(AsyncResult::value)
                .toList();

        assertThat(responses).extracting(OrderDetailResponse::orderId)
                .containsOnly(responses.getFirst().orderId());
        assertThat(orderRepository.findByUserIdOrderByCreatedAtDesc(fixture.userId())).hasSize(1);
        assertThat(orderRepository.findByUserIdAndIdempotencyKey(fixture.userId(), idempotencyKey)).isPresent();
        assertThat(breadRepository.findById(fixture.breadId()).orElseThrow().getRemainingQuantity()).isEqualTo(8);
        assertThat(cartItemRepository.findByCartId(fixture.cartId())).isEmpty();
    }

    @Test
    void concurrentPaymentWithSameKeyReturnsTheSamePaymentAndCallsProcessorOnce() throws Exception {
        UserStoreBreadFixture fixture = createUserStoreBreadFixture(10, 6_000);
        OrderDetailResponse order = orderService.createDirectOrder(
                fixture.userId(),
                new DirectOrderRequest(fixture.breadId(), 2),
                newIdempotencyKey("setup-order")
        );

        String paymentKey = newIdempotencyKey("payment");
        PaymentRequest request = new PaymentRequest(order.orderId(), order.totalAmount());

        given(paymentProcessor.pay(order.orderId(), order.totalAmount()))
                .willAnswer(invocation -> {
                    Thread.sleep(150L);
                    return new PaymentResult(PaymentStatus.APPROVED, "approved");
                });

        List<AsyncResult<PaymentResponse>> results = runConcurrently(List.of(
                () -> paymentService.processPayment(fixture.userId(), request, paymentKey),
                () -> paymentService.processPayment(fixture.userId(), request, paymentKey)
        ));

        assertThat(results).allSatisfy(result -> assertThat(result.error()).isNull());

        List<PaymentResponse> responses = results.stream()
                .map(AsyncResult::value)
                .toList();

        assertThat(responses).extracting(PaymentResponse::paymentId)
                .containsOnly(responses.getFirst().paymentId());
        assertThat(responses).extracting(PaymentResponse::status).containsOnly(PaymentStatus.APPROVED);
        assertThat(paymentRepository.findByOrderIdAndIdempotencyKey(order.orderId(), paymentKey)).isPresent();
        assertThat(orderRepository.findById(order.orderId()).map(OrderEntity::getStatus))
                .contains(OrderStatus.CONFIRMED);
        then(paymentProcessor).should(times(1)).pay(order.orderId(), order.totalAmount());
    }

    private CartFixture createCartFixture(int stock, int quantity, int salePrice) {
        UserStoreBreadFixture fixture = createUserStoreBreadFixture(stock, salePrice);
        CartEntity cart = cartRepository.saveAndFlush(CartEntity.builder()
                .userId(fixture.userId())
                .storeId(fixture.storeId())
                .build());
        cartItemRepository.saveAndFlush(CartItemEntity.builder()
                .cartId(cart.getId())
                .breadId(fixture.breadId())
                .quantity(quantity)
                .build());
        return new CartFixture(fixture.userId(), fixture.storeId(), fixture.breadId(), cart.getId());
    }

    private UserStoreBreadFixture createUserStoreBreadFixture(int stock, int salePrice) {
        long unique = SEQUENCE.getAndIncrement();

        UserEntity user = userRepository.saveAndFlush(UserEntity.builder()
                .email("user" + unique + "@todaybread.test")
                .passwordHash("hashed-password")
                .name("user-" + unique)
                .nickname("nick-" + unique)
                .phoneNumber("0101234" + unique)
                .build());

        StoreEntity store = storeRepository.saveAndFlush(StoreEntity.builder()
                .userId(user.getId())
                .name("store-" + unique)
                .phoneNumber("021234" + unique)
                .description("integration test store")
                .addressLine1("서울시 테스트구")
                .addressLine2("1층")
                .latitude(new BigDecimal("37.5665000"))
                .longitude(new BigDecimal("126.9780000"))
                .build());

        BreadEntity bread = breadRepository.saveAndFlush(BreadEntity.builder()
                .storeId(store.getId())
                .name("bread-" + unique)
                .description("integration test bread")
                .originalPrice(salePrice + 1_000)
                .salePrice(salePrice)
                .remainingQuantity(stock)
                .build());

        return new UserStoreBreadFixture(user.getId(), store.getId(), bread.getId());
    }

    private String newIdempotencyKey(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private <T> List<AsyncResult<T>> runConcurrently(List<ThrowingSupplier<T>> actions) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(actions.size());
        CountDownLatch ready = new CountDownLatch(actions.size());
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<AsyncResult<T>>> futures = new ArrayList<>();
            for (ThrowingSupplier<T> action : actions) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    try {
                        return new AsyncResult<>(action.get(), null);
                    } catch (Throwable error) {
                        return new AsyncResult<>(null, error);
                    }
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<AsyncResult<T>> results = new ArrayList<>();
            for (Future<AsyncResult<T>> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private record AsyncResult<T>(T value, Throwable error) {
    }

    private record UserStoreBreadFixture(Long userId, Long storeId, Long breadId) {
    }

    private record CartFixture(Long userId, Long storeId, Long breadId, Long cartId) {
    }
}
