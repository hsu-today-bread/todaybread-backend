package com.todaybread.server.domain.store.repository;

import com.todaybread.server.domain.store.entity.StoreEntity;
import jakarta.annotation.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * store 리포지터리입니다.
 */
@Resource
public interface StoreRepository extends JpaRepository<StoreEntity, Long> {
    boolean existsByUserIdAndIsActiveTrue(Long userId);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<StoreEntity> findByUserIdAndIsActiveTrue(Long userId);
    Optional<StoreEntity> findByIdAndIsActiveTrue(Long id);

    @Query(value = """
            SELECT s.*, (
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
    List<Object[]> findActiveStoresWithinRadius(
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng,
            @Param("radius") int radius,
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng
    );
}
