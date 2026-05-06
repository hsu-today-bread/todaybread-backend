/*
 빵(Bread) 테이블에 데이터 무결성 CHECK 제약을 추가합니다.
 - original_price >= 0
 - sale_price >= 0
 - remaining_quantity >= 0
 - sale_price <= original_price
 */

-- ============================================================
-- bread 테이블에 CHECK 제약 추가
-- ============================================================

ALTER TABLE bread
    ADD CONSTRAINT chk_bread_original_price CHECK (original_price >= 0),
    ADD CONSTRAINT chk_bread_sale_price CHECK (sale_price >= 0),
    ADD CONSTRAINT chk_bread_remaining_quantity CHECK (remaining_quantity >= 0),
    ADD CONSTRAINT chk_bread_sale_price_lte_original CHECK (sale_price <= original_price);
