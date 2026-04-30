SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET collation_connection = 'utf8mb4_unicode_ci';

/*
 개발용 테스트 데이터 스크립트

 재실행 가능하도록, 아래 샘플 계정/매장에 해당하는 데이터만 먼저 정리한 뒤 다시 삽입합니다.

 샘플 로그인 계정
 - 일반 유저: demo-user@todaybread.local / todaybread123
 - 사장님 1: demo-boss-gangnam@todaybread.local / todaybread123
 - 사장님 2: demo-boss-seolleung@todaybread.local / todaybread123
 - 사장님 3: demo-boss-yeoksam@todaybread.local / todaybread123
 - 사장님 4: demo-boss-samsung@todaybread.local / todaybread123
 - 사장님 5: demo-boss-daechi@todaybread.local / todaybread123

 근처 매장/빵 조회 추천 좌표
 - lat=37.4980950
 - lng=127.0276100
 - radius=3
 */

SET @test_data_password_hash = '$argon2id$v=19$m=16384,t=2,p=1$q74E5AXcCNxyfKx3iCCtEQ$12t7qic0oWGPI8R9E3T8uCO+q0sP+fsK1paO8Hc3hXY';

SET @demo_user_email = 'demo-user@todaybread.local';
SET @demo_boss_gangnam_email = 'demo-boss-gangnam@todaybread.local';
SET @demo_boss_seolleung_email = 'demo-boss-seolleung@todaybread.local';
SET @demo_boss_yeoksam_email = 'demo-boss-yeoksam@todaybread.local';
SET @demo_boss_samsung_email = 'demo-boss-samsung@todaybread.local';
SET @demo_boss_daechi_email = 'demo-boss-daechi@todaybread.local';

START TRANSACTION;

/* ============================================================
   기존 데이터 정리 (재실행 가능)
   ============================================================ */

DELETE fs
FROM favourite_store fs
LEFT JOIN users u ON fs.user_id = u.id
LEFT JOIN store s ON fs.store_id = s.id
LEFT JOIN users su ON s.user_id = su.id
WHERE u.email = @demo_user_email
   OR su.email IN (
       @demo_boss_gangnam_email,
       @demo_boss_seolleung_email,
       @demo_boss_yeoksam_email,
       @demo_boss_samsung_email,
       @demo_boss_daechi_email
   );

DELETE uk
FROM user_keyword uk
JOIN users u ON uk.user_id = u.id
WHERE u.email = @demo_user_email;

/* 주문 관련 정리 (order_item → payment → orders 순서) */
DELETE oi FROM order_item oi
JOIN orders o ON oi.order_id = o.id
JOIN store s ON o.store_id = s.id
JOIN users u ON s.user_id = u.id
WHERE u.email IN (
    @demo_boss_gangnam_email,
    @demo_boss_seolleung_email,
    @demo_boss_yeoksam_email,
    @demo_boss_samsung_email,
    @demo_boss_daechi_email
);

DELETE p FROM payment p
JOIN orders o ON p.order_id = o.id
JOIN store s ON o.store_id = s.id
JOIN users u ON s.user_id = u.id
WHERE u.email IN (
    @demo_boss_gangnam_email,
    @demo_boss_seolleung_email,
    @demo_boss_yeoksam_email,
    @demo_boss_samsung_email,
    @demo_boss_daechi_email
);

DELETE o FROM orders o
JOIN store s ON o.store_id = s.id
JOIN users u ON s.user_id = u.id
WHERE u.email IN (
    @demo_boss_gangnam_email,
    @demo_boss_seolleung_email,
    @demo_boss_yeoksam_email,
    @demo_boss_samsung_email,
    @demo_boss_daechi_email
);

DELETE bi
FROM bread_image bi
JOIN bread b ON bi.bread_id = b.id
JOIN store s ON b.store_id = s.id
JOIN users u ON s.user_id = u.id
WHERE u.email IN (
    @demo_boss_gangnam_email,
    @demo_boss_seolleung_email,
    @demo_boss_yeoksam_email,
    @demo_boss_samsung_email,
    @demo_boss_daechi_email
);

DELETE b
FROM bread b
JOIN store s ON b.store_id = s.id
JOIN users u ON s.user_id = u.id
WHERE u.email IN (
    @demo_boss_gangnam_email,
    @demo_boss_seolleung_email,
    @demo_boss_yeoksam_email,
    @demo_boss_samsung_email,
    @demo_boss_daechi_email
);

DELETE si
FROM store_image si
JOIN store s ON si.store_id = s.id
JOIN users u ON s.user_id = u.id
WHERE u.email IN (
    @demo_boss_gangnam_email,
    @demo_boss_seolleung_email,
    @demo_boss_yeoksam_email,
    @demo_boss_samsung_email,
    @demo_boss_daechi_email
);

DELETE sbh
FROM store_business_hours sbh
JOIN store s ON sbh.store_id = s.id
JOIN users u ON s.user_id = u.id
WHERE u.email IN (
    @demo_boss_gangnam_email,
    @demo_boss_seolleung_email,
    @demo_boss_yeoksam_email,
    @demo_boss_samsung_email,
    @demo_boss_daechi_email
);

DELETE s
FROM store s
JOIN users u ON s.user_id = u.id
WHERE u.email IN (
    @demo_boss_gangnam_email,
    @demo_boss_seolleung_email,
    @demo_boss_yeoksam_email,
    @demo_boss_samsung_email,
    @demo_boss_daechi_email
);

DELETE rt
FROM refresh_token rt
JOIN users u ON rt.user_id = u.id
WHERE u.email IN (
    @demo_user_email,
    @demo_boss_gangnam_email,
    @demo_boss_seolleung_email,
    @demo_boss_yeoksam_email,
    @demo_boss_samsung_email,
    @demo_boss_daechi_email
);

DELETE FROM keyword
WHERE normalised_text IN (
    'tb-seed-croissant',
    'tb-seed-salt-bread',
    'tb-seed-late-night',
    'tb-test-data-croissant',
    'tb-test-data-salt-bread',
    'tb-test-data-late-night'
);

DELETE FROM users
WHERE email IN (
    @demo_user_email,
    @demo_boss_gangnam_email,
    @demo_boss_seolleung_email,
    @demo_boss_yeoksam_email,
    @demo_boss_samsung_email,
    @demo_boss_daechi_email
);

/* ============================================================
   유저 삽입
   ============================================================ */

