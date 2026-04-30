-- V2: orders 테이블에 주문 번호(order_number) 및 주문 날짜(order_date) 컬럼 추가
-- 사장님-유저 간 픽업 확인 수단으로 사용되는 영숫자 4자리 주문 번호

ALTER TABLE orders ADD COLUMN order_number VARCHAR(4) NULL AFTER idempotency_key;
ALTER TABLE orders ADD COLUMN order_date DATE NULL AFTER order_number;

-- 기존 주문의 order_date를 created_at에서 backfill
UPDATE orders SET order_date = DATE(created_at) WHERE order_date IS NULL;

-- 가게+날짜+주문번호 유니크 제약 (동시 주문 시 중복 방지)
ALTER TABLE orders ADD CONSTRAINT uk_orders_store_order_date_number
    UNIQUE (store_id, order_date, order_number);
