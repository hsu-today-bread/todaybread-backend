package com.todaybread.server.domain.bread.repository;

import com.todaybread.server.domain.bread.entity.BreadImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 음식 사진을 위한 리포지터리입니다.
 */
public interface BreadImageRepository extends JpaRepository<BreadImageEntity, Long> {
    Optional<BreadImageEntity> findByStoreId(Long stockId);
    void deleteByStoreId(Long stockId);
}
