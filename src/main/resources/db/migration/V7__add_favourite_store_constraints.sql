/*
 favourite_store 테이블에 유니크 제약조건과 인덱스를 추가합니다.
 - 동일 사용자가 같은 가게를 중복 등록하지 못하도록 유니크 제약조건을 추가합니다.
 - 사용자별 단골 가게 조회 성능을 위해 인덱스를 추가합니다.
 */
ALTER TABLE favourite_store
    ADD CONSTRAINT uk_favourite_store_user_id_store_id UNIQUE (user_id, store_id);

CREATE INDEX idx_favourite_store_user_id ON favourite_store (user_id);
