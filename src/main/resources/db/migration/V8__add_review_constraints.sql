/*
 리뷰 테이블에 CHECK 제약 조건을 추가하고, review_image에 ON DELETE CASCADE를 적용합니다.
 */

ALTER TABLE review
    ADD CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    ADD CONSTRAINT chk_review_content_length CHECK (CHAR_LENGTH(content) >= 10 AND CHAR_LENGTH(content) <= 500);

ALTER TABLE review_image
    DROP FOREIGN KEY fk_review_image_review,
    ADD CONSTRAINT fk_review_image_review FOREIGN KEY (review_id) REFERENCES review(id) ON DELETE CASCADE;