INSERT INTO users (
    email,
    name,
    password_hash,
    nickname,
    phone_number,
    is_boss
) VALUES
    (
        @demo_user_email,
        '데모 유저',
        @test_data_password_hash,
        'demo-user',
        '010-7000-1001',
        FALSE
    ),
    (
        @demo_boss_gangnam_email,
        '강남 사장님',
        @test_data_password_hash,
        'demo-boss-gangnam',
        '010-7000-2001',
        TRUE
    ),
    (
        @demo_boss_seolleung_email,
        '선릉 사장님',
        @test_data_password_hash,
        'demo-boss-seolleung',
        '010-7000-2002',
        TRUE
    ),
    (
        @demo_boss_yeoksam_email,
        '역삼 사장님',
        @test_data_password_hash,
        'demo-boss-yeoksam',
        '010-7000-2003',
        TRUE
    ),
    (
        @demo_boss_samsung_email,
        '삼성 사장님',
        @test_data_password_hash,
        'demo-boss-samsung',
        '010-7000-2004',
        TRUE
    ),
    (
        @demo_boss_daechi_email,
        '대치 사장님',
        @test_data_password_hash,
        'demo-boss-daechi',
        '010-7000-2005',
        TRUE
    );

SET @demo_user_id = (
    SELECT id FROM users WHERE email = @demo_user_email
);
SET @demo_boss_gangnam_id = (
    SELECT id FROM users WHERE email = @demo_boss_gangnam_email
);
SET @demo_boss_seolleung_id = (
    SELECT id FROM users WHERE email = @demo_boss_seolleung_email
);
SET @demo_boss_yeoksam_id = (
    SELECT id FROM users WHERE email = @demo_boss_yeoksam_email
);
SET @demo_boss_samsung_id = (
    SELECT id FROM users WHERE email = @demo_boss_samsung_email
);
SET @demo_boss_daechi_id = (
    SELECT id FROM users WHERE email = @demo_boss_daechi_email
);

/* ============================================================
   키워드
   ============================================================ */

INSERT INTO keyword (normalised_text) VALUES
    ('tb-test-data-croissant'),
    ('tb-test-data-salt-bread'),
    ('tb-test-data-late-night');

INSERT INTO user_keyword (user_id, keyword_id, display_text)
SELECT
    @demo_user_id,
    k.id,
    CASE k.normalised_text
        WHEN 'tb-test-data-croissant' THEN '크루아상'
        WHEN 'tb-test-data-salt-bread' THEN '소금빵'
        WHEN 'tb-test-data-late-night' THEN '늦게까지 영업'
    END
FROM keyword k
WHERE k.normalised_text IN (
    'tb-test-data-croissant',
    'tb-test-data-salt-bread',
    'tb-test-data-late-night'
);


/* ============================================================
   매장 삽입 (5개)
   ============================================================ */

INSERT INTO store (
    user_id,
    name,
    phone_number,
    description,
    address_line1,
    address_line2,
    latitude,
    longitude,
    is_active
) VALUES
    (
        @demo_boss_gangnam_id,
        '투데이브레드 데모 강남점',
        '02-7000-3001',
        '강남역 근처에서 소금빵과 식사용 빵을 판매하는 프론트 연동용 데모 매장입니다.',
        '서울특별시 강남구 테헤란로 123',
        '1층',
        37.4980950,
        127.0276100,
        TRUE
    ),
    (
        @demo_boss_seolleung_id,
        '투데이브레드 데모 선릉점',
        '02-7000-3002',
        '선릉역 근처에서 크루아상과 사워도우를 판매하는 프론트 연동용 데모 매장입니다.',
        '서울특별시 강남구 선릉로 431',
        '2층',
        37.5045000,
        127.0489000,
        TRUE
    ),
    (
        @demo_boss_yeoksam_id,
        '투데이브레드 데모 역삼점',
        '02-7000-3003',
        '역삼역 인근에서 베이글과 머핀을 판매하는 프론트 연동용 데모 매장입니다.',
        '서울특별시 강남구 역삼로 210',
        '1층',
        37.5000000,
        127.0365000,
        TRUE
    ),
    (
        @demo_boss_samsung_id,
        '투데이브레드 데모 삼성점',
        '02-7000-3004',
        '삼성역 근처에서 식빵과 간식빵을 판매하는 프론트 연동용 데모 매장입니다.',
        '서울특별시 강남구 삼성로 512',
        '지하 1층',
        37.5088000,
        127.0630000,
        TRUE
    ),
    (
        @demo_boss_daechi_id,
        '투데이브레드 데모 대치점',
        '02-7000-3005',
        '대치동에서 건강빵과 디저트를 판매하는 프론트 연동용 데모 매장입니다.',
        '서울특별시 강남구 대치로 85',
        '2층',
        37.4945000,
        127.0580000,
        TRUE
    );

SET @demo_store_gangnam_id = (
    SELECT id FROM store WHERE user_id = @demo_boss_gangnam_id
);
SET @demo_store_seolleung_id = (
    SELECT id FROM store WHERE user_id = @demo_boss_seolleung_id
);
SET @demo_store_yeoksam_id = (
    SELECT id FROM store WHERE user_id = @demo_boss_yeoksam_id
);
SET @demo_store_samsung_id = (
    SELECT id FROM store WHERE user_id = @demo_boss_samsung_id
);
SET @demo_store_daechi_id = (
    SELECT id FROM store WHERE user_id = @demo_boss_daechi_id
);

/* ============================================================
   영업시간 (월~토 영업, 일요일 휴무 — 가게마다 시간 다름)
   day_of_week: 1=월 ~ 7=일
   ============================================================ */

/* 강남점: 07:00~23:00 */
INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time) VALUES
    (@demo_store_gangnam_id, 1, FALSE, '07:00:00', '23:00:00', '22:30:00'),
    (@demo_store_gangnam_id, 2, FALSE, '07:00:00', '23:00:00', '22:30:00'),
    (@demo_store_gangnam_id, 3, FALSE, '07:00:00', '23:00:00', '22:30:00'),
    (@demo_store_gangnam_id, 4, FALSE, '07:00:00', '23:00:00', '22:30:00'),
    (@demo_store_gangnam_id, 5, FALSE, '07:00:00', '23:00:00', '22:30:00'),
    (@demo_store_gangnam_id, 6, FALSE, '07:00:00', '23:00:00', '22:30:00'),
    (@demo_store_gangnam_id, 7, TRUE,  NULL,       NULL,       NULL);

