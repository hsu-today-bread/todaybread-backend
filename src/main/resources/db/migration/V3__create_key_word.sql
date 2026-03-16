/*
 keyword와 user_keyword 테이블을 정의합니다.
 keyword: 정규화된 키워드 마스터 테이블
 user_keyword: 사용자-키워드 M:N 관계 테이블 (사용자가 입력한 원본 텍스트 포함)
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
    display_text TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_user_keyword_user_id_keyword_id UNIQUE (user_id, keyword_id),
    CONSTRAINT fk_user_keyword_keyword FOREIGN KEY (keyword_id) REFERENCES keyword(id),
    CONSTRAINT fk_user_keyword_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
