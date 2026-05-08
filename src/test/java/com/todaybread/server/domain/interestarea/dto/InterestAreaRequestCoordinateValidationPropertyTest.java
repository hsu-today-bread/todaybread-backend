package com.todaybread.server.domain.interestarea.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Feature: keyword-area-notification, Property 2: 좌표 유효성 검증
/**
 * Property 2: 좌표 유효성 검증
 *
 * 범위 밖 좌표(latitude: [-90, 90] 밖, longitude: [-180, 180] 밖)로
 * InterestAreaRequest를 생성하면 Bean Validation이 항상 실패해야 한다.
 *
 * **Validates: Requirements 1.3, 1.4, 3.3**
 */
@Tag("Feature: keyword-area-notification, Property 2: 좌표 유효성 검증")
class InterestAreaRequestCoordinateValidationPropertyTest {

    private static final Validator validator;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    /**
     * Property 2a: latitude가 [-90, 90] 범위 밖이면 검증 실패해야 한다.
     *
     * **Validates: Requirements 1.3, 3.3**
     */
    @Property(tries = 100)
    void invalidLatitude_alwaysFailsValidation(
            @ForAll("invalidLatitudes") BigDecimal invalidLatitude
    ) {
        InterestAreaRequest request = new InterestAreaRequest(
                "테스트지역",
                "서울시 강남구 테스트로 1",
                invalidLatitude,
                BigDecimal.valueOf(127.0)
        );

        Set<ConstraintViolation<InterestAreaRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("latitude"));
    }

    /**
     * Property 2b: longitude가 [-180, 180] 범위 밖이면 검증 실패해야 한다.
     *
     * **Validates: Requirements 1.4, 3.3**
     */
    @Property(tries = 100)
    void invalidLongitude_alwaysFailsValidation(
            @ForAll("invalidLongitudes") BigDecimal invalidLongitude
    ) {
        InterestAreaRequest request = new InterestAreaRequest(
                "테스트지역",
                "서울시 강남구 테스트로 1",
                BigDecimal.valueOf(37.5),
                invalidLongitude
        );

        Set<ConstraintViolation<InterestAreaRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("longitude"));
    }

    /**
     * Property 2c: latitude와 longitude 모두 범위 밖이면 검증 실패해야 한다.
     *
     * **Validates: Requirements 1.3, 1.4, 3.3**
     */
    @Property(tries = 100)
    void bothInvalidCoordinates_alwaysFailsValidation(
            @ForAll("invalidLatitudes") BigDecimal invalidLatitude,
            @ForAll("invalidLongitudes") BigDecimal invalidLongitude
    ) {
        InterestAreaRequest request = new InterestAreaRequest(
                "테스트지역",
                "서울시 강남구 테스트로 1",
                invalidLatitude,
                invalidLongitude
        );

        Set<ConstraintViolation<InterestAreaRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("latitude"));
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("longitude"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<BigDecimal> invalidLatitudes() {
        return Arbitraries.oneOf(
                // latitude < -90
                Arbitraries.doubles().lessThan(-90.0).map(BigDecimal::valueOf),
                // latitude > 90
                Arbitraries.doubles().greaterThan(90.0).map(BigDecimal::valueOf)
        );
    }

    @Provide
    Arbitrary<BigDecimal> invalidLongitudes() {
        return Arbitraries.oneOf(
                // longitude < -180
                Arbitraries.doubles().lessThan(-180.0).map(BigDecimal::valueOf),
                // longitude > 180
                Arbitraries.doubles().greaterThan(180.0).map(BigDecimal::valueOf)
        );
    }
}