/* 선릉점: 09:00~21:00 */
INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time) VALUES
    (@demo_store_seolleung_id, 1, FALSE, '09:00:00', '21:00:00', '20:30:00'),
    (@demo_store_seolleung_id, 2, FALSE, '09:00:00', '21:00:00', '20:30:00'),
    (@demo_store_seolleung_id, 3, FALSE, '09:00:00', '21:00:00', '20:30:00'),
    (@demo_store_seolleung_id, 4, FALSE, '09:00:00', '21:00:00', '20:30:00'),
    (@demo_store_seolleung_id, 5, FALSE, '09:00:00', '21:00:00', '20:30:00'),
    (@demo_store_seolleung_id, 6, FALSE, '09:00:00', '21:00:00', '20:30:00'),
    (@demo_store_seolleung_id, 7, TRUE,  NULL,       NULL,       NULL);

/* 역삼점: 08:00~22:00 */
INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time) VALUES
    (@demo_store_yeoksam_id, 1, FALSE, '08:00:00', '22:00:00', '21:30:00'),
    (@demo_store_yeoksam_id, 2, FALSE, '08:00:00', '22:00:00', '21:30:00'),
    (@demo_store_yeoksam_id, 3, FALSE, '08:00:00', '22:00:00', '21:30:00'),
    (@demo_store_yeoksam_id, 4, FALSE, '08:00:00', '22:00:00', '21:30:00'),
    (@demo_store_yeoksam_id, 5, FALSE, '08:00:00', '22:00:00', '21:30:00'),
    (@demo_store_yeoksam_id, 6, FALSE, '08:00:00', '22:00:00', '21:30:00'),
    (@demo_store_yeoksam_id, 7, TRUE,  NULL,       NULL,       NULL);

/* 삼성점: 06:30~20:00 */
INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time) VALUES
    (@demo_store_samsung_id, 1, FALSE, '06:30:00', '20:00:00', '19:30:00'),
    (@demo_store_samsung_id, 2, FALSE, '06:30:00', '20:00:00', '19:30:00'),
    (@demo_store_samsung_id, 3, FALSE, '06:30:00', '20:00:00', '19:30:00'),
    (@demo_store_samsung_id, 4, FALSE, '06:30:00', '20:00:00', '19:30:00'),
    (@demo_store_samsung_id, 5, FALSE, '06:30:00', '20:00:00', '19:30:00'),
    (@demo_store_samsung_id, 6, FALSE, '06:30:00', '20:00:00', '19:30:00'),
    (@demo_store_samsung_id, 7, TRUE,  NULL,       NULL,       NULL);

/* 대치점: 10:00~22:30 */
INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time) VALUES
    (@demo_store_daechi_id, 1, FALSE, '10:00:00', '22:30:00', '22:00:00'),
    (@demo_store_daechi_id, 2, FALSE, '10:00:00', '22:30:00', '22:00:00'),
    (@demo_store_daechi_id, 3, FALSE, '10:00:00', '22:30:00', '22:00:00'),
    (@demo_store_daechi_id, 4, FALSE, '10:00:00', '22:30:00', '22:00:00'),
    (@demo_store_daechi_id, 5, FALSE, '10:00:00', '22:30:00', '22:00:00'),
    (@demo_store_daechi_id, 6, FALSE, '10:00:00', '22:30:00', '22:00:00'),
    (@demo_store_daechi_id, 7, TRUE,  NULL,       NULL,       NULL);


/* ============================================================
   빵 삽입 (각 가게 5개씩, 총 25개)
   original_price: 3000~10000
   sale_price: original_price의 60~80%
   remaining_quantity: 0~20 (일부 품절)
   ============================================================ */

/* ── 강남점 빵 5개 (기존 3 + 신규 2) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@demo_store_gangnam_id, '시그니처 소금빵',  3500, 2500, 14, '겉은 바삭하고 속은 촉촉한 대표 메뉴입니다.'),
    (@demo_store_gangnam_id, '바질 치아바타',    5200, 3900,  8, '점심용으로 바로 먹기 좋은 바질 치아바타입니다.'),
    (@demo_store_gangnam_id, '밤식빵',          6800, 5000,  0, '오늘은 품절 상태를 확인할 수 있도록 재고를 0으로 넣어둔 메뉴입니다.'),
    (@demo_store_gangnam_id, '크림치즈 베이글',  4000, 2800, 10, '부드러운 크림치즈가 듬뿍 들어간 베이글입니다.'),
    (@demo_store_gangnam_id, '초코 크루아상',    4500, 3200,  6, '진한 초콜릿이 겹겹이 들어간 크루아상입니다.');

/* ── 선릉점 빵 5개 (기존 3 + 신규 2) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@demo_store_seolleung_id, '버터 크루아상',   4200, 3200, 12, '겹이 살아 있는 기본 크루아상입니다.'),
    (@demo_store_seolleung_id, '앙버터 바게트',   5800, 4300,  5, '달콤한 팥과 버터 조합을 확인하기 좋은 메뉴입니다.'),
    (@demo_store_seolleung_id, '사워도우 하프',   9000, 7000,  3, '프론트 리스트와 상세 화면에서 가격 비교가 잘 보이는 메뉴입니다.'),
    (@demo_store_seolleung_id, '얼그레이 스콘',   3800, 2600,  9, '얼그레이 향이 은은한 영국식 스콘입니다.'),
    (@demo_store_seolleung_id, '호밀 식빵',      5500, 3800,  0, '건강한 호밀 100% 식빵입니다. 품절 테스트용.');

/* ── 역삼점 빵 5개 ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@demo_store_yeoksam_id, '플레인 베이글',    3500, 2400, 15, '쫄깃한 식감의 기본 베이글입니다.'),
    (@demo_store_yeoksam_id, '블루베리 머핀',    4000, 2800,  7, '블루베리가 톡톡 터지는 촉촉한 머핀입니다.'),
    (@demo_store_yeoksam_id, '단팥빵',          3000, 2100, 20, '달콤한 팥소가 가득한 전통 단팥빵입니다.'),
    (@demo_store_yeoksam_id, '카레빵',          3800, 2700,  0, '바삭한 튀김옷 속 매콤한 카레가 들어간 빵입니다. 품절.'),
    (@demo_store_yeoksam_id, '마늘바게트',       5000, 3500, 11, '마늘 버터가 듬뿍 발린 바삭한 바게트입니다.');

/* ── 삼성점 빵 5개 ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@demo_store_samsung_id, '우유식빵',         4500, 3200, 18, '부드럽고 달콤한 우유식빵입니다.'),
    (@demo_store_samsung_id, '모카빵',           3500, 2500,  5, '커피 향이 진한 모카 크림빵입니다.'),
    (@demo_store_samsung_id, '피자빵',           4000, 2800, 13, '토마토소스와 치즈가 올라간 피자빵입니다.'),
    (@demo_store_samsung_id, '고구마 크루아상',   5500, 3800,  0, '달콤한 고구마 무스가 들어간 크루아상입니다. 품절.'),
    (@demo_store_samsung_id, '치즈 스틱',        3000, 2100,  9, '쭉 늘어나는 모짜렐라 치즈 스틱입니다.');

/* ── 대치점 빵 5개 ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@demo_store_daechi_id, '통밀빵',            5000, 3500, 10, '100% 통밀로 만든 건강한 식사빵입니다.'),
    (@demo_store_daechi_id, '호두파이',          6000, 4200,  4, '고소한 호두가 가득한 미니 파이입니다.'),
    (@demo_store_daechi_id, '팥앙금빵',          3500, 2500, 16, '직접 만든 팥앙금이 들어간 부드러운 빵입니다.'),
    (@demo_store_daechi_id, '크로플',            4500, 3200,  0, '크루아상 반죽으로 구운 바삭한 와플입니다. 품절.'),
    (@demo_store_daechi_id, '에그타르트',         3800, 2600,  8, '바삭한 페이스트리에 부드러운 커스터드가 들어간 타르트입니다.');

/* ============================================================
   즐겨찾기
   ============================================================ */

