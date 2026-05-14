package com.todaybread.server.domain.store.repository;

import com.todaybread.server.domain.store.entity.FavouriteStoreEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 단골 가게 설정을 위한 리포지터리입니다.
 */
public interface FavouriteStoreRepository extends JpaRepository<FavouriteStoreEntity, Long> {

    /**
     * 특정 사용자의 특정 가게 단골 설정을 조회합니다.
     *
     * @param userId  유저 ID
     * @param storeId 가게 ID
     * @return 단골 가게 엔티티 (없으면 빈 Optional)
     */
    Optional<FavouriteStoreEntity> findByUserIdAndStoreId(Long userId, Long storeId);

    /**
     * 특정 사용자의 단골 가게 목록을 조회합니다.
     *
     * @param userId 유저 ID
     * @return 단골 가게 엔티티 목록
     */
    List<FavouriteStoreEntity> findByUserId(Long userId);

    /**
     * 특정 가게를 단골로 등록한 유저 목록을 조회합니다.
     *
     * @param storeId 가게 ID
     * @return 해당 가게를 단골로 등록한 엔티티 목록
     */
    List<FavouriteStoreEntity> findByStoreId(Long storeId);

    /**
     * 특정 가게를 단골로 등록한 일반 유저 목록을 조회합니다.
     * 사장님으로 전환된 유저는 재고/신규 빵 단골 매장 알림 대상에서 제외합니다.
     *
     * @param storeId 가게 ID
     * @return 해당 가게를 단골로 등록한 일반 유저의 단골 엔티티 목록
     */
    @Query("""
            SELECT f
            FROM FavouriteStoreEntity f
            JOIN UserEntity u ON u.id = f.userId
            WHERE f.storeId = :storeId
              AND u.isBoss = false
            """)
    List<FavouriteStoreEntity> findByStoreIdForUserNotificationTargets(@Param("storeId") Long storeId);

    /**
     * 특정 사용자의 단골 가게 등록 수를 비관적 락으로 조회합니다.
     * 동시 요청 시 개수 제한을 정확히 보장하기 위해 사용합니다.
     *
     * @param userId 유저 ID
     * @return 단골 가게 수
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(f) FROM FavouriteStoreEntity f WHERE f.userId = :userId")
    long countByUserIdWithLock(@Param("userId") Long userId);
}
