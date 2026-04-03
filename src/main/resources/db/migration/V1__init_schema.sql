/*
 신규 환경에서 V1~V12를 순차 적용하지 않고,
 현재 최종 스키마를 한 번에 구성하기 위한 baseline migration입니다.

 기존 환경에서는 이미 적용된 V1~V12 이력을 그대로 유지합니다.
 */

/*
 users / auth
 */
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

/*
 keyword
 */
CREATE TABLE keyword (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    normalised_text VARCHAR(255) NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE user_keyword (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    keyword_id BIGINT NOT NULL,
    display_text VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_user_keyword_user_id_keyword_id UNIQUE (user_id, keyword_id),
    CONSTRAINT fk_user_keyword_keyword FOREIGN KEY (keyword_id) REFERENCES keyword(id),
    CONSTRAINT fk_user_keyword_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

/*
 store
 */
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
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_store_users FOREIGN KEY (user_id) REFERENCES users(id),
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
    CONSTRAINT fk_favourite_store_users FOREIGN KEY (user_id) REFERENCES users(id),
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
    CONSTRAINT uk_store_business_hours_store_id_day_of_week UNIQUE (store_id, day_of_week)
);

/*
 bread
 */
CREATE TABLE bread (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    original_price INT NOT NULL,
    sale_price INT NOT NULL,
    remaining_quantity INT NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_bread_store FOREIGN KEY (store_id) REFERENCES store(id) ON DELETE CASCADE,
    INDEX idx_bread_store_id (store_id)
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