INSERT INTO favourite_store (user_id, store_id) VALUES
    (@demo_user_id, @demo_store_gangnam_id),
    (@demo_user_id, @demo_store_seolleung_id);


/* ============================================================
   주문 / 매출 테스트 데이터
   - 3월: 각 가게별 8~12건 (3/1~3/31)
   - 4월: 각 가게별 10~15건 (4/1~4/30)
   - 상태: 대부분 PICKED_UP, 일부 CONFIRMED, 일부 CANCELLED
   - 각 가게별 오늘 날짜 CONFIRMED 주문 1~2건 (픽업 대기 테스트용)
   ============================================================ */

/* ── 빵 ID 조회 ── */

/* 강남점 */
SET @bread_salt = (
    SELECT id FROM bread WHERE store_id = @demo_store_gangnam_id AND name = '시그니처 소금빵'
);
SET @bread_ciabatta = (
    SELECT id FROM bread WHERE store_id = @demo_store_gangnam_id AND name = '바질 치아바타'
);
SET @bread_chestnut = (
    SELECT id FROM bread WHERE store_id = @demo_store_gangnam_id AND name = '밤식빵'
);
SET @bread_cream_bagel = (
    SELECT id FROM bread WHERE store_id = @demo_store_gangnam_id AND name = '크림치즈 베이글'
);
SET @bread_choco_croissant = (
    SELECT id FROM bread WHERE store_id = @demo_store_gangnam_id AND name = '초코 크루아상'
);

/* 선릉점 */
SET @bread_croissant = (
    SELECT id FROM bread WHERE store_id = @demo_store_seolleung_id AND name = '버터 크루아상'
);
SET @bread_baguette = (
    SELECT id FROM bread WHERE store_id = @demo_store_seolleung_id AND name = '앙버터 바게트'
);
SET @bread_sourdough = (
    SELECT id FROM bread WHERE store_id = @demo_store_seolleung_id AND name = '사워도우 하프'
);
SET @bread_earl_grey = (
    SELECT id FROM bread WHERE store_id = @demo_store_seolleung_id AND name = '얼그레이 스콘'
);
SET @bread_rye = (
    SELECT id FROM bread WHERE store_id = @demo_store_seolleung_id AND name = '호밀 식빵'
);

/* 역삼점 */
SET @bread_plain_bagel = (
    SELECT id FROM bread WHERE store_id = @demo_store_yeoksam_id AND name = '플레인 베이글'
);
SET @bread_blueberry = (
    SELECT id FROM bread WHERE store_id = @demo_store_yeoksam_id AND name = '블루베리 머핀'
);
SET @bread_redbean = (
    SELECT id FROM bread WHERE store_id = @demo_store_yeoksam_id AND name = '단팥빵'
);
SET @bread_curry = (
    SELECT id FROM bread WHERE store_id = @demo_store_yeoksam_id AND name = '카레빵'
);
SET @bread_garlic = (
    SELECT id FROM bread WHERE store_id = @demo_store_yeoksam_id AND name = '마늘바게트'
);

/* 삼성점 */
SET @bread_milk = (
    SELECT id FROM bread WHERE store_id = @demo_store_samsung_id AND name = '우유식빵'
);
SET @bread_mocha = (
    SELECT id FROM bread WHERE store_id = @demo_store_samsung_id AND name = '모카빵'
);
SET @bread_pizza = (
    SELECT id FROM bread WHERE store_id = @demo_store_samsung_id AND name = '피자빵'
);
SET @bread_sweetpotato = (
    SELECT id FROM bread WHERE store_id = @demo_store_samsung_id AND name = '고구마 크루아상'
);
SET @bread_cheese_stick = (
    SELECT id FROM bread WHERE store_id = @demo_store_samsung_id AND name = '치즈 스틱'
);

/* 대치점 */
SET @bread_whole_wheat = (
    SELECT id FROM bread WHERE store_id = @demo_store_daechi_id AND name = '통밀빵'
);
SET @bread_walnut_pie = (
    SELECT id FROM bread WHERE store_id = @demo_store_daechi_id AND name = '호두파이'
);
SET @bread_red_angeum = (
    SELECT id FROM bread WHERE store_id = @demo_store_daechi_id AND name = '팥앙금빵'
);
SET @bread_croffle = (
    SELECT id FROM bread WHERE store_id = @demo_store_daechi_id AND name = '크로플'
);
SET @bread_egg_tart = (
    SELECT id FROM bread WHERE store_id = @demo_store_daechi_id AND name = '에그타르트'
);


/* ============================================================
   강남점 주문 — 3월 (10건)
   ============================================================ */

