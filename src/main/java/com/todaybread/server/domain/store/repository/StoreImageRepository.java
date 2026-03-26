package com.todaybread.server.domain.store.repository;

import com.todaybread.server.domain.store.entity.StoreImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 가게 이미지 리포지터리입니다.
 */
@Repository
public interface StoreImageRepository extends JpaRepository<StoreImageEntity, Long> {
    List<StoreImageEntity> findByStoreIdOrderByDisplayOrderAsc(Long storeId);
    void deleteByStoreId(Long storeId);
}
