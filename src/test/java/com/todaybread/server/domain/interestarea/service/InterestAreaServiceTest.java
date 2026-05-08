package com.todaybread.server.domain.interestarea.service;

import com.todaybread.server.domain.interestarea.dto.InterestAreaDeleteResponse;
import com.todaybread.server.domain.interestarea.dto.InterestAreaRequest;
import com.todaybread.server.domain.interestarea.dto.InterestAreaResponse;
import com.todaybread.server.domain.interestarea.dto.InterestAreaWrapperResponse;
import com.todaybread.server.domain.interestarea.entity.InterestAreaEntity;
import com.todaybread.server.domain.interestarea.repository.InterestAreaRepository;
import com.todaybread.server.domain.keyword.repository.UserKeywordRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterestAreaServiceTest {

    @Mock
    private InterestAreaRepository interestAreaRepository;

    @Mock
    private UserKeywordRepository userKeywordRepository;

    @InjectMocks
    private InterestAreaService interestAreaService;

    private static InterestAreaEntity interestAreaEntity(Long id, Long userId) {
        InterestAreaEntity entity = InterestAreaEntity.builder()
                .userId(userId)
                .name("강남역")
                .address("서울특별시 강남구 강남대로 396")
                .latitude(BigDecimal.valueOf(37.4979))
                .longitude(BigDecimal.valueOf(127.0276))
                .radiusKm(3.0)
                .build();
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    @Test
    void createInterestArea_success() {
        InterestAreaRequest request = new InterestAreaRequest(
                "강남역",
                "서울특별시 강남구 강남대로 396",
                BigDecimal.valueOf(37.4979),
                BigDecimal.valueOf(127.0276)
        );
        InterestAreaEntity saved = interestAreaEntity(1L, 1L);
        given(interestAreaRepository.save(any(InterestAreaEntity.class))).willReturn(saved);

        InterestAreaResponse response = interestAreaService.createInterestArea(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("강남역");
        assertThat(response.address()).isEqualTo("서울특별시 강남구 강남대로 396");
        assertThat(response.latitude()).isEqualByComparingTo(BigDecimal.valueOf(37.4979));
        assertThat(response.longitude()).isEqualByComparingTo(BigDecimal.valueOf(127.0276));
        assertThat(response.radiusKm()).isEqualTo(3.0);
    }

    @Test
    void createInterestArea_duplicateThrowsInterestAreaAlreadyExists() {
        InterestAreaRequest request = new InterestAreaRequest(
                "강남역",
                "서울특별시 강남구 강남대로 396",
                BigDecimal.valueOf(37.4979),
                BigDecimal.valueOf(127.0276)
        );
        given(interestAreaRepository.save(any(InterestAreaEntity.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> interestAreaService.createInterestArea(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INTEREST_AREA_ALREADY_EXISTS);
    }

    @Test
    void getInterestArea_exists() {
        InterestAreaEntity entity = interestAreaEntity(1L, 1L);
        given(interestAreaRepository.findByUserId(1L)).willReturn(Optional.of(entity));

        InterestAreaWrapperResponse response = interestAreaService.getInterestArea(1L);

        assertThat(response.interestArea()).isNotNull();
        assertThat(response.interestArea().id()).isEqualTo(1L);
        assertThat(response.interestArea().name()).isEqualTo("강남역");
        assertThat(response.interestArea().radiusKm()).isEqualTo(3.0);
    }

    @Test
    void getInterestArea_notExists() {
        given(interestAreaRepository.findByUserId(1L)).willReturn(Optional.empty());

        InterestAreaWrapperResponse response = interestAreaService.getInterestArea(1L);

        assertThat(response.interestArea()).isNull();
    }

    @Test
    void updateInterestArea_success() {
        InterestAreaEntity entity = interestAreaEntity(1L, 1L);
        given(interestAreaRepository.findByUserId(1L)).willReturn(Optional.of(entity));

        InterestAreaRequest request = new InterestAreaRequest(
                "홍대입구",
                "서울특별시 마포구 양화로 160",
                BigDecimal.valueOf(37.5563),
                BigDecimal.valueOf(126.9236)
        );

        InterestAreaResponse response = interestAreaService.updateInterestArea(1L, request);

        assertThat(response.name()).isEqualTo("홍대입구");
        assertThat(response.address()).isEqualTo("서울특별시 마포구 양화로 160");
        assertThat(response.latitude()).isEqualByComparingTo(BigDecimal.valueOf(37.5563));
        assertThat(response.longitude()).isEqualByComparingTo(BigDecimal.valueOf(126.9236));
        assertThat(response.radiusKm()).isEqualTo(3.0);
    }

    @Test
    void updateInterestArea_notFoundThrowsInterestAreaNotFound() {
        given(interestAreaRepository.findByUserId(1L)).willReturn(Optional.empty());

        InterestAreaRequest request = new InterestAreaRequest(
                "홍대입구",
                "서울특별시 마포구 양화로 160",
                BigDecimal.valueOf(37.5563),
                BigDecimal.valueOf(126.9236)
        );

        assertThatThrownBy(() -> interestAreaService.updateInterestArea(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INTEREST_AREA_NOT_FOUND);
    }

    @Test
    void deleteInterestArea_successWithKeywords() {
        InterestAreaEntity entity = interestAreaEntity(1L, 1L);
        given(interestAreaRepository.findByUserId(1L)).willReturn(Optional.of(entity));
        given(userKeywordRepository.existsByUserId(1L)).willReturn(true);

        InterestAreaDeleteResponse response = interestAreaService.deleteInterestArea(1L);

        assertThat(response.success()).isTrue();
        assertThat(response.keywordNotificationDisabled()).isTrue();
        verify(interestAreaRepository).delete(entity);
    }

    @Test
    void deleteInterestArea_successWithoutKeywords() {
        InterestAreaEntity entity = interestAreaEntity(1L, 1L);
        given(interestAreaRepository.findByUserId(1L)).willReturn(Optional.of(entity));
        given(userKeywordRepository.existsByUserId(1L)).willReturn(false);

        InterestAreaDeleteResponse response = interestAreaService.deleteInterestArea(1L);

        assertThat(response.success()).isTrue();
        assertThat(response.keywordNotificationDisabled()).isFalse();
        verify(interestAreaRepository).delete(entity);
    }

    @Test
    void deleteInterestArea_notFoundThrowsInterestAreaNotFound() {
        given(interestAreaRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> interestAreaService.deleteInterestArea(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INTEREST_AREA_NOT_FOUND);
    }
}