/* 3/2 - 소금빵 2개, 크림치즈 베이글 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 7800, 'A1B2', '2026-03-02', '2026-03-02 18:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 2),
    (@ord_id, @bread_cream_bagel, '크림치즈 베이글', 2800, 1);

/* 3/4 - 초코 크루아상 3개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 9600, 'C3D4', '2026-03-04', '2026-03-04 19:15:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_choco_croissant, '초코 크루아상', 3200, 3);

/* 3/7 - 바질 치아바타 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 7800, 'E5F6', '2026-03-07', '2026-03-07 17:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_ciabatta, '바질 치아바타', 3900, 2);

/* 3/10 - 소금빵 1개, 밤식빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 7500, 'G7H8', '2026-03-10', '2026-03-10 20:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 1),
    (@ord_id, @bread_chestnut, '밤식빵', 5000, 1);

/* 3/12 - 크림치즈 베이글 2개, 초코 크루아상 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 8800, 'J9K1', '2026-03-12', '2026-03-12 18:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_cream_bagel, '크림치즈 베이글', 2800, 2),
    (@ord_id, @bread_choco_croissant, '초코 크루아상', 3200, 1);

/* 3/15 - 소금빵 4개 (주말 대량) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 10000, 'L2M3', '2026-03-15', '2026-03-15 16:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 4);

/* 3/18 - 치아바타 1개, 크림치즈 베이글 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 6700, 'N4P5', '2026-03-18', '2026-03-18 19:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_ciabatta, '바질 치아바타', 3900, 1),
    (@ord_id, @bread_cream_bagel, '크림치즈 베이글', 2800, 1);

/* 3/22 - 초코 크루아상 2개, 소금빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 8900, 'Q6R7', '2026-03-22', '2026-03-22 17:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_choco_croissant, '초코 크루아상', 3200, 2),
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 1);

/* 3/25 - 취소 주문 (매출 제외 확인용) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'CANCELLED', 5000, 'S8T9', '2026-03-25', '2026-03-25 18:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 2);

/* 3/28 - 밤식빵 1개, 치아바타 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 8900, 'U1V2', '2026-03-28', '2026-03-28 20:15:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_chestnut, '밤식빵', 5000, 1),
    (@ord_id, @bread_ciabatta, '바질 치아바타', 3900, 1);

/* ============================================================
   강남점 주문 — 4월 (12건 + 픽업대기 2건 + 취소 1건)
   ============================================================ */

/* 4/1 - 소금빵 3개, 치아바타 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 11400, 'A2B3', '2026-04-01', '2026-04-01 18:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 3),
    (@ord_id, @bread_ciabatta, '바질 치아바타', 3900, 1);

/* 4/2 - 소금빵 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 5000, 'C4D5', '2026-04-02', '2026-04-02 19:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 2);

/* 4/3 - 치아바타 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 7800, 'E6F7', '2026-04-03', '2026-04-03 17:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_ciabatta, '바질 치아바타', 3900, 2);

/* 4/4 - 취소 주문 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'CANCELLED', 5000, 'HH89', '2026-04-04', '2026-04-04 18:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 2);

/* 4/5 - 소금빵 5개, 치아바타 2개 (주말 대량) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 20300, 'G8H9', '2026-04-05', '2026-04-05 16:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 5),
    (@ord_id, @bread_ciabatta, '바질 치아바타', 3900, 2);

/* 4/7 - 소금빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 2500, 'J2K3', '2026-04-07', '2026-04-07 20:15:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 1);

/* 4/8 - 소금빵 2개, 치아바타 3개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 16700, 'L4M5', '2026-04-08', '2026-04-08 18:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 2),
    (@ord_id, @bread_ciabatta, '바질 치아바타', 3900, 3);

/* 4/10 - 치아바타 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 3900, 'N6P7', '2026-04-10', '2026-04-10 19:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_ciabatta, '바질 치아바타', 3900, 1);

/* 4/12 - 소금빵 4개 (주말) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 10000, 'Q8R9', '2026-04-12', '2026-04-12 15:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 4);

/* 4/14 - 소금빵 1개, 치아바타 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 6400, 'S2T3', '2026-04-14', '2026-04-14 21:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 1),
    (@ord_id, @bread_ciabatta, '바질 치아바타', 3900, 1);

/* 4/15 - 소금빵 3개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 7500, 'U4V5', '2026-04-15', '2026-04-15 17:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 3);

/* 4/20 - 크림치즈 베이글 2개, 초코 크루아상 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'PICKED_UP', 8800, 'W3X4', '2026-04-20', '2026-04-20 16:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_cream_bagel, '크림치즈 베이글', 2800, 2),
    (@ord_id, @bread_choco_croissant, '초코 크루아상', 3200, 1);

/* 강남점 픽업 대기 주문 (CONFIRMED, 오늘 날짜) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'CONFIRMED', 7500, 'W6X7', CURDATE(), NOW());
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_salt, '시그니처 소금빵', 2500, 3);

INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_gangnam_id, 'CONFIRMED', 3900, 'Y8Z9', CURDATE(), NOW());
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_ciabatta, '바질 치아바타', 3900, 1);


/* ============================================================
   선릉점 주문 — 3월 (9건)
   ============================================================ */

/* 3/1 - 크루아상 2개, 얼그레이 스콘 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 9000, 'SA12', '2026-03-01', '2026-03-01 17:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_croissant, '버터 크루아상', 3200, 2),
    (@ord_id, @bread_earl_grey, '얼그레이 스콘', 2600, 1);

/* 3/5 - 사워도우 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 7000, 'SB34', '2026-03-05', '2026-03-05 18:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_sourdough, '사워도우 하프', 7000, 1);

/* 3/8 - 바게트 2개, 크루아상 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 11800, 'SC56', '2026-03-08', '2026-03-08 16:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_baguette, '앙버터 바게트', 4300, 2),
    (@ord_id, @bread_croissant, '버터 크루아상', 3200, 1);

/* 3/11 - 얼그레이 스콘 3개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 7800, 'SD78', '2026-03-11', '2026-03-11 19:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_earl_grey, '얼그레이 스콘', 2600, 3);

/* 3/14 - 크루아상 1개, 사워도우 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 10200, 'SE91', '2026-03-14', '2026-03-14 20:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_croissant, '버터 크루아상', 3200, 1),
    (@ord_id, @bread_sourdough, '사워도우 하프', 7000, 1);

/* 3/18 - 바게트 1개, 얼그레이 스콘 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 9500, 'SF23', '2026-03-18', '2026-03-18 17:15:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_baguette, '앙버터 바게트', 4300, 1),
    (@ord_id, @bread_earl_grey, '얼그레이 스콘', 2600, 2);

/* 3/22 - 취소 주문 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'CANCELLED', 6400, 'SG45', '2026-03-22', '2026-03-22 18:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_croissant, '버터 크루아상', 3200, 2);

/* 3/25 - 사워도우 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 14000, 'SH67', '2026-03-25', '2026-03-25 19:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_sourdough, '사워도우 하프', 7000, 2);

/* 3/30 - 크루아상 3개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 9600, 'SJ89', '2026-03-30', '2026-03-30 16:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_croissant, '버터 크루아상', 3200, 3);

/* ============================================================
   선릉점 주문 — 4월 (10건 + 픽업대기 1건)
   ============================================================ */

