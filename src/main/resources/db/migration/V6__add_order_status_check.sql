-- V6: Add CHECK constraint on orders.status to only allow valid enum values
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCEL_PENDING', 'CANCELLED', 'PICKED_UP'));
