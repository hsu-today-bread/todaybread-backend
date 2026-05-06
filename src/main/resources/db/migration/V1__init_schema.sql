/*
 TodayBread 전체 스키마 통합 baseline migration (V1~V11 병합)
 신규 환경에서 현재 최종 스키마를 한 번에 구성합니다.
 users, auth, keyword, store, bread, cart, order, payment, review 도메인 테이블과 인덱스를 포함합니다.
 */

-- ============================================================
-- users / auth
-- ============================================================

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(30) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(30) NOT NULL UNIQUE,
    phone_number VARCHAR(30) NOT NULL UNIQUE,
    is_boss BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    token VARCHAR(512) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_refresh_token_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE password_reset_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_password_reset_token_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- keyword
-- ============================================================

CREATE TABLE keyword (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    normalised_text VARCHAR(10) NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_keyword_normalised_text_length CHECK (CHAR_LENGTH(normalised_text) <= 10)
);

CREATE TABLE user_keyword (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    keyword_id BIGINT NOT NULL,
    display_text VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_user_keyword_user_id_keyword_id UNIQUE (user_id, keyword_id),
    CONSTRAINT fk_user_keyword_keyword FOREIGN KEY (keyword_id) REFERENCES keyword(id),
    CONSTRAINT fk_user_keyword_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_user_keyword_display_text_length CHECK (CHAR_LENGTH(display_text) <= 10)
);

-- ============================================================
-- store
-- ============================================================

CREATE TABLE store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    address_line1 VARCHAR(200) NOT NULL,
    address_line2 VARCHAR(200) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    rating_sum INT NOT NULL DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_store_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_store_rating_sum CHECK (rating_sum >= 0),
    CONSTRAINT chk_store_review_count CHECK (review_count >= 0),
    INDEX idx_store_lat_lng (latitude, longitude),
    INDEX idx_store_is_active (is_active)
);

CREATE TABLE favourite_store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_favourite_store_user_id_store_id UNIQUE (user_id, store_id),
    CONSTRAINT fk_favourite_store_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favourite_store_store FOREIGN KEY (store_id) REFERENCES store(id),
    INDEX idx_favourite_store_user_id (user_id)
);

CREATE TABLE store_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_store_image_store FOREIGN KEY (store_id) REFERENCES store(id) ON DELETE CASCADE,
    CONSTRAINT uk_store_image_store_id_display_order UNIQUE (store_id, display_order),
    INDEX idx_store_image_store_id (store_id)
);

CREATE TABLE store_business_hours (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    day_of_week INT NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    start_time TIME NULL,
    end_time TIME NULL,
    last_order_time TIME NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_store_business_hours_store FOREIGN KEY (store_id) REFERENCES store(id),
    CONSTRAINT uk_store_business_hours_store_id_day_of_week UNIQUE (store_id, day_of_week),
    CONSTRAINT chk_store_business_hours_day_of_week CHECK (day_of_week BETWEEN 1 AND 7)
);

-- ============================================================
-- bread
-- ============================================================

CREATE TABLE bread (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    original_price INT NOT NULL,
    sale_price INT NOT NULL,
    remaining_quantity INT NOT NULL,
    description VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_bread_store FOREIGN KEY (store_id) REFERENCES store(id) ON DELETE CASCADE,
    CONSTRAINT chk_bread_original_price CHECK (original_price >= 0),
    CONSTRAINT chk_bread_sale_price CHECK (sale_price >= 0),
    CONSTRAINT chk_bread_remaining_quantity CHECK (remaining_quantity >= 0),
    CONSTRAINT chk_bread_sale_price_lte_original CHECK (sale_price <= original_price),
    INDEX idx_bread_store_id (store_id),
    INDEX idx_bread_is_deleted (is_deleted)
);

CREATE TABLE bread_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bread_id BIGINT NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_bread_image_bread FOREIGN KEY (bread_id) REFERENCES bread(id) ON DELETE CASCADE
);

-- ============================================================
-- cart
-- ============================================================

CREATE TABLE cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    store_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_cart_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_cart_store FOREIGN KEY (store_id) REFERENCES store(id)
);

CREATE TABLE cart_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    bread_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_bread FOREIGN KEY (bread_id) REFERENCES bread(id),
    CONSTRAINT uk_cart_item_cart_id_bread_id UNIQUE (cart_id, bread_id),
    CONSTRAINT chk_cart_item_quantity CHECK (quantity > 0)
);

-- ============================================================
-- order / order_item
-- ============================================================

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount INT NOT NULL,
    idempotency_key VARCHAR(255) NULL,
    order_number VARCHAR(4) NULL,
    order_date DATE NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_orders_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES store(id),
    CONSTRAINT chk_orders_total_amount CHECK (total_amount > 0),
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCEL_PENDING', 'CANCELLED', 'PICKED_UP')),
    UNIQUE INDEX uk_orders_user_id_idempotency_key (user_id, idempotency_key),
    UNIQUE INDEX uk_orders_store_order_date_number (store_id, order_date, order_number),
    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_store_id (store_id),
    INDEX idx_orders_user_id_created_at (user_id, created_at DESC),
    INDEX idx_orders_status_created_at (status, created_at, id)
);

CREATE TABLE order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    bread_id BIGINT NULL,
    bread_name VARCHAR(100) NOT NULL,
    bread_price INT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_order_item_orders FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_bread FOREIGN KEY (bread_id) REFERENCES bread(id) ON DELETE SET NULL,
    CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
    INDEX idx_order_item_order_id (order_id)
);

-- ============================================================
-- payment
-- ============================================================

CREATE TABLE payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    amount INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    paid_at DATETIME(6) NULL,
    idempotency_key VARCHAR(255) NULL,
    payment_key VARCHAR(200) NULL,
    method VARCHAR(50) NULL,
    cancel_reason VARCHAR(200) NULL,
    cancelled_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_payment_orders FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'APPROVED', 'FAILED', 'CANCELLED')),
    INDEX idx_payment_order_id_idempotency_key (order_id, idempotency_key),
    INDEX idx_payment_payment_key (payment_key),
    INDEX idx_payment_idempotency_key (idempotency_key)
);

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
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_review_content_length CHECK (CHAR_LENGTH(content) >= 10 AND CHAR_LENGTH(content) <= 500),
    INDEX idx_review_store_id (store_id),
    INDEX idx_review_user_id (user_id),
    INDEX idx_review_bread_id (bread_id)
);

CREATE TABLE review_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_review_image_review FOREIGN KEY (review_id) REFERENCES review(id) ON DELETE CASCADE,
    INDEX idx_review_image_review_id (review_id)
);