/* 4/1 - 크루아상 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 6400, 'AA23', '2026-04-01', '2026-04-01 17:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_croissant, '버터 크루아상', 3200, 2);

/* 4/3 - 바게트 1개, 사워도우 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 11300, 'BB45', '2026-04-03', '2026-04-03 18:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_baguette, '앙버터 바게트', 4300, 1),
    (@ord_id, @bread_sourdough, '사워도우 하프', 7000, 1);

/* 4/5 - 크루아상 3개, 바게트 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 18200, 'CC67', '2026-04-05', '2026-04-05 16:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_croissant, '버터 크루아상', 3200, 3),
    (@ord_id, @bread_baguette, '앙버터 바게트', 4300, 2);

/* 4/7 - 사워도우 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 14000, 'DD89', '2026-04-07', '2026-04-07 19:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_sourdough, '사워도우 하프', 7000, 2);

/* 4/8 - 크루아상 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 3200, 'EE23', '2026-04-08', '2026-04-08 20:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_croissant, '버터 크루아상', 3200, 1);

/* 4/10 - 바게트 3개, 크루아상 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 19300, 'FF45', '2026-04-10', '2026-04-10 17:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_baguette, '앙버터 바게트', 4300, 3),
    (@ord_id, @bread_croissant, '버터 크루아상', 3200, 2);

/* 4/13 - 얼그레이 스콘 2개, 크루아상 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 8400, 'FK12', '2026-04-13', '2026-04-13 18:15:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_earl_grey, '얼그레이 스콘', 2600, 2),
    (@ord_id, @bread_croissant, '버터 크루아상', 3200, 1);

/* 4/17 - 사워도우 1개, 바게트 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 11300, 'FL34', '2026-04-17', '2026-04-17 19:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_sourdough, '사워도우 하프', 7000, 1),
    (@ord_id, @bread_baguette, '앙버터 바게트', 4300, 1);

/* 4/22 - 크루아상 4개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 12800, 'FM56', '2026-04-22', '2026-04-22 16:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_croissant, '버터 크루아상', 3200, 4);

/* 4/28 - 얼그레이 스콘 1개, 사워도우 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'PICKED_UP', 9600, 'FN78', '2026-04-28', '2026-04-28 20:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_earl_grey, '얼그레이 스콘', 2600, 1),
    (@ord_id, @bread_sourdough, '사워도우 하프', 7000, 1);

/* 선릉점 픽업 대기 주문 (CONFIRMED, 오늘 날짜) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_seolleung_id, 'CONFIRMED', 10500, 'GG67', CURDATE(), NOW());
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_sourdough, '사워도우 하프', 7000, 1),
    (@ord_id, @bread_croissant, '버터 크루아상', 3200, 1);


/* ============================================================
   역삼점 주문 — 3월 (9건)
   ============================================================ */

/* 3/3 - 플레인 베이글 3개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 7200, 'YA12', '2026-03-03', '2026-03-03 17:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_plain_bagel, '플레인 베이글', 2400, 3);

/* 3/5 - 블루베리 머핀 2개, 단팥빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 7700, 'YB34', '2026-03-05', '2026-03-05 18:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_blueberry, '블루베리 머핀', 2800, 2),
    (@ord_id, @bread_redbean, '단팥빵', 2100, 1);

/* 3/9 - 마늘바게트 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 7000, 'YC56', '2026-03-09', '2026-03-09 19:15:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_garlic, '마늘바게트', 3500, 2);

/* 3/12 - 단팥빵 4개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 8400, 'YD78', '2026-03-12', '2026-03-12 16:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_redbean, '단팥빵', 2100, 4);

/* 3/15 - 카레빵 2개, 베이글 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 7800, 'YE91', '2026-03-15', '2026-03-15 17:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_curry, '카레빵', 2700, 2),
    (@ord_id, @bread_plain_bagel, '플레인 베이글', 2400, 1);

/* 3/19 - 취소 주문 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'CANCELLED', 5600, 'YF23', '2026-03-19', '2026-03-19 18:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_blueberry, '블루베리 머핀', 2800, 2);

/* 3/21 - 마늘바게트 1개, 블루베리 머핀 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 6300, 'YG45', '2026-03-21', '2026-03-21 20:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_garlic, '마늘바게트', 3500, 1),
    (@ord_id, @bread_blueberry, '블루베리 머핀', 2800, 1);

/* 3/26 - 베이글 2개, 단팥빵 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 9000, 'YH67', '2026-03-26', '2026-03-26 17:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_plain_bagel, '플레인 베이글', 2400, 2),
    (@ord_id, @bread_redbean, '단팥빵', 2100, 2);

/* 3/30 - 카레빵 1개, 마늘바게트 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 6200, 'YJ89', '2026-03-30', '2026-03-30 19:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_curry, '카레빵', 2700, 1),
    (@ord_id, @bread_garlic, '마늘바게트', 3500, 1);

/* ============================================================
   역삼점 주문 — 4월 (11건 + 픽업대기 2건)
   ============================================================ */

