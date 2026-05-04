/*
 리뷰 기능을 위한 테이블 생성 및 store 테이블 변경 마이그레이션입니다.
 review, review_image 테이블을 생성하고, store 테이블에 평점 집계 컬럼을 추가합니다.
 */

-- ============================================================
-- review
-- ============================================================

CREATE TABLE review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    bread_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_review_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_review_store FOREIGN KEY (store_id) REFERENCES store(id),
    CONSTRAINT fk_review_bread FOREIGN KEY (bread_id) REFERENCES bread(id),
    CONSTRAINT fk_review_order_item FOREIGN KEY (order_item_id) REFERENCES order_item(id),
    CONSTRAINT uk_review_user_id_order_item_id UNIQUE (user_id, order_item_id),
    INDEX idx_review_store_id (store_id),
    INDEX idx_review_user_id (user_id),
    INDEX idx_review_bread_id (bread_id)
);

-- ============================================================
-- review_image
-- ============================================================

CREATE TABLE review_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_review_image_review FOREIGN KEY (review_id) REFERENCES review(id),
    INDEX idx_review_image_review_id (review_id)
);

-- ============================================================
-- store 테이블에 평점 집계 컬럼 추가
-- ============================================================

ALTER TABLE store
    ADD COLUMN rating_sum INT NOT NULL DEFAULT 0,
    ADD COLUMN review_count INT NOT NULL DEFAULT 0;
