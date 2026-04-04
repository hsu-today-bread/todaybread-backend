/*
 cart / order / payment
 장바구니, 주문, 결제 도메인 테이블을 생성합니다.
 idempotency key, CHECK 제약을 포함합니다.
 */

-- cart: 유저당 하나의 장바구니 (user_id UNIQUE)
CREATE TABLE cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    store_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_cart_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_cart_store FOREIGN KEY (store_id) REFERENCES store(id)
);

-- cart_item: 장바구니 항목 (cart_id + bread_id 복합 유니크)
CREATE TABLE cart_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    bread_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_bread FOREIGN KEY (bread_id) REFERENCES bread(id),
    CONSTRAINT uk_cart_item_cart_id_bread_id UNIQUE (cart_id, bread_id),
    CONSTRAINT chk_cart_item_quantity CHECK (quantity > 0)
);

-- orders: 주문 (테이블명 'orders' — SQL 예약어 'order' 회피)
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount INT NOT NULL,
    idempotency_key VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_orders_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES store(id),
    CONSTRAINT chk_orders_total_amount CHECK (total_amount > 0),
    UNIQUE INDEX uk_orders_user_id_idempotency_key (user_id, idempotency_key),
    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_store_id (store_id),
    INDEX idx_orders_user_id_created_at (user_id, created_at DESC)
);

-- order_item: 주문 항목 (빵 정보 스냅샷 저장)
CREATE TABLE order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    bread_id BIGINT NULL,
    bread_name VARCHAR(100) NOT NULL,
    bread_price INT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_order_item_orders FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_bread FOREIGN KEY (bread_id) REFERENCES bread(id) ON DELETE SET NULL,
    CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
    INDEX idx_order_item_order_id (order_id)
);

-- payment: 결제 (order_id UNIQUE — 주문당 하나의 결제)
CREATE TABLE payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    amount INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    paid_at DATETIME(6) NULL,
    idempotency_key VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_payment_orders FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    INDEX idx_payment_order_id_idempotency_key (order_id, idempotency_key)
);
