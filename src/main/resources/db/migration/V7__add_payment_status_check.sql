ALTER TABLE payment
    ADD CONSTRAINT chk_payment_status
        CHECK (status IN ('PENDING', 'APPROVED', 'FAILED', 'CANCELLED'));
