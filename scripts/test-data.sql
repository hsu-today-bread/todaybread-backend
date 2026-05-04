SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET collation_connection = 'utf8mb4_unicode_ci';

/*
 개발용 테스트 데이터 스크립트 (v2)

 재실행 가능하도록, 아래 샘플 계정/매장에 해당하는 데이터만 먼저 정리한 뒤 다시 삽입합니다.

 샘플 로그인 계정
 - 일반 유저:  demo-user@todaybread.com / todaybread123
 - 사장님 1~10: demo-boss1@todaybread.com ~ demo-boss10@todaybread.com / todaybread123

 근처 매장/빵 조회 추천 좌표 (강남역 기준)
 - lat=37.4980950
 - lng=127.0276100
 - radius=5

 이미지
 - 빵 이미지 10장(seed_bread_01 ~ seed_bread_10)을 돌려서 사용
 - 매장 이미지 10장(seed_store_01 ~ seed_store_10)을 중복 없이 하나씩 사용
 - scripts/test-data.sh 가 uploads/ 에 SVG 플레이스홀더를 생성합니다.
   실제 이미지로 교체하려면 scripts/seed-images/ 에 같은 파일명으로 넣어주세요.
 */

SET @pw = '$argon2id$v=19$m=16384,t=2,p=1$q74E5AXcCNxyfKx3iCCtEQ$12t7qic0oWGPI8R9E3T8uCO+q0sP+fsK1paO8Hc3hXY';

SET @e_user   = 'demo-user@todaybread.com';
SET @e_boss1  = 'demo-boss1@todaybread.com';
SET @e_boss2  = 'demo-boss2@todaybread.com';
SET @e_boss3  = 'demo-boss3@todaybread.com';
SET @e_boss4  = 'demo-boss4@todaybread.com';
SET @e_boss5  = 'demo-boss5@todaybread.com';
SET @e_boss6  = 'demo-boss6@todaybread.com';
SET @e_boss7  = 'demo-boss7@todaybread.com';
SET @e_boss8  = 'demo-boss8@todaybread.com';
SET @e_boss9  = 'demo-boss9@todaybread.com';
SET @e_boss10 = 'demo-boss10@todaybread.com';

START TRANSACTION;

/* ============================================================
   기존 데이터 정리 (재실행 가능)
   ============================================================ */

/* -- 구버전(v1) seed 이메일도 함께 정리 -- */
SET @old_user   = 'demo-user@todaybread.local';
SET @old_boss1  = 'demo-boss-gangnam@todaybread.local';
SET @old_boss2  = 'demo-boss-seolleung@todaybread.local';
SET @old_boss3  = 'demo-boss-yeoksam@todaybread.local';
SET @old_boss4  = 'demo-boss-samsung@todaybread.local';
SET @old_boss5  = 'demo-boss-daechi@todaybread.local';

DELETE fs FROM favourite_store fs
LEFT JOIN users u  ON fs.user_id  = u.id
LEFT JOIN store s  ON fs.store_id = s.id
LEFT JOIN users su ON s.user_id   = su.id
WHERE u.email IN (@e_user, @old_user)
   OR su.email IN (
       @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
       @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
       @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
   );

DELETE uk FROM user_keyword uk
JOIN users u ON uk.user_id = u.id
WHERE u.email IN (@e_user, @old_user);

DELETE oi FROM order_item oi
JOIN orders o ON oi.order_id = o.id
JOIN store s  ON o.store_id  = s.id
JOIN users u  ON s.user_id   = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE p FROM payment p
JOIN orders o ON p.order_id = o.id
JOIN store s  ON o.store_id = s.id
JOIN users u  ON s.user_id  = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE o FROM orders o
JOIN store s ON o.store_id = s.id
JOIN users u ON s.user_id  = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE ci FROM cart_item ci
JOIN cart c ON ci.cart_id = c.id
LEFT JOIN users cu ON c.user_id = cu.id
LEFT JOIN store s  ON c.store_id = s.id
LEFT JOIN users su ON s.user_id  = su.id
WHERE cu.email IN (@e_user, @old_user,
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10)
   OR su.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE c FROM cart c
LEFT JOIN users cu ON c.user_id = cu.id
LEFT JOIN store s  ON c.store_id = s.id
LEFT JOIN users su ON s.user_id  = su.id
WHERE cu.email IN (@e_user, @old_user,
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10)
   OR su.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE bi FROM bread_image bi
