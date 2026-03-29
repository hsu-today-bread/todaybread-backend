package com.todaybread.server.domain.bread.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 음식 사진 엔티티를 정의합니다.
 * URL은 storedFilename 기반으로 FileStorage에서 동적 생성합니다.
 */
@Entity
@Table(name = "bread_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BreadImageEntity extends BaseEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bread_id", nullable = false, unique = true)
    private Long breadId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename;

    @Builder
    public BreadImageEntity(Long breadId,
                            String originalFilename,
                            String storedFilename) {
        this.breadId = breadId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
    }
}
