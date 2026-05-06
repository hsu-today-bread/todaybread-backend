/*
 favourite_store 테이블의 user_id FK에 ON DELETE CASCADE를 추가합니다.
 유저 삭제 시 해당 유저의 단골 가게 설정도 자동 삭제됩니다.
 */

ALTER TABLE favourite_store
    DROP FOREIGN KEY fk_favourite_store_users,
    ADD CONSTRAINT fk_favourite_store_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
