package com.todaybread.server.domain.store.repository;

import com.todaybread.server.domain.store.entity.FavouriteStoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 단골 가게 설정을 위한 리포지터리입니다.
 */
@Repository
public interface FavouriteStoreRepository extends JpaRepository<FavouriteStoreEntity, Long> {
}
