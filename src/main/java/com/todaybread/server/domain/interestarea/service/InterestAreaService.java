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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관심지역 CRUD를 담당하는 서비스입니다.
 * 유저당 1개의 관심지역만 등록할 수 있으며, radiusKm은 서버 상수(3.0km)로 관리합니다.
 */
@Service
@RequiredArgsConstructor
public class InterestAreaService {

    private static final double RADIUS_KM = 3.0;

    private final InterestAreaRepository interestAreaRepository;
    private final UserKeywordRepository userKeywordRepository;

    /**
     * 관심지역을 등록합니다.
     * 유저당 1개만 등록 가능하며, 중복 시 INTEREST_AREA_ALREADY_EXISTS 에러를 반환합니다.
     *
     * @param userId  유저 ID
     * @param request 관심지역 등록 요청
     * @return 등록된 관심지역 응답
     */
    @Transactional
    public InterestAreaResponse createInterestArea(Long userId, InterestAreaRequest request) {
        InterestAreaEntity entity = InterestAreaEntity.builder()
                .userId(userId)
                .name(request.name())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .radiusKm(RADIUS_KM)
                .build();

        try {
            InterestAreaEntity saved = interestAreaRepository.save(entity);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.INTEREST_AREA_ALREADY_EXISTS);
        }
    }

    /**
     * 유저의 관심지역을 조회합니다.
     * 관심지역이 존재하면 상세 정보를, 존재하지 않으면 null을 래퍼 DTO로 반환합니다.
     *
     * @param userId 유저 ID
     * @return 관심지역 래퍼 응답 (interestArea가 null일 수 있음)
     */
    @Transactional(readOnly = true)
    public InterestAreaWrapperResponse getInterestArea(Long userId) {
        return interestAreaRepository.findByUserId(userId)
                .map(entity -> new InterestAreaWrapperResponse(toResponse(entity)))
                .orElse(new InterestAreaWrapperResponse(null));
    }

    /**
     * 관심지역을 수정합니다.
     * 관심지역이 없으면 INTEREST_AREA_NOT_FOUND 에러를 반환합니다.
     * radiusKm은 변경하지 않습니다.
     *
     * @param userId  유저 ID
     * @param request 관심지역 수정 요청
     * @return 수정된 관심지역 응답
     */
    @Transactional
    public InterestAreaResponse updateInterestArea(Long userId, InterestAreaRequest request) {
        InterestAreaEntity entity = interestAreaRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTEREST_AREA_NOT_FOUND));

        entity.updateInfo(request.name(), request.address(), request.latitude(), request.longitude());

        return toResponse(entity);
    }

    /**
     * 관심지역을 삭제합니다.
     * 관심지역이 없으면 INTEREST_AREA_NOT_FOUND 에러를 반환합니다.
     * 삭제 후 유저의 키워드 존재 여부에 따라 keywordNotificationDisabled 플래그를 계산합니다.
     *
     * @param userId 유저 ID
     * @return 삭제 응답 (keywordNotificationDisabled 포함)
     */
    @Transactional
    public InterestAreaDeleteResponse deleteInterestArea(Long userId) {
        InterestAreaEntity entity = interestAreaRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTEREST_AREA_NOT_FOUND));

        interestAreaRepository.delete(entity);

        boolean keywordNotificationDisabled = userKeywordRepository.existsByUserId(userId);

        return new InterestAreaDeleteResponse(true, keywordNotificationDisabled);
    }

    private InterestAreaResponse toResponse(InterestAreaEntity entity) {
        return new InterestAreaResponse(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getRadiusKm()
        );
    }
}
