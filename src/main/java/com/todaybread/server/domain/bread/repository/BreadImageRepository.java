package com.todaybread.server.domain.bread.repository;

import com.todaybread.server.domain.bread.entity.BreadImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 빵 이미지 리포지터리입니다.
 */
public interface BreadImageRepository extends JpaRepository<BreadImageEntity, Long> {

    /**
     * 빵 ID로 이미지 엔티티를 조회합니다.
     *
     * @param breadId 빵 ID
     * @return 이미지 엔티티 (없으면 빈 Optional)
     */
    Optional<BreadImageEntity> findByBreadId(Long breadId);

    /**
     * 여러 빵 ID에 해당하는 이미지 엔티티를 일괄 조회합니다.
     *
     * @param breadIds 빵 ID 목록
     * @return 이미지 엔티티 목록
     */
    List<BreadImageEntity> findByBreadIdIn(List<Long> breadIds);
}
