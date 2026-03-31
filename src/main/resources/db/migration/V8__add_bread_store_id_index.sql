/*
 bread 테이블의 store_id 컬럼에 명시적 인덱스를 추가합니다.
 FK(fk_bread_store)로 인해 InnoDB가 암묵적 인덱스를 생성하지만,
 컨벤션에 따라 의미가 드러나는 명시적 인덱스를 추가합니다.
 */
CREATE INDEX idx_bread_store_id ON bread (store_id);
