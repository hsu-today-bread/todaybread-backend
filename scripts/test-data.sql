SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET collation_connection = 'utf8mb4_unicode_ci';

/*
 개발용 테스트 데이터 스크립트

 재실행 가능하도록, 아래 샘플 계정/매장에 해당하는 데이터만 먼저 정리한 뒤 다시 삽입합니다.

 샘플 로그인 계정
 - 일반 유저: demo-user@todaybread.local / todaybread123
 - 사장님 1: demo-boss-gangnam@todaybread.local / todaybread123
 - 사장님 2: demo-boss-seolleung@todaybread.local / todaybread123

 근처 매장/빵 조회 추천 좌표
 - lat=37.4980950
 - lng=127.0276100
 - radius=3
 */

SET @test_data_password_hash = '$argon2id$v=19$m=16384,t=2,p=1$q74E5AXcCNxyfKx3iCCtEQ$12t7qic0oWGPI8R9E3T8uCO+q0sP+fsK1paO8Hc3hXY';

SET @demo_user_email = 'demo-user@todaybread.local';
SET @demo_boss_gangnam_email = 'demo-boss-gangnam@todaybread.local';
SET @demo_boss_seolleung_email = 'demo-boss-seolleung@todaybread.local';

START TRANSACTION;

DELETE fs
FROM favourite_store fs
LEFT JOIN users u ON fs.user_id = u.id
LEFT JOIN store s ON fs.store_id = s.id
LEFT JOIN users su ON s.user_id = su.id
WHERE u.email = @demo_user_email
   OR su.email IN (@demo_boss_gangnam_email, @demo_boss_seolleung_email);

DELETE uk
FROM user_keyword uk
JOIN users u ON uk.user_id = u.id
WHERE u.email = @demo_user_email;

DELETE bi
FROM bread_image bi
JOIN bread b ON bi.bread_id = b.id
JOIN store s ON b.store_id = s.id
JOIN users u ON s.user_id = u.id
WHERE u.email IN (@demo_boss_gangnam_email, @demo_boss_seolleung_email);

DELETE b
FROM bread b
JOIN store s ON b.store_id = s.id
JOIN users u ON s.user_id = u.id
WHERE u.email IN (@demo_boss_gangnam_email, @demo_boss_seolleung_email);

DELETE si
FROM store_image si
JOIN store s ON si.store_id = s.id
JOIN users u ON s.user_id = u.id
WHERE u.email IN (@demo_boss_gangnam_email, @demo_boss_seolleung_email);

DELETE sbh
FROM store_business_hours sbh
JOIN store s ON sbh.store_id = s.id
JOIN users u ON s.user_id = u.id
WHERE u.email IN (@demo_boss_gangnam_email, @demo_boss_seolleung_email);

DELETE s
FROM store s
JOIN users u ON s.user_id = u.id
WHERE u.email IN (@demo_boss_gangnam_email, @demo_boss_seolleung_email);

DELETE rt
FROM refresh_token rt
JOIN users u ON rt.user_id = u.id
WHERE u.email IN (
    @demo_user_email,
    @demo_boss_gangnam_email,
    @demo_boss_seolleung_email
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
    @demo_boss_seolleung_email
);

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
    );

SET @demo_store_gangnam_id = (
    SELECT id FROM store WHERE user_id = @demo_boss_gangnam_id
);
SET @demo_store_seolleung_id = (
    SELECT id FROM store WHERE user_id = @demo_boss_seolleung_id
);

INSERT INTO store_business_hours (
    store_id,
    day_of_week,
    is_closed,
    start_time,
    end_time,
    last_order_time
) VALUES
    (@demo_store_gangnam_id, 1, FALSE, '07:00:00', '23:00:00', '22:30:00'),
    (@demo_store_gangnam_id, 2, FALSE, '07:00:00', '23:00:00', '22:30:00'),
    (@demo_store_gangnam_id, 3, FALSE, '07:00:00', '23:00:00', '22:30:00'),
    (@demo_store_gangnam_id, 4, FALSE, '07:00:00', '23:00:00', '22:30:00'),
    (@demo_store_gangnam_id, 5, FALSE, '07:00:00', '23:00:00', '22:30:00'),
    (@demo_store_gangnam_id, 6, FALSE, '07:00:00', '23:00:00', '22:30:00'),
    (@demo_store_gangnam_id, 7, FALSE, '07:00:00', '23:00:00', '22:30:00'),
    (@demo_store_seolleung_id, 1, FALSE, '09:00:00', '21:00:00', '20:30:00'),
    (@demo_store_seolleung_id, 2, FALSE, '09:00:00', '21:00:00', '20:30:00'),
    (@demo_store_seolleung_id, 3, FALSE, '09:00:00', '21:00:00', '20:30:00'),
    (@demo_store_seolleung_id, 4, FALSE, '09:00:00', '21:00:00', '20:30:00'),
    (@demo_store_seolleung_id, 5, FALSE, '09:00:00', '21:00:00', '20:30:00'),
    (@demo_store_seolleung_id, 6, FALSE, '09:00:00', '21:00:00', '20:30:00'),
    (@demo_store_seolleung_id, 7, FALSE, '09:00:00', '21:00:00', '20:30:00');

INSERT INTO bread (
    store_id,
    name,
    original_price,
    sale_price,
    remaining_quantity,
    description
) VALUES
    (
        @demo_store_gangnam_id,
        '시그니처 소금빵',
        3500,
        2500,
        14,
        '겉은 바삭하고 속은 촉촉한 대표 메뉴입니다.'
    ),
    (
        @demo_store_gangnam_id,
        '바질 치아바타',
        5200,
        3900,
        8,
        '점심용으로 바로 먹기 좋은 바질 치아바타입니다.'
    ),
    (
        @demo_store_gangnam_id,
        '밤식빵',
        6800,
        5000,
        0,
        '오늘은 품절 상태를 확인할 수 있도록 재고를 0으로 넣어둔 메뉴입니다.'
    ),
    (
        @demo_store_seolleung_id,
        '버터 크루아상',
        4200,
        3200,
        12,
        '겹이 살아 있는 기본 크루아상입니다.'
    ),
    (
        @demo_store_seolleung_id,
        '앙버터 바게트',
        5800,
        4300,
        5,
        '달콤한 팥과 버터 조합을 확인하기 좋은 메뉴입니다.'
    ),
    (
        @demo_store_seolleung_id,
        '사워도우 하프',
        9000,
        7000,
        3,
        '프론트 리스트와 상세 화면에서 가격 비교가 잘 보이는 메뉴입니다.'
    );

INSERT INTO favourite_store (user_id, store_id) VALUES
    (@demo_user_id, @demo_store_gangnam_id),
    (@demo_user_id, @demo_store_seolleung_id);

COMMIT;