JOIN bread b ON bi.bread_id = b.id
JOIN store s ON b.store_id  = s.id
JOIN users u ON s.user_id   = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE b FROM bread b
JOIN store s ON b.store_id = s.id
JOIN users u ON s.user_id  = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE si FROM store_image si
JOIN store s ON si.store_id = s.id
JOIN users u ON s.user_id   = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE sbh FROM store_business_hours sbh
JOIN store s ON sbh.store_id = s.id
JOIN users u ON s.user_id    = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE s FROM store s
JOIN users u ON s.user_id = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE rt FROM refresh_token rt
JOIN users u ON rt.user_id = u.id
WHERE u.email IN (
    @e_user, @old_user,
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE FROM keyword WHERE normalised_text LIKE 'tb-test-data-%' OR normalised_text LIKE 'tb-seed-%';

DELETE FROM users WHERE email IN (
    @e_user, @old_user,
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

/* ============================================================
   유저 삽입 (일반 유저 1 + 사장님 10)
   ============================================================ */

INSERT INTO users (email, name, password_hash, nickname, phone_number, is_boss) VALUES
    (@e_user,   '데모 유저',    @pw, 'demo-user',   '010-9000-0001', FALSE),
    (@e_boss1,  '사장님 01',    @pw, 'demo-boss1',  '010-9000-1001', TRUE),
    (@e_boss2,  '사장님 02',    @pw, 'demo-boss2',  '010-9000-1002', TRUE),
    (@e_boss3,  '사장님 03',    @pw, 'demo-boss3',  '010-9000-1003', TRUE),
    (@e_boss4,  '사장님 04',    @pw, 'demo-boss4',  '010-9000-1004', TRUE),
    (@e_boss5,  '사장님 05',    @pw, 'demo-boss5',  '010-9000-1005', TRUE),
    (@e_boss6,  '사장님 06',    @pw, 'demo-boss6',  '010-9000-1006', TRUE),
    (@e_boss7,  '사장님 07',    @pw, 'demo-boss7',  '010-9000-1007', TRUE),
    (@e_boss8,  '사장님 08',    @pw, 'demo-boss8',  '010-9000-1008', TRUE),
    (@e_boss9,  '사장님 09',    @pw, 'demo-boss9',  '010-9000-1009', TRUE),
    (@e_boss10, '사장님 10',    @pw, 'demo-boss10', '010-9000-1010', TRUE);

SET @uid  = (SELECT id FROM users WHERE email = @e_user);
SET @b1   = (SELECT id FROM users WHERE email = @e_boss1);
SET @b2   = (SELECT id FROM users WHERE email = @e_boss2);
SET @b3   = (SELECT id FROM users WHERE email = @e_boss3);
SET @b4   = (SELECT id FROM users WHERE email = @e_boss4);
SET @b5   = (SELECT id FROM users WHERE email = @e_boss5);
SET @b6   = (SELECT id FROM users WHERE email = @e_boss6);
SET @b7   = (SELECT id FROM users WHERE email = @e_boss7);
SET @b8   = (SELECT id FROM users WHERE email = @e_boss8);
SET @b9   = (SELECT id FROM users WHERE email = @e_boss9);
SET @b10  = (SELECT id FROM users WHERE email = @e_boss10);

/* ============================================================
   키워드
   ============================================================ */

INSERT INTO keyword (normalised_text) VALUES
    ('tb-test-data-croissant'),
    ('tb-test-data-sourdough'),
    ('tb-test-data-bagel');

INSERT INTO user_keyword (user_id, keyword_id, display_text)
SELECT @uid, k.id,
    CASE k.normalised_text
        WHEN 'tb-test-data-croissant' THEN '크루아상'
        WHEN 'tb-test-data-sourdough' THEN '사워도우'
        WHEN 'tb-test-data-bagel'     THEN '베이글'
    END
FROM keyword k
WHERE k.normalised_text IN ('tb-test-data-croissant','tb-test-data-sourdough','tb-test-data-bagel');

/* ============================================================
   매장 삽입 (10개) — 강남역(37.4981, 127.0276) 반경 ~5km
   ============================================================ */

INSERT INTO store (user_id, name, phone_number, description, address_line1, address_line2, latitude, longitude, is_active) VALUES
    (@b1,  '르뺑드마리 강남점',   '02-9000-3001', '프랑스식 정통 빵을 굽는 강남역 근처 베이커리입니다.',       '서울특별시 강남구 테헤란로 123',   '1층',     37.4981000, 127.0276000, TRUE),
    (@b2,  '밀도 선릉점',         '02-9000-3002', '매일 아침 갓 구운 식빵과 샌드위치를 판매합니다.',           '서울특별시 강남구 선릉로 431',     '2층',     37.5045000, 127.0489000, TRUE),
    (@b3,  '나폴레옹과자점 역삼점','02-9000-3003', '40년 전통의 수제 과자와 빵을 만듭니다.',                   '서울특별시 강남구 역삼로 210',     '1층',     37.5000000, 127.0365000, TRUE),
    (@b4,  '오월의종 삼성점',      '02-9000-3004', '천연 발효종으로 만든 건강한 빵을 판매합니다.',              '서울특별시 강남구 삼성로 512',     '지하 1층', 37.5088000, 127.0630000, TRUE),
    (@b5,  '빵명장 대치점',        '02-9000-3005', '장인이 직접 구운 프리미엄 빵을 만나보세요.',               '서울특별시 강남구 대치로 85',      '2층',     37.4945000, 127.0580000, TRUE),
    (@b6,  '쿠헨 논현점',          '02-9000-3006', '독일식 호밀빵과 프레첼 전문 베이커리입니다.',              '서울특별시 강남구 논현로 654',     '1층',     37.5110000, 127.0230000, TRUE),
    (@b7,  '아티장베이커리 청담점', '02-9000-3007', '유기농 밀가루로 만든 수제 빵을 판매합니다.',               '서울특별시 강남구 청담동 12-3',    '1층',     37.5200000, 127.0470000, TRUE),
    (@b8,  '브레드랩 서초점',       '02-9000-3008', '실험적인 레시피로 새로운 빵을 연구합니다.',               '서울특별시 서초구 서초대로 321',   '3층',     37.4920000, 127.0100000, TRUE),
    (@b9,  '밀밭제과 잠실점',       '02-9000-3009', '동네 주민이 사랑하는 따뜻한 동네 빵집입니다.',            '서울특별시 송파구 올림픽로 240',   '1층',     37.5130000, 127.0850000, TRUE),
    (@b10, '뚜레쥬르 양재점',       '02-9000-3010', '매일 신선한 빵과 케이크를 만듭니다.',                    '서울특별시 서초구 양재대로 55',    '1층',     37.4840000, 127.0340000, TRUE);

SET @s1  = (SELECT id FROM store WHERE user_id = @b1);
SET @s2  = (SELECT id FROM store WHERE user_id = @b2);
SET @s3  = (SELECT id FROM store WHERE user_id = @b3);
SET @s4  = (SELECT id FROM store WHERE user_id = @b4);
SET @s5  = (SELECT id FROM store WHERE user_id = @b5);
SET @s6  = (SELECT id FROM store WHERE user_id = @b6);
SET @s7  = (SELECT id FROM store WHERE user_id = @b7);
SET @s8  = (SELECT id FROM store WHERE user_id = @b8);
SET @s9  = (SELECT id FROM store WHERE user_id = @b9);
SET @s10 = (SELECT id FROM store WHERE user_id = @b10);

/* ============================================================
   매장 이미지 (10장을 중복 없이 하나씩)
   ============================================================ */

INSERT INTO store_image (store_id, original_filename, stored_filename, display_order) VALUES
    (@s1,  'seed-store-01.svg', 'seed_store_01.svg', 0),
    (@s2,  'seed-store-02.svg', 'seed_store_02.svg', 0),
    (@s3,  'seed-store-03.svg', 'seed_store_03.svg', 0),
    (@s4,  'seed-store-04.svg', 'seed_store_04.svg', 0),
    (@s5,  'seed-store-05.svg', 'seed_store_05.svg', 0),
    (@s6,  'seed-store-06.svg', 'seed_store_06.svg', 0),
    (@s7,  'seed-store-07.svg', 'seed_store_07.svg', 0),
    (@s8,  'seed-store-08.svg', 'seed_store_08.svg', 0),
    (@s9,  'seed-store-09.svg', 'seed_store_09.svg', 0),
    (@s10, 'seed-store-10.svg', 'seed_store_10.svg', 0);

/* ============================================================
   영업시간 — 개발 편의상 모든 요일 00:00~23:59 영업
   ============================================================ */

INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time)
SELECT s.id, d.day, FALSE, '00:00:00', '23:59:00', '23:58:00'
FROM store s
JOIN (SELECT 1 AS day UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7) d
WHERE s.id IN (@s1,@s2,@s3,@s4,@s5,@s6,@s7,@s8,@s9,@s10);


/* ============================================================
   빵 삽입 (각 가게 6~8개, 총 70개)
   빵 이미지 10장(seed_bread_01~10)을 돌려서 사용
   ============================================================ */

/* ── 가게 1: 르뺑드마리 강남점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@s1, '클래식 크루아상',     4200, 3200, 12, '겹겹이 바삭한 정통 프랑스식 크루아상입니다.'),
    (@s1, '통밀 캄파뉴',         7500, 5500,  3, '천연 발효종으로 만든 묵직한 캄파뉴입니다.'),
    (@s1, '에그 타르트',         3800, 2800,  8, '바삭한 페이스트리에 부드러운 커스터드가 가득합니다.'),
    (@s1, '갈릭 바게트',         5000, 3500, 15, '마늘 버터가 듬뿍 발린 바삭한 바게트입니다.'),
    (@s1, '초코 브리오슈',       4500, 3200,  0, '진한 초콜릿이 들어간 부드러운 브리오슈입니다.'),
    (@s1, '블루베리 스콘',       3500, 2500,  6, '블루베리가 톡톡 터지는 영국식 스콘입니다.'),
    (@s1, '호두 크랜베리 빵',    5500, 4000,  4, '호두와 크랜베리가 듬뿍 들어간 건강빵입니다.');

/* ── 가게 2: 밀도 선릉점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@s2, '우유 식빵',           4500, 3200, 10, '부드럽고 달콤한 프리미엄 우유식빵입니다.'),
    (@s2, '크림치즈 베이글',     4000, 2800,  7, '크림치즈가 듬뿍 들어간 쫄깃한 베이글입니다.'),
    (@s2, '시나몬 롤',           4800, 3500,  5, '달콤한 시나몬 향이 가득한 롤빵입니다.'),
    (@s2, '올리브 포카치아',     5200, 3800, 11, '올리브와 로즈마리가 올라간 이탈리안 포카치아입니다.'),
    (@s2, '단팥 크루아상',       4200, 3000,  0, '달콤한 팥앙금이 들어간 크루아상입니다.'),
    (@s2, '레몬 파운드케이크',   5000, 3500,  9, '상큼한 레몬 글레이즈가 올라간 파운드케이크입니다.'),
    (@s2, '흑미 식빵',           5500, 4000,  2, '흑미로 만든 건강한 식빵입니다.');

/* ── 가게 3: 나폴레옹과자점 역삼점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@s3, '소보로빵',            3000, 2100, 14, '바삭한 소보로 토핑이 올라간 추억의 빵입니다.'),
    (@s3, '카레빵',              3800, 2700,  6, '매콤한 카레가 가득 찬 튀김빵입니다.'),
    (@s3, '크림빵',              3200, 2300, 13, '부드러운 커스터드 크림이 가득합니다.'),
    (@s3, '피자빵',              4000, 2800,  0, '토마토소스와 치즈가 올라간 피자빵입니다.'),
    (@s3, '꽈배기 도넛',         2800, 2000, 10, '달콤한 설탕이 묻은 꽈배기 도넛입니다.'),
    (@s3, '햄치즈 샌드위치빵',   4500, 3200,  8, '햄과 치즈가 들어간 든든한 샌드위치빵입니다.'),
    (@s3, '밤식빵',              6800, 5000,  1, '달콤한 밤이 듬뿍 들어간 식빵입니다.');

/* ── 가게 4: 오월의종 삼성점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@s4, '사워도우 부울',       8500, 6500,  4, '48시간 발효한 정통 사워도우입니다.'),
    (@s4, '호밀빵',              6000, 4200,  7, '100% 호밀로 만든 독일식 빵입니다.'),
    (@s4, '치아바타',            5200, 3900,  9, '겉은 바삭하고 속은 쫄깃한 이탈리안 치아바타입니다.'),
    (@s4, '무화과 호두빵',       7000, 5200,  2, '무화과와 호두가 박힌 건강한 빵입니다.'),
    (@s4, '올리브 치아바타',     5500, 4000, 11, '블랙 올리브가 들어간 치아바타입니다.'),
    (@s4, '통밀 바게트',         4800, 3500,  0, '통밀로 만든 바삭한 바게트입니다.'),
    (@s4, '크랜베리 호밀빵',     6500, 4800,  5, '크랜베리가 들어간 새콤달콤한 호밀빵입니다.');

/* ── 가게 5: 빵명장 대치점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@s5, '앙버터 바게트',       5800, 4300,  6, '달콤한 팥과 버터의 환상 조합입니다.'),
    (@s5, '마카다미아 쿠키빵',   4200, 3000, 10, '마카다미아가 듬뿍 들어간 쿠키빵입니다.'),
    (@s5, '녹차 크루아상',       4500, 3200,  3, '녹차 크림이 들어간 크루아상입니다.'),
    (@s5, '모카빵',              3500, 2500, 15, '커피 향이 진한 모카 크림빵입니다.'),
    (@s5, '치즈 스틱',           3000, 2100,  0, '쭉 늘어나는 모짜렐라 치즈 스틱입니다.'),
    (@s5, '딸기 크림빵',         4000, 2800,  8, '신선한 딸기 크림이 가득한 빵입니다.'),
    (@s5, '소금빵',              3500, 2500, 12, '겉은 바삭하고 속은 촉촉한 소금빵입니다.');

/* ── 가게 6: 쿠헨 논현점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@s6, '프레첼',              3800, 2800, 11, '독일식 정통 프레첼입니다.'),
    (@s6, '라우겐 롤',           4200, 3000,  7, '라우겐 반죽으로 만든 쫄깃한 롤빵입니다.'),
    (@s6, '호밀 사워도우',       7000, 5200,  4, '독일식 호밀 사워도우입니다.'),
    (@s6, '버터 쿠헨',           5500, 4000,  9, '버터가 듬뿍 들어간 독일식 케이크빵입니다.'),
    (@s6, '슈톨렌',              9000, 6500,  1, '과일과 견과류가 가득한 독일 전통빵입니다.'),
    (@s6, '카이저 롤',           3500, 2500, 13, '바삭한 겉면의 오스트리아식 롤빵입니다.'),
    (@s6, '흑맥주빵',            6000, 4200,  0, '흑맥주로 반죽한 풍미 깊은 빵입니다.');

/* ── 가게 7: 아티장베이커리 청담점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@s7, '트러플 포카치아',     8000, 6000,  3, '트러플 오일을 넣은 프리미엄 포카치아입니다.'),
    (@s7, '얼그레이 스콘',       4500, 3200,  8, '얼그레이 향이 은은한 영국식 스콘입니다.'),
    (@s7, '크로플',              4500, 3200, 10, '크루아상 반죽으로 구운 바삭한 와플입니다.'),
    (@s7, '유자 파운드케이크',   5500, 4000,  5, '상큼한 유자가 들어간 파운드케이크입니다.'),
    (@s7, '피스타치오 크루아상', 5800, 4200,  0, '피스타치오 크림이 가득한 크루아상입니다.'),
    (@s7, '바닐라 브리오슈',     4200, 3000, 14, '바닐라빈이 들어간 부드러운 브리오슈입니다.'),
    (@s7, '잣 타르트',           6000, 4500,  2, '고소한 잣이 올라간 미니 타르트입니다.');

/* ── 가게 8: 브레드랩 서초점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@s8, '비건 통밀빵',         5000, 3500,  7, '동물성 재료 없이 만든 건강한 통밀빵입니다.'),
    (@s8, '글루텐프리 머핀',     4500, 3200,  4, '쌀가루로 만든 글루텐프리 블루베리 머핀입니다.'),
    (@s8, '흑임자 식빵',         5500, 4000, 11, '고소한 흑임자가 들어간 식빵입니다.'),
    (@s8, '당근 케이크빵',       4800, 3500,  6, '당근과 호두가 들어간 건강한 케이크빵입니다.'),
    (@s8, '귀리 쿠키',           3200, 2300, 15, '귀리와 건포도가 들어간 건강 쿠키입니다.'),
    (@s8, '고구마 크루아상',     5500, 3800,  0, '달콤한 고구마 무스가 들어간 크루아상입니다.'),
    (@s8, '두부 도넛',           3000, 2100,  9, '두부로 만든 담백한 도넛입니다.');

/* ── 가게 9: 밀밭제과 잠실점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@s9, '단팥빵',              3000, 2100, 13, '달콤한 팥소가 가득한 전통 단팥빵입니다.'),
    (@s9, '고로케',              3500, 2500,  8, '바삭한 튀김옷 속 크림 고로케입니다.'),
    (@s9, '메론빵',              3800, 2700, 10, '메론 모양의 달콤한 빵입니다.'),
    (@s9, '찹쌀 도넛',           2800, 2000,  5, '쫄깃한 찹쌀 반죽의 도넛입니다.'),
    (@s9, '마늘 크림치즈빵',     4200, 3000, 14, '마늘과 크림치즈가 조화로운 빵입니다.'),
    (@s9, '팥앙금빵',            3500, 2500,  0, '직접 만든 팥앙금이 들어간 부드러운 빵입니다.'),
    (@s9, '옥수수빵',            3200, 2300,  7, '달콤한 옥수수 크림이 들어간 빵입니다.');

/* ── 가게 10: 뚜레쥬르 양재점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description) VALUES
    (@s10, '허니 브레드',        5000, 3500, 11, '꿀이 듬뿍 발린 달콤한 식빵입니다.'),
    (@s10, '크로크무슈',         5500, 4000,  6, '햄과 치즈가 올라간 프랑스식 토스트입니다.'),
    (@s10, '티라미수 크림빵',    4500, 3200,  3, '티라미수 크림이 가득한 빵입니다.'),
    (@s10, '플레인 베이글',      3500, 2400, 15, '쫄깃한 식감의 기본 베이글입니다.'),
    (@s10, '초코 머핀',          3800, 2700,  0, '진한 초콜릿 머핀입니다.'),
    (@s10, '마늘 바게트',        5000, 3500,  9, '마늘 버터가 듬뿍 발린 바게트입니다.'),
    (@s10, '호두파이',           6000, 4200,  4, '고소한 호두가 가득한 미니 파이입니다.');


/* ============================================================
   빵 이미지 — 10장(seed_bread_01~10)을 돌려서 사용
   bread_image.bread_id 는 UNIQUE 이므로 빵 1개당 이미지 1개
   ============================================================ */

INSERT INTO bread_image (bread_id, original_filename, stored_filename)
SELECT b.id,
       CONCAT('seed-bread-', LPAD(((ROW_NUMBER() OVER (ORDER BY b.id) - 1) % 10) + 1, 2, '0'), '.svg'),
       CONCAT('seed_bread_', LPAD(((ROW_NUMBER() OVER (ORDER BY b.id) - 1) % 10) + 1, 2, '0'), '_', b.id, '.svg')
FROM bread b
WHERE b.store_id IN (@s1,@s2,@s3,@s4,@s5,@s6,@s7,@s8,@s9,@s10);

/* ============================================================
   즐겨찾기
   ============================================================ */

INSERT INTO favourite_store (user_id, store_id) VALUES
    (@uid, @s1),
    (@uid, @s3),
    (@uid, @s7);

/* ============================================================
   장바구니 샘플
   ============================================================ */

INSERT INTO cart (user_id, store_id) VALUES (@uid, @s1);
SET @cart_id = LAST_INSERT_ID();

INSERT INTO cart_item (cart_id, bread_id, quantity)
SELECT @cart_id, b.id, 2
FROM bread b WHERE b.store_id = @s1 ORDER BY b.id LIMIT 2;

/* ============================================================
   주문 / 매출 데이터 (2026-01-01 ~ 현재)
   각 가게별 월 6~10건, 상태 혼합 (PICKED_UP / CONFIRMED / CANCELLED)
   ============================================================ */

/* 주문 생성 프로시저 — 반복 INSERT를 줄이기 위한 헬퍼 */

DROP PROCEDURE IF EXISTS seed_orders;

DELIMITER //
CREATE PROCEDURE seed_orders()
BEGIN
    DECLARE v_store_id BIGINT;
    DECLARE v_store_idx INT DEFAULT 0;
    DECLARE v_month_start DATE;
    DECLARE v_month_end DATE;
    DECLARE v_order_date DATE;
    DECLARE v_order_count INT;
    DECLARE v_i INT;
    DECLARE v_status VARCHAR(20);
    DECLARE v_bread_id BIGINT;
    DECLARE v_bread_name VARCHAR(100);
    DECLARE v_bread_price INT;
    DECLARE v_qty INT;
    DECLARE v_total INT;
    DECLARE v_order_id BIGINT;
    DECLARE v_order_num VARCHAR(4);
    DECLARE v_rand DOUBLE;
    DECLARE v_bread_count INT;
    DECLARE v_bread_offset INT;
    DECLARE v_items_per_order INT;
    DECLARE v_j INT;
    DECLARE v_cur_month DATE;

    /* 10개 가게 ID를 임시 테이블에 */
    DROP TEMPORARY TABLE IF EXISTS tmp_stores;
    CREATE TEMPORARY TABLE tmp_stores (idx INT AUTO_INCREMENT PRIMARY KEY, store_id BIGINT);
    INSERT INTO tmp_stores (store_id) VALUES
        (@s1),(@s2),(@s3),(@s4),(@s5),(@s6),(@s7),(@s8),(@s9),(@s10);

    SET v_store_idx = 1;

    WHILE v_store_idx <= 10 DO
        SELECT store_id INTO v_store_id FROM tmp_stores WHERE idx = v_store_idx;

        /* 해당 가게의 빵 개수 */
        SELECT COUNT(*) INTO v_bread_count FROM bread WHERE store_id = v_store_id;

        /* 2026-01 부터 현재 월까지 반복 */
        SET v_cur_month = '2026-01-01';

        WHILE v_cur_month <= CURDATE() DO
            SET v_month_start = v_cur_month;
            SET v_month_end = LAST_DAY(v_cur_month);
            IF v_month_end > CURDATE() THEN
                SET v_month_end = CURDATE();
            END IF;

            /* 월별 주문 수: 6~10건 */
            SET v_order_count = 6 + FLOOR(RAND() * 5);
            SET v_i = 0;

            WHILE v_i < v_order_count DO
                /* 랜덤 날짜 */
                SET v_order_date = DATE_ADD(v_month_start,
                    INTERVAL FLOOR(RAND() * (DATEDIFF(v_month_end, v_month_start) + 1)) DAY);

                /* 상태: 80% PICKED_UP, 10% CONFIRMED, 10% CANCELLED */
                SET v_rand = RAND();
                IF v_rand < 0.10 THEN
                    SET v_status = 'CANCELLED';
                ELSEIF v_rand < 0.20 THEN
                    SET v_status = 'CONFIRMED';
                ELSE
                    SET v_status = 'PICKED_UP';
                END IF;

                /* 주문번호 4자리 영숫자 */
                SET v_order_num = UPPER(SUBSTR(MD5(CONCAT(v_store_id, v_order_date, v_i, RAND())), 1, 4));

                /* 주문 항목 수: 1~3개 */
                SET v_items_per_order = 1 + FLOOR(RAND() * 3);
                SET v_total = 0;

                /* 주문 먼저 삽입 (total_amount는 나중에 업데이트) */
                INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
                VALUES (@uid, v_store_id, v_status, 1, v_order_num, v_order_date,
                        TIMESTAMP(v_order_date, MAKETIME(15 + FLOOR(RAND()*7), FLOOR(RAND()*60), 0)));
                SET v_order_id = LAST_INSERT_ID();

                SET v_j = 0;
                WHILE v_j < v_items_per_order DO
                    /* 랜덤 빵 선택 */
                    SET v_bread_offset = FLOOR(RAND() * v_bread_count);
                    SELECT id, name, sale_price INTO v_bread_id, v_bread_name, v_bread_price
                    FROM bread WHERE store_id = v_store_id
                    ORDER BY id LIMIT 1 OFFSET v_bread_offset;

                    IF v_bread_id IS NOT NULL THEN
                        SET v_qty = 1 + FLOOR(RAND() * 4);
                        SET v_total = v_total + (v_bread_price * v_qty);

                        INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity)
                        VALUES (v_order_id, v_bread_id, v_bread_name, v_bread_price, v_qty);
                    END IF;

                    SET v_j = v_j + 1;
                END WHILE;

                /* total_amount 업데이트 */
                UPDATE orders SET total_amount = v_total WHERE id = v_order_id;

                SET v_i = v_i + 1;
            END WHILE;

            SET v_cur_month = DATE_ADD(v_cur_month, INTERVAL 1 MONTH);
        END WHILE;

        SET v_store_idx = v_store_idx + 1;
    END WHILE;

    DROP TEMPORARY TABLE IF EXISTS tmp_stores;
