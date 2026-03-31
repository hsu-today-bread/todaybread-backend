package com.todaybread.server.domain.bread.repository;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 음식 관련 리포지터리를 만듭니다.
 */
public interface BreadRepository extends JpaRepository<BreadEntity, Long> {
    List<BreadEntity> findByStoreId(Long storeId);

    List<BreadEntity> findByStoreIdIn(List<Long> storeIds);
}
