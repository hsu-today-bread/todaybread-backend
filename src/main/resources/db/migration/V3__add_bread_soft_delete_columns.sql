/*
 빵(Bread) Soft Delete를 위한 컬럼 추가 마이그레이션입니다.
 is_deleted, deleted_at 컬럼을 추가하고, is_deleted에 대한 인덱스를 생성합니다.
 */

-- ============================================================
-- bread 테이블에 soft delete 컬럼 추가
-- ============================================================

ALTER TABLE bread
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at DATETIME(6) NULL;

CREATE INDEX idx_bread_is_deleted ON bread (is_deleted);
