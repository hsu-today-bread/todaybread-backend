package com.todaybread.server.domain.review.repository;

import com.todaybread.server.domain.review.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 리뷰 리포지터리입니다.
 */
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    /**
     * 가게 ID로 리뷰 목록을 페이지네이션하여 조회합니다.
     *
     * @param storeId  가게 ID
     * @param pageable 페이지네이션 정보
     * @return 리뷰 페이지
     */
    Page<ReviewEntity> findByStoreId(Long storeId, Pageable pageable);

    /**
     * 사용자 ID로 리뷰 목록을 페이지네이션하여 조회합니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이지네이션 정보
     * @return 리뷰 페이지
     */
    Page<ReviewEntity> findByUserId(Long userId, Pageable pageable);

    /**
     * 해당 사용자가 해당 주문 항목에 대해 이미 리뷰를 작성했는지 확인합니다.
     *
     * @param userId      사용자 ID
     * @param orderItemId 주문 항목 ID
     * @return 리뷰 존재 여부
     */
    boolean existsByUserIdAndOrderItemId(Long userId, Long orderItemId);

    /**
     * 가게 ID로 이미지가 첨부된 리뷰만 페이지네이션하여 조회합니다.
     * ReviewImageEntity가 존재하는 리뷰만 반환합니다.
     *
     * @param storeId  가게 ID
     * @param pageable 페이지네이션 정보
     * @return 이미지가 있는 리뷰 페이지
     */
    @Query("SELECT r FROM ReviewEntity r WHERE r.storeId = :storeId AND r.id IN (SELECT ri.reviewId FROM ReviewImageEntity ri)")
    Page<ReviewEntity> findByStoreIdWithImages(@Param("storeId") Long storeId, Pageable pageable);

    /**
     * 가게 ID로 이미지가 첨부되지 않은 리뷰만 페이지네이션하여 조회합니다.
     * ReviewImageEntity가 존재하지 않는 리뷰만 반환합니다.
     *
     * @param storeId  가게 ID
     * @param pageable 페이지네이션 정보
     * @return 이미지가 없는 리뷰 페이지
     */
    @Query("SELECT r FROM ReviewEntity r WHERE r.storeId = :storeId AND r.id NOT IN (SELECT ri.reviewId FROM ReviewImageEntity ri)")
    Page<ReviewEntity> findByStoreIdWithoutImages(@Param("storeId") Long storeId, Pageable pageable);
}
