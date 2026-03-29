package com.todaybread.server.domain.bread.repository;

import com.todaybread.server.domain.bread.entity.BreadImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 빵 이미지 리포지터리입니다.
 */
public interface BreadImageRepository extends JpaRepository<BreadImageEntity, Long> {
    Optional<BreadImageEntity> findByBreadId(Long breadId);
    List<BreadImageEntity> findByBreadIdIn(List<Long> breadIds);
}
