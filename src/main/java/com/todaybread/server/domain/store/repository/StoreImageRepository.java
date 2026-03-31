package com.todaybread.server.domain.store.repository;

import com.todaybread.server.domain.store.entity.StoreImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 가게 이미지 리포지터리입니다.
 */
public interface StoreImageRepository extends JpaRepository<StoreImageEntity, Long> {

    /**
     * 가게 ID로 이미지 목록을 표시 순서 오름차순으로 조회합니다.
     *
     * @param storeId 가게 ID
     * @return 이미지 엔티티 목록 (displayOrder 오름차순)
     */
    List<StoreImageEntity> findByStoreIdOrderByDisplayOrderAsc(Long storeId);

    /**
     * 가게 ID에 해당하는 이미지를 모두 삭제합니다.
     *
     * @param storeId 가게 ID
     */
    void deleteByStoreId(Long storeId);
}
