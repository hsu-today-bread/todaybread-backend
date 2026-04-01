-- users.phone_number에 NOT NULL 제약조건 추가
-- 엔티티(nullable = false)와 DDL 정합성 맞춤
ALTER TABLE users MODIFY COLUMN phone_number VARCHAR(30) NOT NULL UNIQUE;
