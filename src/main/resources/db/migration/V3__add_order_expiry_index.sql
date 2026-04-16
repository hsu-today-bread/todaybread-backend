-- 만료 주문 스케줄러 조회 전용 인덱스
CREATE INDEX idx_orders_status_created_at ON orders (status, created_at);
