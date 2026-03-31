/*
 TEXT 타입 컬럼을 VARCHAR(255)로 변경합니다.
 - store.description: 가게 설명 (255자 제한)
 - store.order_time: 영업 시간 텍스트 (255자 제한)
 - user_keyword.display_text: 사용자 입력 키워드 원본 (255자 제한)
 */
ALTER TABLE store MODIFY COLUMN description VARCHAR(255) NOT NULL;
ALTER TABLE store MODIFY COLUMN order_time VARCHAR(255) NOT NULL;
ALTER TABLE user_keyword MODIFY COLUMN display_text VARCHAR(255) NOT NULL;
