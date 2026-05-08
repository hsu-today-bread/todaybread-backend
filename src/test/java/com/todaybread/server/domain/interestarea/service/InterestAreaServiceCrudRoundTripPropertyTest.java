package com.todaybread.server.domain.interestarea.service;

import com.todaybread.server.domain.interestarea.dto.InterestAreaRequest;
import com.todaybread.server.domain.interestarea.dto.InterestAreaResponse;
import com.todaybread.server.domain.interestarea.entity.InterestAreaEntity;
import com.todaybread.server.domain.interestarea.repository.InterestAreaRepository;
import com.todaybread.server.domain.keyword.repository.UserKeywordRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Property 1: 관심지역 CRUD 라운드트립 (radiusKm 불변)
 *
 * For any 유효한 입력(name 1~50자, address 1~200자, latitude -90~90, longitude -180~180)으로
 * 관심지역을 생성하거나 수정하면, 반환된 응답의 모든 필드가 입력과 일치하고 radiusKm은 항상 3.0이어야 한다.
 *
 * **Validates: Requirements 1.1, 3.1, 3.4**
 */
@Tag("Feature: keyword-area-notification, Property 1: 관심지역 CRUD 라운드트립 (radiusKm 불변)")
class InterestAreaServiceCrudRoundTripPropertyTest {

    @Mock
    private InterestAreaRepository interestAreaRepository;

    @Mock
    private UserKeywordRepository userKeywordRepository;

    private InterestAreaService interestAreaService;

    @BeforeProperty
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interestAreaService = new InterestAreaService(interestAreaRepository, userKeywordRepository);
    }

    /**
     * Property 1-1: createInterestArea() 반환값은 입력과 일치하고 radiusKm=3.0이다.
     *
     * **Validates: Requirements 1.1, 3.4**
     */
    @Property(tries = 100)
    void createInterestArea_returnsMatchingFieldsAndFixedRadius(
            @ForAll("validInterestAreaInputs") InterestAreaInput input
    ) {
        // Arrange
        Long userId = 1L;
        Long savedId = 42L;

        given(interestAreaRepository.save(any(InterestAreaEntity.class)))
                .willAnswer(invocation -> {
                    InterestAreaEntity entity = invocation.getArgument(0);
                    ReflectionTestUtils.setField(entity, "id", savedId);
                    return entity;
                });

        InterestAreaRequest request = new InterestAreaRequest(
                input.name(), input.address(), input.latitude(), input.longitude()
        );

        // Act
        InterestAreaResponse response = interestAreaService.createInterestArea(userId, request);

        // Assert: 모든 필드가 입력과 일치
        assertThat(response.id()).isEqualTo(savedId);
        assertThat(response.name()).isEqualTo(input.name());
        assertThat(response.address()).isEqualTo(input.address());
        assertThat(response.latitude()).isEqualByComparingTo(input.latitude());
        assertThat(response.longitude()).isEqualByComparingTo(input.longitude());

        // Assert: radiusKm은 항상 3.0
        assertThat(response.radiusKm()).isEqualTo(3.0);
    }

    /**
     * Property 1-2: updateInterestArea() 반환값은 새 입력과 일치하고 radiusKm=3.0이 유지된다.
     *
     * **Validates: Requirements 3.1, 3.4**
     */
    @Property(tries = 100)
    void updateInterestArea_returnsUpdatedFieldsAndFixedRadius(
            @ForAll("validInterestAreaInputs") InterestAreaInput originalInput,
            @ForAll("validInterestAreaInputs") InterestAreaInput updateInput
    ) {
        // Arrange
        Long userId = 1L;
        Long entityId = 42L;

        InterestAreaEntity existingEntity = InterestAreaEntity.builder()
                .userId(userId)
                .name(originalInput.name())
                .address(originalInput.address())
                .latitude(originalInput.latitude())
                .longitude(originalInput.longitude())
                .radiusKm(3.0)
                .build();
        ReflectionTestUtils.setField(existingEntity, "id", entityId);

        given(interestAreaRepository.findByUserId(userId))
                .willReturn(Optional.of(existingEntity));

        InterestAreaRequest request = new InterestAreaRequest(
                updateInput.name(), updateInput.address(), updateInput.latitude(), updateInput.longitude()
        );

        // Act
        InterestAreaResponse response = interestAreaService.updateInterestArea(userId, request);

        // Assert: 모든 필드가 새 입력과 일치
        assertThat(response.id()).isEqualTo(entityId);
        assertThat(response.name()).isEqualTo(updateInput.name());
        assertThat(response.address()).isEqualTo(updateInput.address());
        assertThat(response.latitude()).isEqualByComparingTo(updateInput.latitude());
        assertThat(response.longitude()).isEqualByComparingTo(updateInput.longitude());

        // Assert: radiusKm은 수정 후에도 항상 3.0
        assertThat(response.radiusKm()).isEqualTo(3.0);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Providers
    // ──────────────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<InterestAreaInput> validInterestAreaInputs() {
        Arbitrary<String> names = Arbitraries.strings()
                .ofMinLength(1).ofMaxLength(50)
                .alpha().numeric().withChars(' ', '-', '_')
                .filter(s -> !s.isBlank());

        Arbitrary<String> addresses = Arbitraries.strings()
                .ofMinLength(1).ofMaxLength(200)
                .alpha().numeric().withChars(' ', '-', '_', ',', '.')
                .filter(s -> !s.isBlank());

        Arbitrary<BigDecimal> latitudes = Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(-90), BigDecimal.valueOf(90))
                .ofScale(7);

        Arbitrary<BigDecimal> longitudes = Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(-180), BigDecimal.valueOf(180))
                .ofScale(7);

        return Combinators.combine(names, addresses, latitudes, longitudes)
                .as(InterestAreaInput::new);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Internal record for test data
    // ──────────────────────────────────────────────────────────────────────

    record InterestAreaInput(String name, String address, BigDecimal latitude, BigDecimal longitude) {}
}
