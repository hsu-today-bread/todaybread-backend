package com.todaybread.server.domain.review.repository;

import com.todaybread.server.domain.review.entity.ReviewImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 리뷰 이미지 리포지터리입니다.
 */
public interface ReviewImageRepository extends JpaRepository<ReviewImageEntity, Long> {

    /**
     * 리뷰 ID로 이미지 엔티티 목록을 조회합니다.
     *
     * @param reviewId 리뷰 ID
     * @return 이미지 엔티티 목록
     */
    List<ReviewImageEntity> findByReviewId(Long reviewId);

    /**
     * 여러 리뷰 ID에 해당하는 이미지 엔티티를 일괄 조회합니다.
     * N+1 문제를 방지하기 위해 사용합니다.
     *
     * @param reviewIds 리뷰 ID 목록
     * @return 이미지 엔티티 목록
     */
    List<ReviewImageEntity> findByReviewIdIn(List<Long> reviewIds);
}