/* 4/1 - 베이글 2개, 머핀 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 7600, 'YK12', '2026-04-01', '2026-04-01 17:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_plain_bagel, '플레인 베이글', 2400, 2),
    (@ord_id, @bread_blueberry, '블루베리 머핀', 2800, 1);

/* 4/3 - 단팥빵 3개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 6300, 'YL34', '2026-04-03', '2026-04-03 18:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_redbean, '단팥빵', 2100, 3);

/* 4/5 - 마늘바게트 3개 (주말) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 10500, 'YM56', '2026-04-05', '2026-04-05 16:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_garlic, '마늘바게트', 3500, 3);

/* 4/7 - 카레빵 2개, 베이글 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 10200, 'YN78', '2026-04-07', '2026-04-07 19:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_curry, '카레빵', 2700, 2),
    (@ord_id, @bread_plain_bagel, '플레인 베이글', 2400, 2);

/* 4/9 - 블루베리 머핀 3개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 8400, 'YP91', '2026-04-09', '2026-04-09 20:15:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_blueberry, '블루베리 머핀', 2800, 3);

/* 4/12 - 단팥빵 2개, 마늘바게트 1개 (주말) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 7700, 'YQ23', '2026-04-12', '2026-04-12 15:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_redbean, '단팥빵', 2100, 2),
    (@ord_id, @bread_garlic, '마늘바게트', 3500, 1);

/* 4/15 - 베이글 4개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 9600, 'YR45', '2026-04-15', '2026-04-15 17:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_plain_bagel, '플레인 베이글', 2400, 4);

/* 4/18 - 취소 주문 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'CANCELLED', 5400, 'YS67', '2026-04-18', '2026-04-18 18:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_curry, '카레빵', 2700, 2);

/* 4/21 - 머핀 1개, 단팥빵 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 7000, 'YT89', '2026-04-21', '2026-04-21 19:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_blueberry, '블루베리 머핀', 2800, 1),
    (@ord_id, @bread_redbean, '단팥빵', 2100, 2);

/* 4/24 - 마늘바게트 2개, 카레빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 9700, 'YU12', '2026-04-24', '2026-04-24 16:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_garlic, '마늘바게트', 3500, 2),
    (@ord_id, @bread_curry, '카레빵', 2700, 1);

/* 4/28 - 베이글 1개, 머핀 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'PICKED_UP', 8000, 'YV34', '2026-04-28', '2026-04-28 20:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_plain_bagel, '플레인 베이글', 2400, 1),
    (@ord_id, @bread_blueberry, '블루베리 머핀', 2800, 2);

/* 역삼점 픽업 대기 주문 (CONFIRMED, 오늘 날짜) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'CONFIRMED', 4800, 'YW56', CURDATE(), NOW());
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_plain_bagel, '플레인 베이글', 2400, 2);

INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_yeoksam_id, 'CONFIRMED', 7000, 'YX78', CURDATE(), NOW());
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_garlic, '마늘바게트', 3500, 2);


/* ============================================================
   삼성점 주문 — 3월 (8건)
   ============================================================ */

/* 3/2 - 우유식빵 1개, 모카빵 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 8200, 'MA12', '2026-03-02', '2026-03-02 17:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_milk, '우유식빵', 3200, 1),
    (@ord_id, @bread_mocha, '모카빵', 2500, 2);

/* 3/6 - 피자빵 3개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 8400, 'MB34', '2026-03-06', '2026-03-06 18:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_pizza, '피자빵', 2800, 3);

/* 3/10 - 치즈 스틱 4개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 8400, 'MC56', '2026-03-10', '2026-03-10 19:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_cheese_stick, '치즈 스틱', 2100, 4);

/* 3/13 - 우유식빵 2개, 피자빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 9200, 'MD78', '2026-03-13', '2026-03-13 16:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_milk, '우유식빵', 3200, 2),
    (@ord_id, @bread_pizza, '피자빵', 2800, 1);

/* 3/17 - 모카빵 3개, 치즈 스틱 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 9600, 'ME91', '2026-03-17', '2026-03-17 20:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_mocha, '모카빵', 2500, 3),
    (@ord_id, @bread_cheese_stick, '치즈 스틱', 2100, 1);

/* 3/20 - 취소 주문 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'CANCELLED', 6400, 'MF23', '2026-03-20', '2026-03-20 18:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_milk, '우유식빵', 3200, 2);

/* 3/24 - 피자빵 2개, 모카빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 8100, 'MG45', '2026-03-24', '2026-03-24 17:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_pizza, '피자빵', 2800, 2),
    (@ord_id, @bread_mocha, '모카빵', 2500, 1);

/* 3/29 - 치즈 스틱 3개, 우유식빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 9500, 'MH67', '2026-03-29', '2026-03-29 19:15:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_cheese_stick, '치즈 스틱', 2100, 3),
    (@ord_id, @bread_milk, '우유식빵', 3200, 1);

/* ============================================================
   삼성점 주문 — 4월 (12건 + 픽업대기 1건)
   ============================================================ */

/* 4/1 - 우유식빵 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 6400, 'MJ12', '2026-04-01', '2026-04-01 17:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_milk, '우유식빵', 3200, 2);

/* 4/3 - 모카빵 2개, 피자빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 7800, 'MK34', '2026-04-03', '2026-04-03 18:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_mocha, '모카빵', 2500, 2),
    (@ord_id, @bread_pizza, '피자빵', 2800, 1);

/* 4/5 - 치즈 스틱 5개 (주말 대량) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 10500, 'ML56', '2026-04-05', '2026-04-05 16:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_cheese_stick, '치즈 스틱', 2100, 5);

/* 4/7 - 피자빵 2개, 우유식빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 8800, 'MM78', '2026-04-07', '2026-04-07 19:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_pizza, '피자빵', 2800, 2),
    (@ord_id, @bread_milk, '우유식빵', 3200, 1);

/* 4/9 - 모카빵 4개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 10000, 'MN91', '2026-04-09', '2026-04-09 20:15:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_mocha, '모카빵', 2500, 4);

/* 4/11 - 우유식빵 1개, 치즈 스틱 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 7400, 'MP23', '2026-04-11', '2026-04-11 17:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_milk, '우유식빵', 3200, 1),
    (@ord_id, @bread_cheese_stick, '치즈 스틱', 2100, 2);

/* 4/14 - 피자빵 3개, 모카빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 10900, 'MQ45', '2026-04-14', '2026-04-14 18:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_pizza, '피자빵', 2800, 3),
    (@ord_id, @bread_mocha, '모카빵', 2500, 1);

/* 4/17 - 치즈 스틱 3개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 6300, 'MR67', '2026-04-17', '2026-04-17 19:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_cheese_stick, '치즈 스틱', 2100, 3);

/* 4/20 - 우유식빵 3개 (주말) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 9600, 'MS89', '2026-04-20', '2026-04-20 15:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_milk, '우유식빵', 3200, 3);

/* 4/23 - 취소 주문 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'CANCELLED', 5000, 'MT12', '2026-04-23', '2026-04-23 18:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_mocha, '모카빵', 2500, 2);

/* 4/25 - 모카빵 1개, 피자빵 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 8100, 'MU34', '2026-04-25', '2026-04-25 17:15:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_mocha, '모카빵', 2500, 1),
    (@ord_id, @bread_pizza, '피자빵', 2800, 2);

/* 4/29 - 치즈 스틱 2개, 우유식빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'PICKED_UP', 7400, 'MV56', '2026-04-29', '2026-04-29 20:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_cheese_stick, '치즈 스틱', 2100, 2),
    (@ord_id, @bread_milk, '우유식빵', 3200, 1);

/* 삼성점 픽업 대기 주문 (CONFIRMED, 오늘 날짜) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_samsung_id, 'CONFIRMED', 5600, 'MW78', CURDATE(), NOW());
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_pizza, '피자빵', 2800, 2);


/* ============================================================
   대치점 주문 — 3월 (9건)
   ============================================================ */

