package com.todaybread.server.domain.bread.repository;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 음식 관련 리포지터리를 만듭니다.
 */
public interface BreadRepository extends JpaRepository<BreadEntity, Long> {

    /**
     * 가게 ID로 해당 가게의 빵 목록을 조회합니다.
     *
     * @param storeId 가게 ID
     * @return 빵 엔티티 목록
     */
    List<BreadEntity> findByStoreId(Long storeId);

    /**
     * 여러 가게 ID에 해당하는 빵을 일괄 조회합니다.
     *
     * @param storeIds 가게 ID 목록
     * @return 빵 엔티티 목록
     */
    List<BreadEntity> findByStoreIdIn(List<Long> storeIds);

    /**
     * 비관적 락으로 빵 엔티티를 일괄 조회합니다.
     * 주문 생성 시 동시성 제어를 위해 사용합니다.
     *
     * @param ids 빵 ID 목록
     * @return 빵 엔티티 목록 (락 획득)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BreadEntity b WHERE b.id IN :ids")
    List<BreadEntity> findAllByIdWithLock(@Param("ids") List<Long> ids);
}
