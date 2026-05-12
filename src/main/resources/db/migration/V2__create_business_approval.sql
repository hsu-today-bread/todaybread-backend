CREATE TABLE business_approval (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    business_number_hash VARCHAR(64) NOT NULL,
    business_number_last4 VARCHAR(4) NOT NULL,
    business_start_date CHAR(8) NOT NULL,
    business_status_code VARCHAR(2) NOT NULL,
    business_status_name VARCHAR(30) NOT NULL,
    verified_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_business_approval_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_business_approval_user UNIQUE (user_id),
    CONSTRAINT uk_business_approval_number_hash UNIQUE (business_number_hash),
    INDEX idx_business_approval_user_id (user_id)
);
