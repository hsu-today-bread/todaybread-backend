package com.todaybread.server.domain.store.repository;

import com.todaybread.server.domain.store.entity.FavouriteStoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 단골 가게 설정을 위한 리포지터리입니다.
 */
@Repository
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
     * 특정 사용자의 단골 가게 등록 수를 조회합니다.
     *
     * @param userId 유저 ID
     * @return 단골 가게 수
     */
    long countByUserId(Long userId);
}