END //
DELIMITER ;

CALL seed_orders();
DROP PROCEDURE IF EXISTS seed_orders;

/* ============================================================
   각 가게별 오늘 날짜 CONFIRMED 주문 2건 (픽업 대기 테스트용)
   ============================================================ */

DROP PROCEDURE IF EXISTS seed_today_orders;

DELIMITER //
CREATE PROCEDURE seed_today_orders()
BEGIN
    DECLARE v_store_id BIGINT;
    DECLARE v_idx INT DEFAULT 1;
    DECLARE v_bread_id BIGINT;
    DECLARE v_bread_name VARCHAR(100);
    DECLARE v_bread_price INT;
    DECLARE v_qty INT;
    DECLARE v_order_id BIGINT;
    DECLARE v_order_num VARCHAR(4);

    DROP TEMPORARY TABLE IF EXISTS tmp_stores2;
    CREATE TEMPORARY TABLE tmp_stores2 (idx INT AUTO_INCREMENT PRIMARY KEY, store_id BIGINT);
    INSERT INTO tmp_stores2 (store_id) VALUES
        (@s1),(@s2),(@s3),(@s4),(@s5),(@s6),(@s7),(@s8),(@s9),(@s10);

    WHILE v_idx <= 10 DO
        SELECT store_id INTO v_store_id FROM tmp_stores2 WHERE idx = v_idx;

        /* 첫 번째 픽업 대기 주문 */
        SELECT id, name, sale_price INTO v_bread_id, v_bread_name, v_bread_price
        FROM bread WHERE store_id = v_store_id ORDER BY id LIMIT 1;

        SET v_qty = 1 + FLOOR(RAND() * 3);
        SET v_order_num = UPPER(SUBSTR(MD5(CONCAT('today1', v_store_id, RAND())), 1, 4));

        INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
        VALUES (@uid, v_store_id, 'CONFIRMED', v_bread_price * v_qty, v_order_num, CURDATE(), NOW());
        SET v_order_id = LAST_INSERT_ID();
        INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity)
        VALUES (v_order_id, v_bread_id, v_bread_name, v_bread_price, v_qty);

        /* 두 번째 픽업 대기 주문 */
        SELECT id, name, sale_price INTO v_bread_id, v_bread_name, v_bread_price
        FROM bread WHERE store_id = v_store_id ORDER BY id LIMIT 1 OFFSET 1;

        SET v_qty = 1 + FLOOR(RAND() * 2);
        SET v_order_num = UPPER(SUBSTR(MD5(CONCAT('today2', v_store_id, RAND())), 1, 4));

        INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
        VALUES (@uid, v_store_id, 'CONFIRMED', v_bread_price * v_qty, v_order_num, CURDATE(), NOW());
        SET v_order_id = LAST_INSERT_ID();
        INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity)
        VALUES (v_order_id, v_bread_id, v_bread_name, v_bread_price, v_qty);

        SET v_idx = v_idx + 1;
    END WHILE;

    DROP TEMPORARY TABLE IF EXISTS tmp_stores2;
