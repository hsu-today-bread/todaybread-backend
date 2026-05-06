ALTER TABLE store
    ADD CONSTRAINT chk_store_rating_sum CHECK (rating_sum >= 0),
    ADD CONSTRAINT chk_store_review_count CHECK (review_count >= 0);

ALTER TABLE store_business_hours
    ADD CONSTRAINT chk_store_business_hours_day_of_week CHECK (day_of_week BETWEEN 1 AND 7);
