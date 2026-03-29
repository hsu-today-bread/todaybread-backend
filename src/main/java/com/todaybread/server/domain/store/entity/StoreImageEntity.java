package com.todaybread.server.domain.store.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * store_image 테이블 엔티티를 정의합니다.
 */
@Entity
@Table(name = "store_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreImageEntity extends BaseEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Builder
    private StoreImageEntity(Long storeId, String originalFilename,
                             String storedFilename, String filePath,
                             Integer displayOrder) {
        this.storeId = storeId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
        this.displayOrder = displayOrder;
    }
}
