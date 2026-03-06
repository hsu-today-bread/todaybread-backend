/*
 users 테이블을 만들기 위한 SQL문 입니다.
 */
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(30) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(30) NOT NULL UNIQUE,
    phone_number VARCHAR(30) UNIQUE,
    is_boss BOOLEAN NOT NULL DEFAULT FALSE
);