END //
DELIMITER ;

CALL seed_today_orders();
DROP PROCEDURE IF EXISTS seed_today_orders;

/* ============================================================
   결제 데이터 — CONFIRMED/PICKED_UP 주문에 결제 레코드 매칭
   ============================================================ */

INSERT INTO payment (order_id, amount, status, paid_at, idempotency_key, payment_key, method)
SELECT
    o.id,
    o.total_amount,
    CASE o.status
        WHEN 'CANCELLED' THEN 'CANCELLED'
        ELSE 'APPROVED'
    END,
    o.created_at,
    CONCAT('seed-pay-', o.id),
    CONCAT('seed_pay_', o.id),
    'STUB'
FROM orders o
JOIN store s ON o.store_id = s.id
WHERE s.id IN (@s1,@s2,@s3,@s4,@s5,@s6,@s7,@s8,@s9,@s10)
  AND o.status IN ('CONFIRMED', 'PICKED_UP')
  AND NOT EXISTS (SELECT 1 FROM payment p WHERE p.order_id = o.id);

/* CANCELLED 주문에도 취소 결제 레코드 */
INSERT INTO payment (order_id, amount, status, paid_at, idempotency_key, payment_key, method, cancel_reason, cancelled_at)
SELECT
    o.id,
    o.total_amount,
    'CANCELLED',
    o.created_at,
    CONCAT('seed-pay-', o.id),
    CONCAT('seed_pay_', o.id),
    'STUB',
    '테스트 취소',
    DATE_ADD(o.created_at, INTERVAL 30 MINUTE)
FROM orders o
JOIN store s ON o.store_id = s.id
WHERE s.id IN (@s1,@s2,@s3,@s4,@s5,@s6,@s7,@s8,@s9,@s10)
  AND o.status = 'CANCELLED'
  AND NOT EXISTS (SELECT 1 FROM payment p WHERE p.order_id = o.id);

COMMIT;
