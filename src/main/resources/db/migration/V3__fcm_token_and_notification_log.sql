-- ============================================================
-- fcm_token
-- ============================================================

CREATE TABLE fcm_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    platform VARCHAR(10) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_fcm_token_user_id UNIQUE (user_id),
    CONSTRAINT uk_fcm_token_token UNIQUE (token),
    CONSTRAINT fk_fcm_token_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_fcm_token_platform CHECK (platform IN ('ANDROID', 'IOS')),
    INDEX idx_fcm_token_active (active)
);

-- ============================================================
-- notification_log
-- ============================================================

CREATE TABLE notification_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    target_id VARCHAR(100) NOT NULL,
    event_key VARCHAR(200) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_notification_log_dedup UNIQUE (user_id, type, event_key),
    CONSTRAINT fk_notification_log_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_notification_log_type CHECK (type IN ('KEYWORD_STOCK', 'FAVORITE_STORE_STOCK', 'ORDER_CREATED')),
    INDEX idx_notification_log_user_id (user_id),
    INDEX idx_notification_log_type (type)
);
