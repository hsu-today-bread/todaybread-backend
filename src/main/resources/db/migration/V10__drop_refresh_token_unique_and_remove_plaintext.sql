-- Refresh Token을 해시 형태로 저장하도록 변경
-- token 컬럼의 UNIQUE 제약조건 제거 (해시값은 userId로 조회)
ALTER TABLE refresh_token DROP INDEX token;
