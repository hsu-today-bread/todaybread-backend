package com.todaybread.server.domain.bread.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 음식 사진 엔티티를 정의합니다.
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

    @Column(name = "bread_id", nullable = false)
    private Long breadId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Builder
    public BreadImageEntity(Long breadId,
                            String originalFilename,
                            String storedFilename,
                            String filePath) {
        this.breadId = breadId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
    }

    /**
     * 이미지를 업데이트합니다.
     * @param originalFilename 실제 파일 이름
     * @param storedFilename 저장 파일 이름
     * @param filePath 경로
     */
    public void updateImage(String originalFilename, String storedFilename, String filePath) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
    }
}
