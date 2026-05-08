package com.todaybread.server.domain.interestarea.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.interestarea.dto.NotificationTargetResult;
import com.todaybread.server.domain.interestarea.repository.InterestAreaRepository;
import com.todaybread.server.domain.keyword.repository.KeywordRepository;
import com.todaybread.server.domain.keyword.repository.UserKeywordRepository;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * Property 5: 전제조건 미충족 시 빈 결과
 *
 * For any 빵에 대해 bread.isDeleted=true이거나, bread.remainingQuantity≤0이거나,
 * SellingStatusUtil.isSelling()=false이면, 알림 대상 계산 결과는 항상 빈 목록이어야 한다.
 *
 * **Validates: Requirements 7.1, 7.2, 7.3, 7.5**
 */
@Tag("Feature: keyword-area-notification, Property 5: 전제조건 미충족 시 빈 결과")
class NotificationPreconditionFailPropertyTest {

    @Mock
    private InterestAreaRepository interestAreaRepository;

    @Mock
    private UserKeywordRepository userKeywordRepository;

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private StoreBusinessHoursRepository storeBusinessHoursRepository;

    private NotificationTargetCalculator calculator;

    // 고정 시간: 2024-01-15 (월요일) 10:00
    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDateTime.of(2024, 1, 15, 10, 0)
                    .toInstant(ZoneOffset.of("+09:00")),
            ZoneId.of("Asia/Seoul")
    );

    @BeforeProperty
    void setUp() {
        MockitoAnnotations.openMocks(this);
        calculator = new NotificationTargetCalculator(
                interestAreaRepository, userKeywordRepository,
                keywordRepository, storeBusinessHoursRepository
        );
    }

    /**
     * Property 5a: bread.isDeleted=true이면 항상 빈 결과를 반환한다.
     *
     * **Validates: Requirements 7.1**
     */
    @Property(tries = 100)
    void deletedBread_alwaysReturnsEmptyResult(
            @ForAll("deletedBreads") BreadEntity bread,
            @ForAll("activeStores") StoreEntity store
    ) {
        // isDeleted=true인 빵은 전제조건 검증에서 바로 빈 결과 반환
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        assertThat(result.targets())
                .as("isDeleted=true인 빵은 항상 빈 결과를 반환해야 한다")
                .isEmpty();
    }

    /**
     * Property 5b: bread.remainingQuantity≤0이면 항상 빈 결과를 반환한다.
     *
     * **Validates: Requirements 7.2**
     */
    @Property(tries = 100)
    void zeroOrNegativeQuantity_alwaysReturnsEmptyResult(
            @ForAll("zeroQuantityBreads") BreadEntity bread,
            @ForAll("activeStores") StoreEntity store
    ) {
        // remainingQuantity≤0인 빵은 전제조건 검증에서 바로 빈 결과 반환
        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        assertThat(result.targets())
                .as("remainingQuantity≤0인 빵은 항상 빈 결과를 반환해야 한다")
                .isEmpty();
    }

    /**
     * Property 5c: store.isActive=false이면 (isSelling()=false) 항상 빈 결과를 반환한다.
     *
     * **Validates: Requirements 7.3, 7.5**
     */
    @Property(tries = 100)
    void inactiveStore_alwaysReturnsEmptyResult(
            @ForAll("validBreads") BreadEntity bread,
            @ForAll("inactiveStores") StoreEntity store
    ) {
        // store.isActive=false → SellingStatusUtil.isSelling()=false → 빈 결과
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(anyLong()))
                .willReturn(List.of());

        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        assertThat(result.targets())
                .as("store.isActive=false이면 항상 빈 결과를 반환해야 한다")
                .isEmpty();
    }

    /**
     * Property 5d: 전제조건 미충족 조합 (isDeleted, remainingQuantity≤0, isActive=false 중 하나 이상)
     * 어떤 조합이든 항상 빈 결과를 반환한다.
     *
     * **Validates: Requirements 7.1, 7.2, 7.3, 7.5**
     */
    @Property(tries = 100)
    void anyPreconditionFailure_alwaysReturnsEmptyResult(
            @ForAll("invalidScenarios") InvalidScenario scenario
    ) {
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(anyLong()))
                .willReturn(List.of());

        NotificationTargetResult result = calculator.calculateTargets(
                scenario.bread(), scenario.store(), FIXED_CLOCK);

        assertThat(result.targets())
                .as("전제조건 미충족 시 항상 빈 결과를 반환해야 한다 (isDeleted=%b, quantity=%d, isActive=%b)",
                        scenario.bread().isDeleted(),
                        scenario.bread().getRemainingQuantity(),
                        scenario.store().getIsActive())
                .isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Record for combined scenario
    // ──────────────────────────────────────────────────────────────────────

    record InvalidScenario(BreadEntity bread, StoreEntity store) {}

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * isDeleted=true인 빵을 생성합니다.
     * remainingQuantity는 양수로 설정하여 isDeleted만으로 전제조건 실패를 검증합니다.
     */
    @Provide
    Arbitrary<BreadEntity> deletedBreads() {
        return Arbitraries.integers().between(1, 100).map(quantity -> {
            BreadEntity bread = BreadEntity.builder()
                    .storeId(1L)
                    .name("테스트빵")
                    .description("설명")
                    .originalPrice(5000)
                    .salePrice(3000)
                    .remainingQuantity(quantity)
                    .build();
            ReflectionTestUtils.setField(bread, "id", 1L);
            bread.softDelete(LocalDateTime.now());
            return bread;
        });
    }

    /**
     * remainingQuantity=0인 빵을 생성합니다 (isDeleted=false).
     * BreadEntity의 validateFields에서 remainingQuantity < 0은 예외를 던지므로 0만 사용합니다.
     */
    @Provide
    Arbitrary<BreadEntity> zeroQuantityBreads() {
        return Arbitraries.just(0).map(quantity -> {
            BreadEntity bread = BreadEntity.builder()
                    .storeId(1L)
                    .name("테스트빵")
                    .description("설명")
                    .originalPrice(5000)
                    .salePrice(3000)
                    .remainingQuantity(0)
                    .build();
            ReflectionTestUtils.setField(bread, "id", 1L);
            return bread;
        });
    }

    /**
     * 유효한 빵을 생성합니다 (isDeleted=false, remainingQuantity > 0).
     * store.isActive=false 테스트에서 사용합니다.
     */
    @Provide
    Arbitrary<BreadEntity> validBreads() {
        return Arbitraries.integers().between(1, 100).map(quantity -> {
            BreadEntity bread = BreadEntity.builder()
                    .storeId(1L)
                    .name("테스트빵")
                    .description("설명")
                    .originalPrice(5000)
                    .salePrice(3000)
                    .remainingQuantity(quantity)
                    .build();
            ReflectionTestUtils.setField(bread, "id", 1L);
            return bread;
        });
    }

    /**
     * isActive=true인 매장을 생성합니다.
     */
    @Provide
    Arbitrary<StoreEntity> activeStores() {
        return Arbitraries.just(createStore(true));
    }

    /**
     * isActive=false인 매장을 생성합니다.
     * SellingStatusUtil.isSelling()이 false를 반환하게 됩니다.
     */
    @Provide
    Arbitrary<StoreEntity> inactiveStores() {
        return Arbitraries.just(createStore(false));
    }

    /**
     * 전제조건 미충족 조합을 생성합니다.
     * 최소 하나의 전제조건이 실패하는 시나리오를 생성합니다:
     * - isDeleted=true, OR
     * - remainingQuantity=0, OR
     * - store.isActive=false
     */
    @Provide
    Arbitrary<InvalidScenario> invalidScenarios() {
        // 3가지 실패 유형 중 하나 이상을 랜덤으로 선택
        Arbitrary<Boolean> isDeletedArb = Arbitraries.of(true, false);
        Arbitrary<Integer> quantityArb = Arbitraries.of(0, 1, 5, 10);
        Arbitrary<Boolean> isActiveArb = Arbitraries.of(true, false);

        return Combinators.combine(isDeletedArb, quantityArb, isActiveArb)
                .as((isDeleted, quantity, isActive) -> {
                    BreadEntity bread = BreadEntity.builder()
                            .storeId(1L)
                            .name("테스트빵")
                            .description("설명")
                            .originalPrice(5000)
                            .salePrice(3000)
                            .remainingQuantity(quantity)
                            .build();
                    ReflectionTestUtils.setField(bread, "id", 1L);
                    if (isDeleted) {
                        bread.softDelete(LocalDateTime.now());
                    }

                    StoreEntity store = createStore(isActive);
                    return new InvalidScenario(bread, store);
                })
                // 최소 하나의 전제조건이 실패해야 함
                .filter(scenario ->
                        scenario.bread().isDeleted()
                                || scenario.bread().getRemainingQuantity() <= 0
                                || !scenario.store().getIsActive()
                );
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helper methods
    // ──────────────────────────────────────────────────────────────────────

    private static StoreEntity createStore(boolean isActive) {
        StoreEntity store = StoreEntity.builder()
                .userId(100L)
                .name("테스트매장")
                .phoneNumber("010-1234-5678")
                .description("매장 설명")
                .addressLine1("서울시 강남구")
                .addressLine2("1층")
                .latitude(BigDecimal.valueOf(37.5))
                .longitude(BigDecimal.valueOf(127.0))
                .build();
        ReflectionTestUtils.setField(store, "id", 1L);
        ReflectionTestUtils.setField(store, "isActive", isActive);
        return store;
    }
}
