/*
 Refresh 토큰을 저장하기 위한 sql문입니다.
 1명의 유저는 하나의 refresh 토큰을 가집니다.
 */
CREATE TABLE refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_refresh_token_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
