package com.todaybread.server.domain.review.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리뷰 이미지 엔티티를 정의합니다.
 * URL은 storedFilename 기반으로 FileStorage에서 동적 생성합니다.
 */
@Entity
@Table(name = "review_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewImageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename;

    @Builder
    public ReviewImageEntity(Long reviewId,
                             String originalFilename,
                             String storedFilename) {
        this.reviewId = reviewId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
    }
}