/* 3/1 - 통밀빵 2개, 에그타르트 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 9600, 'DA12', '2026-03-01', '2026-03-01 18:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_whole_wheat, '통밀빵', 3500, 2),
    (@ord_id, @bread_egg_tart, '에그타르트', 2600, 1);

/* 3/4 - 호두파이 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 8400, 'DB34', '2026-03-04', '2026-03-04 19:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_walnut_pie, '호두파이', 4200, 2);

/* 3/8 - 팥앙금빵 3개, 크로플 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 10700, 'DC56', '2026-03-08', '2026-03-08 17:15:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_red_angeum, '팥앙금빵', 2500, 3),
    (@ord_id, @bread_croffle, '크로플', 3200, 1);

/* 3/11 - 에그타르트 4개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 10400, 'DD12', '2026-03-11', '2026-03-11 20:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_egg_tart, '에그타르트', 2600, 4);

/* 3/14 - 통밀빵 1개, 호두파이 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 7700, 'DE34', '2026-03-14', '2026-03-14 18:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_whole_wheat, '통밀빵', 3500, 1),
    (@ord_id, @bread_walnut_pie, '호두파이', 4200, 1);

/* 3/18 - 크로플 2개, 팥앙금빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 8900, 'DF56', '2026-03-18', '2026-03-18 16:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_croffle, '크로플', 3200, 2),
    (@ord_id, @bread_red_angeum, '팥앙금빵', 2500, 1);

/* 3/21 - 취소 주문 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'CANCELLED', 7000, 'DG78', '2026-03-21', '2026-03-21 18:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_whole_wheat, '통밀빵', 3500, 2);

/* 3/25 - 호두파이 1개, 에그타르트 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 9400, 'DH91', '2026-03-25', '2026-03-25 19:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_walnut_pie, '호두파이', 4200, 1),
    (@ord_id, @bread_egg_tart, '에그타르트', 2600, 2);

/* 3/29 - 팥앙금빵 2개, 통밀빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 8500, 'DJ23', '2026-03-29', '2026-03-29 17:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_red_angeum, '팥앙금빵', 2500, 2),
    (@ord_id, @bread_whole_wheat, '통밀빵', 3500, 1);

/* ============================================================
   대치점 주문 — 4월 (10건 + 픽업대기 2건)
   ============================================================ */

/* 4/1 - 통밀빵 2개, 팥앙금빵 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 9500, 'DK12', '2026-04-01', '2026-04-01 17:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_whole_wheat, '통밀빵', 3500, 2),
    (@ord_id, @bread_red_angeum, '팥앙금빵', 2500, 1);

/* 4/3 - 에그타르트 3개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 7800, 'DL34', '2026-04-03', '2026-04-03 18:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_egg_tart, '에그타르트', 2600, 3);

/* 4/5 - 호두파이 3개 (주말) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 12600, 'DM56', '2026-04-05', '2026-04-05 16:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_walnut_pie, '호두파이', 4200, 3);

/* 4/8 - 크로플 2개, 에그타르트 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 9000, 'DN78', '2026-04-08', '2026-04-08 19:15:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_croffle, '크로플', 3200, 2),
    (@ord_id, @bread_egg_tart, '에그타르트', 2600, 1);

/* 4/10 - 팥앙금빵 4개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 10000, 'DP91', '2026-04-10', '2026-04-10 20:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_red_angeum, '팥앙금빵', 2500, 4);

/* 4/13 - 통밀빵 1개, 호두파이 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 11900, 'DQ23', '2026-04-13', '2026-04-13 17:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_whole_wheat, '통밀빵', 3500, 1),
    (@ord_id, @bread_walnut_pie, '호두파이', 4200, 2);

/* 4/16 - 에그타르트 2개, 크로플 1개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 8400, 'DR45', '2026-04-16', '2026-04-16 18:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_egg_tart, '에그타르트', 2600, 2),
    (@ord_id, @bread_croffle, '크로플', 3200, 1);

/* 4/20 - 취소 주문 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'CANCELLED', 8400, 'DS67', '2026-04-20', '2026-04-20 18:00:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_walnut_pie, '호두파이', 4200, 2);

/* 4/23 - 팥앙금빵 2개, 통밀빵 2개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 12000, 'DT89', '2026-04-23', '2026-04-23 19:30:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_red_angeum, '팥앙금빵', 2500, 2),
    (@ord_id, @bread_whole_wheat, '통밀빵', 3500, 2);

/* 4/27 - 호두파이 1개, 에그타르트 3개 */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'PICKED_UP', 12000, 'DU12', '2026-04-27', '2026-04-27 16:45:00');
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_walnut_pie, '호두파이', 4200, 1),
    (@ord_id, @bread_egg_tart, '에그타르트', 2600, 3);

/* 대치점 픽업 대기 주문 (CONFIRMED, 오늘 날짜) */
INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'CONFIRMED', 7000, 'DV34', CURDATE(), NOW());
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_whole_wheat, '통밀빵', 3500, 2);

INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
VALUES (@demo_user_id, @demo_store_daechi_id, 'CONFIRMED', 5200, 'DW56', CURDATE(), NOW());
SET @ord_id = LAST_INSERT_ID();
INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity) VALUES
    (@ord_id, @bread_egg_tart, '에그타르트', 2600, 2);

COMMIT;
