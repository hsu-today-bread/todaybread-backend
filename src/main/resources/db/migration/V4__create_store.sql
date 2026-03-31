/*
 가게 정보 등록을 위한 테이블을 정의합니다.
 추후 빠른 검색 성능을 위해 인덱스를 추가합니다.
 */
CREATE TABLE store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(30) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    address_line1 VARCHAR(200) NOT NULL,
    address_line2 VARCHAR(200) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    end_time TIME NOT NULL,
    last_order_time TIME NOT NULL,
    order_time TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_store_users FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_store_lat_lng (latitude, longitude),
    INDEX idx_store_is_active (is_active)
);

/*
 유저의 단골 매장을 위한 테이블을 정의합니다.
 */
CREATE TABLE favourite_store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_favourite_store_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_favourite_store_store FOREIGN KEY (store_id) REFERENCES store(id)
);
