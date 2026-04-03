package com.todaybread.server.domain.store.repository;

import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 가게 영업시간 리포지터리입니다.
 */
public interface StoreBusinessHoursRepository extends JpaRepository<StoreBusinessHoursEntity, Long> {

    /**
     * 가게 ID로 7개의 요일별 영업시간을 조회합니다.
     *
     * @param storeId 가게 ID
     * @return 영업시간 엔티티 목록
     */
    List<StoreBusinessHoursEntity> findByStoreIdOrderByDayOfWeekAsc(Long storeId);

    /**
     * 가게 ID와 요일로 특정 요일의 영업시간을 조회합니다.
     *
     * @param storeId   가게 ID
     * @param dayOfWeek 요일 (1=월, 2=화, ..., 7=일)
     * @return 영업시간 엔티티 (없으면 빈 Optional)
     */
    Optional<StoreBusinessHoursEntity> findByStoreIdAndDayOfWeek(Long storeId, Integer dayOfWeek);

    /**
     * 여러 가게 ID에 해당하는 영업시간을 일괄 조회합니다.
     *
     * @param storeIds 가게 ID 목록
     * @return 영업시간 엔티티 목록
     */
    List<StoreBusinessHoursEntity> findByStoreIdIn(List<Long> storeIds);

    /**
     * 가게 ID에 해당하는 영업시간 데이터를 모두 삭제합니다.
     *
     * @param storeId 가게 ID
     */
    @Modifying
    @Query("DELETE FROM StoreBusinessHoursEntity e WHERE e.storeId = :storeId")
    void deleteByStoreId(@Param("storeId") Long storeId);
}
