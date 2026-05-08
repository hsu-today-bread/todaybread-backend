package com.todaybread.server.domain.interestarea.service;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 8: 비트리거 재고 증가
 *
 * For any remainingQuantity가 1 이상인 빵에 대해 수량이 더 증가하는 경우,
 * 알림 대상 계산이 트리거되지 않아야 한다.
 *
 * 트리거 조건: previousQuantity == 0 && newQuantity >= 1 (품절 해제)
 * 비트리거 조건: previousQuantity >= 1 (이미 재고 있음) → 수량 증가 시 트리거 안 됨
 *
 * 이 테스트는 NotificationTargetCalculator.calculateTargets()의 호출 조건을 문서화하고 검증한다.
 * calculateTargets 자체는 "새 등록"과 "비제로 재고 증가"를 구분하지 않으므로,
 * 호출 측(BreadService)에서 적용해야 할 트리거 조건 로직을 프로퍼티 테스트로 검증한다.
 *
 * **Validates: Requirements 6.3**
 */
@Tag("Feature: keyword-area-notification, Property 8: 비트리거 재고 증가")
class NotificationTriggerConditionPropertyTest {

    private final NotificationTargetCalculator calculator =
            new NotificationTargetCalculator(null, null, null, null);

    /**
     * Property 8: previousQuantity >= 1인 상태에서 수량이 증가해도 트리거되지 않음을 검증.
     *
     * For any previousQuantity >= 1, newQuantity > previousQuantity:
     * shouldTriggerNotification은 항상 false를 반환해야 한다.
     *
     * **Validates: Requirements 6.3**
     */
    @Property(tries = 100)
    void nonZeroStock_increase_doesNotTriggerNotification(
            @ForAll("previousQuantityAtLeastOne") int previousQuantity,
            @ForAll("positiveIncrements") int increment
    ) {
        int newQuantity = previousQuantity + increment;

        boolean shouldTrigger = calculator.shouldTriggerForStockChange(previousQuantity, newQuantity);

        assertThat(shouldTrigger)
                .as("previousQuantity=%d, newQuantity=%d → 트리거되지 않아야 함", previousQuantity, newQuantity)
                .isFalse();
    }

    /**
     * 보완 검증: previousQuantity == 0이고 newQuantity >= 1이면 트리거되어야 함.
     * 이는 Property 8의 반대 케이스로, 트리거 조건의 정확성을 함께 검증한다.
     */
    @Property(tries = 100)
    void zeroStock_toPositive_triggersNotification(
            @ForAll("positiveNewQuantities") int newQuantity
    ) {
        int previousQuantity = 0;

        boolean shouldTrigger = calculator.shouldTriggerForStockChange(previousQuantity, newQuantity);

        assertThat(shouldTrigger)
                .as("previousQuantity=0, newQuantity=%d → 트리거되어야 함", newQuantity)
                .isTrue();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 1 이상의 previousQuantity를 생성합니다.
     * 이미 재고가 있는 상태를 나타냅니다.
     */
    @Provide
    Arbitrary<Integer> previousQuantityAtLeastOne() {
        return Arbitraries.integers().between(1, 10000);
    }

    /**
     * 양수 증가분을 생성합니다.
     * 재고가 증가하는 시나리오를 나타냅니다.
     */
    @Provide
    Arbitrary<Integer> positiveIncrements() {
        return Arbitraries.integers().between(1, 1000);
    }

    /**
     * 1 이상의 새 수량을 생성합니다.
     * 품절 해제(0 → 1+) 시나리오를 나타냅니다.
     */
    @Provide
    Arbitrary<Integer> positiveNewQuantities() {
        return Arbitraries.integers().between(1, 10000);
    }
}
