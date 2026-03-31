package com.todaybread.server.domain.store.repository;

import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * store 리포지터리입니다.
 */
public interface StoreRepository extends JpaRepository<StoreEntity, Long> {

    /**
     * 특정 유저의 활성 가게 존재 여부를 확인합니다.
     *
     * @param userId 유저 ID
     * @return 활성 가게가 있으면 true
     */
    boolean existsByUserIdAndIsActiveTrue(Long userId);

    /**
     * 전화번호 중복 여부를 확인합니다.
     *
     * @param phoneNumber 전화번호
     * @return 존재하면 true
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * 유저 ID로 활성 가게를 조회합니다.
     *
     * @param userId 유저 ID
     * @return 가게 엔티티 (없으면 빈 Optional)
     */
    Optional<StoreEntity> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * 가게 ID로 활성 가게를 조회합니다.
     *
     * @param id 가게 ID
     * @return 가게 엔티티 (없으면 빈 Optional)
     */
    Optional<StoreEntity> findByIdAndIsActiveTrue(Long id);

    /**
     * 사장님의 활성 가게를 조회합니다. 없으면 STORE_NOT_FOUND 예외를 던집니다.
     *
     * @param userId 사장님 ID
     * @return 가게 엔티티
     */
    default StoreEntity getByUserIdAndIsActiveTrue(Long userId) {
        return findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
    }

    /**
     * Haversine 공식을 사용하여 반경 내 활성 가게를 거리순으로 조회합니다.
     * Bounding Box 필터링 후 정밀 거리 계산을 수행합니다.
     *
     * @param lat    유저 위도
     * @param lng    유저 경도
     * @param radius 검색 반경 (km)
     * @param minLat Bounding Box 최소 위도
     * @param maxLat Bounding Box 최대 위도
     * @param minLng Bounding Box 최소 경도
     * @param maxLng Bounding Box 최대 경도
     * @return 가게 ID와 거리를 포함하는 Projection 목록
     */
    @Query(value = """
            SELECT s.id AS storeId, (
                6371 * ACOS(
                    COS(RADIANS(:lat)) * COS(RADIANS(s.latitude))
                    * COS(RADIANS(s.longitude) - RADIANS(:lng))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(s.latitude))
                )
            ) AS distance
            FROM store s
            WHERE s.is_active = true
              AND s.latitude BETWEEN :minLat AND :maxLat
              AND s.longitude BETWEEN :minLng AND :maxLng
            HAVING distance <= :radius
            ORDER BY distance ASC
            """, nativeQuery = true)
    List<StoreDistanceProjection> findActiveStoresWithinRadius(
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng,
            @Param("radius") int radius,
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng
    );
}
