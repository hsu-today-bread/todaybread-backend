package com.todaybread.server.domain.bread.repository;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 음식 관련 리포지터리를 만듭니다.
 */
public interface BreadRepository extends JpaRepository<BreadEntity, Long> {

    // === 판매/노출 경로용 (삭제된 빵 제외) ===

    /**
     * 가게의 활성 빵 목록을 조회합니다 (삭제되지 않은 것만).
     *
     * @param storeId 가게 ID
     * @return 삭제되지 않은 빵 엔티티 목록
     */
    List<BreadEntity> findByStoreIdAndIsDeletedFalse(Long storeId);

    /**
     * 여러 가게의 활성 빵을 일괄 조회합니다 (삭제되지 않은 것만).
     *
     * @param storeIds 가게 ID 목록
     * @return 삭제되지 않은 빵 엔티티 목록
     */
    List<BreadEntity> findByStoreIdInAndIsDeletedFalse(List<Long> storeIds);

    /**
     * 단일 빵을 조회합니다 (삭제되지 않은 것만).
     *
     * @param id 빵 ID
     * @return 삭제되지 않은 빵 엔티티 (Optional)
     */
    Optional<BreadEntity> findByIdAndIsDeletedFalse(Long id);

    // === 과거 데이터 경로용 (기존 메서드 유지, 필터 없음) ===

    /**
     * 가게 ID로 해당 가게의 빵 목록을 조회합니다 (삭제 여부 무관).
     * 과거 데이터 접근 경로에서 사용합니다.
     *
     * @param storeId 가게 ID
     * @return 빵 엔티티 목록
     */
    List<BreadEntity> findByStoreId(Long storeId);

    /**
     * 여러 가게 ID에 해당하는 빵을 일괄 조회합니다 (삭제 여부 무관).
     * 과거 데이터 접근 경로에서 사용합니다.
     *
     * @param storeIds 가게 ID 목록
     * @return 빵 엔티티 목록
     */
    List<BreadEntity> findByStoreIdIn(List<Long> storeIds);

    /**
     * 비관적 락으로 빵 엔티티를 일괄 조회합니다 (삭제 여부 무관).
     * 주문 생성 시 동시성 제어를 위해 사용합니다.
     *
     * @param ids 빵 ID 목록
     * @return 빵 엔티티 목록 (락 획득)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BreadEntity b WHERE b.id IN :ids ORDER BY b.id")
    List<BreadEntity> findAllByIdWithLock(@Param("ids") List<Long> ids);
}
