/*
 가게 이미지 관리를 위한 테이블을 정의합니다.
 */
CREATE TABLE store_image (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT       NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255)  NOT NULL UNIQUE,
    file_path       VARCHAR(500)  NOT NULL,
    display_order   INT           NOT NULL,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_store_image_store FOREIGN KEY (store_id) REFERENCES store(id) ON DELETE CASCADE,
    CONSTRAINT uk_store_image_store_id_display_order UNIQUE (store_id, display_order),
    INDEX idx_store_image_store_id (store_id)
);
