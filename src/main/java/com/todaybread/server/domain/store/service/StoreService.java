package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.store.dto.StoreAddRequest;
import com.todaybread.server.domain.store.dto.StoreAddResponse;
import com.todaybread.server.domain.store.dto.StoreInfo;
import com.todaybread.server.domain.store.dto.StoreStatusResponse;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * store 도메인 서비스 계층입니다.
 */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    /**
     * 사장님 탭 진입 상태를 조회합니다.
     * @param userId 유저 ID
     * @return 가게 등록 응답 DTO
     */
    @Transactional(readOnly = true)
    public StoreStatusResponse getStoreStatus(Long userId){
        Optional<StoreEntity> storeEntityOptional = storeRepository.findByUserIdAndIsActiveTrue(userId);

        // 가게가 없다면...
        if (storeEntityOptional.isEmpty()) {
            return StoreStatusResponse.hasNoStore();
        }

        // 가게가 있는 경우
        StoreEntity storeEntity = storeEntityOptional.get();
        return StoreStatusResponse.hasStore(StoreInfo.getStoreInfo(storeEntity));
    }

    /**
     * 가게 등록 요청을 처리합니다.
     * 이후 가게를 등록합니다.
     *
     * @param userId 유저 ID
     * @param request 요청 DTO
     * @return 응답 DTO
     */
    @Transactional
    public StoreAddResponse addStore(Long userId, StoreAddRequest request) {
        if (storeRepository.existsByUserIdAndIsActiveTrue(userId)) {
            throw new CustomException(ErrorCode.STORE_ALREADY_EXISTS);
        }

        StoreEntity storeEntity = StoreEntity.builder()
                .userId(userId)
                .name(request.name())
                .phoneNumber(request.phone())
                .description(request.description())
                .addressLine1(request.addressLine1())
                .addressLine2(request.addressLine2())
                .latitude(BigDecimal.valueOf(request.latitude()))
                .longitude(BigDecimal.valueOf(request.longitude()))
                .endTime(request.endTime().toLocalTime())
                .lastOrderTime(request.lastOrderTime().toLocalTime())
                .orderTime(request.orderTime())
                .build();

        storeRepository.save(storeEntity);

        return StoreAddResponse.ok(StoreInfo.getStoreInfo(storeEntity));
    }

}
