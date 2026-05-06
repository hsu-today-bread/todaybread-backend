/*
 키워드 테이블에 길이 제약을 추가하고 컬럼 길이를 조정합니다.
 - normalised_text: VARCHAR(10)으로 변경 + CHECK 제약
 - display_text: VARCHAR(10)으로 변경 + CHECK 제약
 */

-- ============================================================
-- keyword 테이블 컬럼 길이 조정 및 CHECK 제약 추가
-- ============================================================

ALTER TABLE keyword
    MODIFY COLUMN normalised_text VARCHAR(10) NOT NULL,
    ADD CONSTRAINT chk_keyword_normalised_text_length CHECK (CHAR_LENGTH(normalised_text) <= 10);

-- ============================================================
-- user_keyword 테이블 컬럼 길이 조정 및 CHECK 제약 추가
-- ============================================================

ALTER TABLE user_keyword
    MODIFY COLUMN display_text VARCHAR(10) NOT NULL,
    ADD CONSTRAINT chk_user_keyword_display_text_length CHECK (CHAR_LENGTH(display_text) <= 10);
