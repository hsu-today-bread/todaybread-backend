CREATE TABLE interest_area (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    name        VARCHAR(50) NOT NULL,
    address     VARCHAR(200) NOT NULL,
    latitude    DECIMAL(10, 7) NOT NULL,
    longitude   DECIMAL(10, 7) NOT NULL,
    radius_km   DOUBLE NOT NULL DEFAULT 3.0,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,

    CONSTRAINT uk_interest_area_user_id UNIQUE (user_id),
    CONSTRAINT fk_interest_area_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_interest_area_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_interest_area_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_interest_area_name_length CHECK (CHAR_LENGTH(name) >= 1),
    CONSTRAINT chk_interest_area_address_length CHECK (CHAR_LENGTH(address) >= 1)
);
