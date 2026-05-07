SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET collation_connection = 'utf8mb4_unicode_ci';

/*
 개발용 테스트 데이터 스크립트 (v5)

 기본 실행으로 서울 전역 120개 매장을 생성합니다.

 샘플 로그인 계정
 - 일반 유저:  demo-user01@todaybread.com / todaybread123
 - 사장님 1~120: demo-boss1@todaybread.com ~ demo-boss120@todaybread.com / todaybread123

 근처 매장/빵 조회 기본 좌표
 - 한성대학교 기준
 - lat=37.5826000
 - lng=127.0106000
 - radius=1 / 3 / 5

 데이터 규칙
 - 한성대 1km 이내 3개, 3km 이내 누적 10개, 5km 이내 누적 20개를 고정합니다.
 - 한성대 5km 안 매장은 20개만 배치하고, 나머지 100개는 5km 밖 서울 전역에 분산합니다.
 - seed 자체는 sellingStatus를 저장하지 않습니다. 영업시간 + 재고 조건으로 SELLING / OPEN_SOLD_OUT / CLOSED를 계산할 수 있게 만듭니다.
 - 주문은 PICKED_UP 또는 CANCELLED만 생성합니다. PENDING / CONFIRMED / CANCEL_PENDING은 생성하지 않습니다.
 - 주문/매출 날짜는 2026-01-01부터 2026-05-07까지만 사용합니다.
 - 매장별 월 주문은 최대 15건이며, 2026년 5월은 7일까지만 생성합니다.
 - 리뷰는 매장당 10개를 만들고, 이미지 리뷰 5개 + 텍스트 리뷰 5개를 유지합니다.
 - 이미지 리뷰는 리뷰당 1장 또는 2장만 연결합니다.
 */

SET @pw = '$argon2id$v=19$m=16384,t=2,p=1$q74E5AXcCNxyfKx3iCCtEQ$12t7qic0oWGPI8R9E3T8uCO+q0sP+fsK1paO8Hc3hXY';

DROP PROCEDURE IF EXISTS seed_demo_data;

DELIMITER //
CREATE PROCEDURE seed_demo_data()
BEGIN
    DECLARE v_i INT DEFAULT 1;
    DECLARE v_j INT DEFAULT 1;
    DECLARE v_k INT DEFAULT 1;
    DECLARE v_store_id BIGINT;
    DECLARE v_boss_id BIGINT;
    DECLARE v_user_id BIGINT;
    DECLARE v_cart_id BIGINT;
    DECLARE v_menu_count INT;
    DECLARE v_target_menu_count INT;
    DECLARE v_menu_idx INT;
    DECLARE v_bread_id BIGINT;
    DECLARE v_bread_name VARCHAR(100);
    DECLARE v_bread_desc VARCHAR(255);
    DECLARE v_original_price INT;
    DECLARE v_sale_price INT;
    DECLARE v_discount_percent INT;
    DECLARE v_quantity INT;
    DECLARE v_lat DECIMAL(10,7);
    DECLARE v_lng DECIMAL(10,7);
    DECLARE v_district_idx INT;
    DECLARE v_offset_idx INT;
    DECLARE v_district VARCHAR(30);
    DECLARE v_center_lat DECIMAL(10,7);
    DECLARE v_center_lng DECIMAL(10,7);
    DECLARE v_store_name VARCHAR(100);
    DECLARE v_store_desc VARCHAR(255);
    DECLARE v_prefix VARCHAR(30);
    DECLARE v_branch VARCHAR(30);
    DECLARE v_expected_status VARCHAR(20);
    DECLARE v_store_image_idx INT;
    DECLARE v_store_ext VARCHAR(10);
    DECLARE v_bread_image_idx INT;
    DECLARE v_bread_ext VARCHAR(10);
    DECLARE v_bread_image_sequence INT DEFAULT 1;
    DECLARE v_review_image_idx INT;
    DECLARE v_review_ext VARCHAR(10);
    DECLARE v_review_image_sequence INT DEFAULT 1;
    DECLARE v_review_image_count INT;
    DECLARE v_status VARCHAR(20);
    DECLARE v_payment_status VARCHAR(20);
    DECLARE v_order_id BIGINT;
    DECLARE v_order_item_id BIGINT;
    DECLARE v_order_date DATE;
    DECLARE v_created_at DATETIME(6);
    DECLARE v_order_number VARCHAR(4);
    DECLARE v_order_bread_id BIGINT;
    DECLARE v_order_bread_name VARCHAR(100);
    DECLARE v_order_bread_price INT;
    DECLARE v_qty INT;
    DECLARE v_month INT;
    DECLARE v_max_day INT;
    DECLARE v_month_order_count INT;
    DECLARE v_day_no INT;
    DECLARE v_sales_pattern INT;
    DECLARE v_pickup_seq INT;
    DECLARE v_global_order_seq INT DEFAULT 1;
    DECLARE v_rating INT;
    DECLARE v_content VARCHAR(500);
    DECLARE v_review_id BIGINT;
    DECLARE v_review_idx INT;
    DECLARE v_review_created_at DATETIME(6);
    DECLARE v_source_idx INT;

    DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_users;
    DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_stores;
    DROP TEMPORARY TABLE IF EXISTS tmp_district_centers;
    DROP TEMPORARY TABLE IF EXISTS tmp_store_specs;
    DROP TEMPORARY TABLE IF EXISTS tmp_seed_breads;
    DROP TEMPORARY TABLE IF EXISTS tmp_seed_order_items;
    DROP TEMPORARY TABLE IF EXISTS tmp_review_candidates;
    DROP TEMPORARY TABLE IF EXISTS tmp_bread_catalog;
    DROP TEMPORARY TABLE IF EXISTS tmp_review_contents;

    CREATE TEMPORARY TABLE tmp_cleanup_users (id BIGINT PRIMARY KEY);
    CREATE TEMPORARY TABLE tmp_cleanup_stores (id BIGINT PRIMARY KEY);

    INSERT INTO tmp_cleanup_users (id)
    SELECT id
    FROM users
    WHERE email IN ('demo-user@todaybread.com', 'demo-user01@todaybread.com')
       OR email LIKE 'demo-boss%@todaybread.com'
       OR email IN (
           'demo-user@todaybread.local',
           'demo-boss-gangnam@todaybread.local',
           'demo-boss-seolleung@todaybread.local',
           'demo-boss-yeoksam@todaybread.local',
           'demo-boss-samsung@todaybread.local',
           'demo-boss-daechi@todaybread.local'
       );

    INSERT INTO tmp_cleanup_stores (id)
    SELECT s.id
    FROM store s
    JOIN tmp_cleanup_users cu ON s.user_id = cu.id;

    DELETE ri FROM review_image ri
    JOIN review r ON ri.review_id = r.id
    LEFT JOIN tmp_cleanup_users cu ON r.user_id = cu.id
    LEFT JOIN tmp_cleanup_stores cs ON r.store_id = cs.id
    WHERE cu.id IS NOT NULL OR cs.id IS NOT NULL;

    DELETE r FROM review r
    LEFT JOIN tmp_cleanup_users cu ON r.user_id = cu.id
    LEFT JOIN tmp_cleanup_stores cs ON r.store_id = cs.id
    WHERE cu.id IS NOT NULL OR cs.id IS NOT NULL;

    DELETE fs FROM favourite_store fs
    LEFT JOIN tmp_cleanup_users cu ON fs.user_id = cu.id
    LEFT JOIN tmp_cleanup_stores cs ON fs.store_id = cs.id
    WHERE cu.id IS NOT NULL OR cs.id IS NOT NULL;

    DELETE uk FROM user_keyword uk
    JOIN tmp_cleanup_users cu ON uk.user_id = cu.id;

    DELETE p FROM payment p
    JOIN orders o ON p.order_id = o.id
    LEFT JOIN tmp_cleanup_users cu ON o.user_id = cu.id
    LEFT JOIN tmp_cleanup_stores cs ON o.store_id = cs.id
    WHERE cu.id IS NOT NULL OR cs.id IS NOT NULL;

    DELETE oi FROM order_item oi
    JOIN orders o ON oi.order_id = o.id
    LEFT JOIN tmp_cleanup_users cu ON o.user_id = cu.id
    LEFT JOIN tmp_cleanup_stores cs ON o.store_id = cs.id
    WHERE cu.id IS NOT NULL OR cs.id IS NOT NULL;

    DELETE o FROM orders o
    LEFT JOIN tmp_cleanup_users cu ON o.user_id = cu.id
    LEFT JOIN tmp_cleanup_stores cs ON o.store_id = cs.id
    WHERE cu.id IS NOT NULL OR cs.id IS NOT NULL;

    DELETE ci FROM cart_item ci
    JOIN cart c ON ci.cart_id = c.id
    LEFT JOIN tmp_cleanup_users cu ON c.user_id = cu.id
    LEFT JOIN tmp_cleanup_stores cs ON c.store_id = cs.id
    WHERE cu.id IS NOT NULL OR cs.id IS NOT NULL;

    DELETE c FROM cart c
    LEFT JOIN tmp_cleanup_users cu ON c.user_id = cu.id
    LEFT JOIN tmp_cleanup_stores cs ON c.store_id = cs.id
    WHERE cu.id IS NOT NULL OR cs.id IS NOT NULL;

    DELETE bi FROM bread_image bi
    JOIN bread b ON bi.bread_id = b.id
    JOIN tmp_cleanup_stores cs ON b.store_id = cs.id;

    DELETE b FROM bread b
    JOIN tmp_cleanup_stores cs ON b.store_id = cs.id;

    DELETE si FROM store_image si
    JOIN tmp_cleanup_stores cs ON si.store_id = cs.id;

    DELETE sbh FROM store_business_hours sbh
    JOIN tmp_cleanup_stores cs ON sbh.store_id = cs.id;

    DELETE s FROM store s
    JOIN tmp_cleanup_stores cs ON s.id = cs.id;

    DELETE rt FROM refresh_token rt
    JOIN tmp_cleanup_users cu ON rt.user_id = cu.id;

    DELETE prt FROM password_reset_token prt
    JOIN tmp_cleanup_users cu ON prt.user_id = cu.id;

    DELETE u FROM users u
    JOIN tmp_cleanup_users cu ON u.id = cu.id;

    CREATE TEMPORARY TABLE tmp_district_centers (
        idx INT PRIMARY KEY,
        district VARCHAR(30) NOT NULL,
        latitude DECIMAL(10,7) NOT NULL,
        longitude DECIMAL(10,7) NOT NULL
    );

    CREATE TEMPORARY TABLE tmp_store_specs (
        store_no INT PRIMARY KEY,
        store_id BIGINT NOT NULL,
        distance_bucket VARCHAR(20) NOT NULL,
        expected_status VARCHAR(20) NOT NULL,
        sales_pattern INT NOT NULL,
        menu_count INT NOT NULL
    );

    CREATE TEMPORARY TABLE tmp_seed_breads (
        store_no INT NOT NULL,
        menu_idx INT NOT NULL,
        bread_id BIGINT NOT NULL,
        bread_name VARCHAR(100) NOT NULL,
        sale_price INT NOT NULL,
        is_review_target BOOLEAN NOT NULL,
        PRIMARY KEY (store_no, menu_idx),
        INDEX idx_tmp_seed_breads_store_target (store_no, is_review_target),
        INDEX idx_tmp_seed_breads_bread_id (bread_id)
    );

    CREATE TEMPORARY TABLE tmp_seed_order_items (
        store_no INT NOT NULL,
        pickup_seq INT NOT NULL,
        order_item_id BIGINT NOT NULL,
        bread_id BIGINT NOT NULL,
        created_at DATETIME(6) NOT NULL,
        PRIMARY KEY (store_no, pickup_seq),
        INDEX idx_tmp_seed_order_items_bread_id (bread_id)
    );

    CREATE TEMPORARY TABLE tmp_bread_catalog (
        idx INT PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        description VARCHAR(255) NOT NULL,
        original_price INT NOT NULL
    );

    CREATE TEMPORARY TABLE tmp_review_contents (
        idx INT PRIMARY KEY,
        content VARCHAR(500) NOT NULL
    );

    INSERT INTO tmp_district_centers (idx, district, latitude, longitude) VALUES
        (1, '은평구', 37.6176000, 126.9227000),
        (2, '마포구', 37.5663000, 126.9015000),
        (3, '서대문구', 37.5791000, 126.9368000),
        (4, '용산구', 37.5326000, 126.9900000),
        (5, '강서구', 37.5509000, 126.8495000),
        (6, '양천구', 37.5169000, 126.8665000),
        (7, '구로구', 37.4955000, 126.8877000),
        (8, '금천구', 37.4569000, 126.8955000),
        (9, '영등포구', 37.5264000, 126.8963000),
        (10, '동작구', 37.5124000, 126.9393000),
        (11, '관악구', 37.4784000, 126.9516000),
        (12, '서초구', 37.4837000, 127.0324000),
        (13, '강남구', 37.5172000, 127.0473000),
        (14, '송파구', 37.5145000, 127.1059000),
        (15, '강동구', 37.5301000, 127.1238000),
        (16, '광진구', 37.5384000, 127.0823000),
        (17, '중랑구', 37.6063000, 127.0927000),
        (18, '노원구', 37.6542000, 127.0568000),
        (19, '도봉구', 37.6688000, 127.0471000),
        (20, '강북구', 37.6396000, 127.0257000);

    INSERT INTO tmp_bread_catalog (idx, name, description, original_price) VALUES
        (1, '소금빵', '버터 향과 짭짤한 맛이 살아 있는 대표 메뉴입니다.', 3600),
        (2, '크림빵', '부드러운 커스터드 크림을 가득 채운 인기 메뉴입니다.', 3400),
        (3, '단팥빵', '직접 끓인 팥앙금이 들어간 전통 단팥빵입니다.', 3200),
        (4, '식빵', '담백한 결이 살아 있는 데일리 식빵입니다.', 4800),
        (5, '우유 식빵', '우유와 버터 풍미가 부드럽게 퍼지는 식빵입니다.', 5600),
        (6, '밤식빵', '달콤한 밤을 듬뿍 넣은 식빵입니다.', 6800),
        (7, '모카빵', '커피 향이 은은한 추억의 모카빵입니다.', 3600),
        (8, '마늘바게트', '마늘 버터를 바른 바삭한 바게트입니다.', 5000),
        (9, '앙버터', '팥앙금과 버터를 채운 달콤한 메뉴입니다.', 5800),
        (10, '클래식 크루아상', '겹겹이 바삭한 버터 크루아상입니다.', 4600),
        (11, '아몬드 크루아상', '아몬드 크림과 슬라이스가 올라간 크루아상입니다.', 5400),
        (12, '초코 브리오슈', '초콜릿과 버터 풍미가 진한 브리오슈입니다.', 4800),
        (13, '블루베리 스콘', '블루베리가 톡톡 터지는 영국식 스콘입니다.', 3900),
        (14, '에그타르트', '바삭한 페이스트리와 커스터드가 어울립니다.', 3800),
        (15, '올리브 치아바타', '올리브와 허브 향이 좋은 담백한 치아바타입니다.', 5400),
        (16, '바질 포카치아', '바질과 토마토를 올린 촉촉한 포카치아입니다.', 5800),
        (17, '통밀 캄파뉴', '천연 발효종으로 묵직하게 구운 식사빵입니다.', 7600),
        (18, '사워도우', '48시간 발효해 산미가 깊은 사워도우입니다.', 8500),
        (19, '호밀빵', '고소한 호밀 향이 진한 건강빵입니다.', 6200),
        (20, '통밀 베이글', '통밀로 구워 고소하고 쫄깃한 베이글입니다.', 4200),
        (21, '크림치즈 베이글', '쫄깃한 베이글에 크림치즈를 듬뿍 넣었습니다.', 4400),
        (22, '카레빵', '매콤한 카레를 채운 든든한 튀김빵입니다.', 3800),
        (23, '명란바게트', '짭조름한 명란 크림을 바른 바게트입니다.', 5400),
        (24, '마늘 크림치즈빵', '마늘과 크림치즈가 조화로운 인기 메뉴입니다.', 4400),
        (25, '흑임자 식빵', '흑임자의 고소함을 살린 식빵입니다.', 5600),
        (26, '쑥 단팥빵', '쑥 향과 팥앙금이 어우러진 한국식 빵입니다.', 3800),
        (27, '고구마 식빵', '달콤한 고구마 무스를 넣은 식빵입니다.', 5800),
        (28, '카라멜 소금빵', '달콤한 카라멜과 짭짤한 버터가 만난 메뉴입니다.', 3900),
        (29, '얼그레이 파운드', '얼그레이 향이 은은한 파운드케이크입니다.', 5200),
        (30, '레몬 파운드', '상큼한 레몬 글레이즈를 올린 파운드케이크입니다.', 5000),
        (31, '두바이 쫀득 쿠키', '피스타치오와 초콜릿을 넣은 바이럴 쿠키입니다.', 6200),
        (32, '버터 떡', '버터 풍미와 쫀득한 식감이 어울리는 신메뉴입니다.', 4200),
        (33, '약과 퀸아망', '약과 풍미와 카라멜 식감이 만난 디저트빵입니다.', 5900),
        (34, '황치즈 소금빵', '진한 황치즈 크림을 채운 소금빵입니다.', 4500),
        (35, '옥수수 크림빵', '달콤한 옥수수 크림이 들어간 인기 메뉴입니다.', 4300),
        (36, '말차 생크림번', '쌉싸름한 말차와 생크림을 채운 번입니다.', 4700),
        (37, '피스타치오 크루핀', '크루아상 결에 피스타치오 크림을 채웠습니다.', 6500),
        (38, '고메버터 프레첼', '고메버터를 넣은 짭짤한 프레첼입니다.', 4300),
        (39, '대파 크림치즈 베이글', '대파 크림치즈가 든든하게 들어간 베이글입니다.', 5200),
        (40, '인절미 크림빵', '고소한 콩가루와 크림을 채운 한국식 디저트빵입니다.', 4200),
        (41, '초당옥수수 타르트', '초당옥수수의 단맛을 살린 시즌 타르트입니다.', 5600),
        (42, '브라운버터 휘낭시에', '브라운버터 향이 진한 한입 디저트입니다.', 3300),
        (43, '라즈베리 데니쉬', '새콤한 라즈베리잼을 올린 데니쉬입니다.', 4900),
        (44, '피칸 퀸아망', '카라멜과 피칸이 어울리는 바삭한 퀸아망입니다.', 5600),
        (45, '호두 무화과빵', '무화과와 호두를 듬뿍 넣은 식사빵입니다.', 6800),
        (46, '귀리 쿠키', '귀리와 건포도가 들어간 건강 쿠키입니다.', 3200),
        (47, '레몬 크림빵', '상큼한 레몬 크림을 넣은 부드러운 빵입니다.', 4100),
        (48, '트러플 포카치아', '트러플 오일을 더한 프리미엄 포카치아입니다.', 8000);

    INSERT INTO tmp_review_contents (idx, content) VALUES
        (1, '할인된 가격으로 먹기에는 충분히 만족스러운 품질이었습니다.'),
        (2, '갓 구운 빵 향이 좋고 포장도 깔끔해서 만족했습니다.'),
        (3, '가격 대비 양과 맛이 괜찮아서 친구에게도 추천하고 싶어요.'),
        (4, '버터 풍미가 진하고 식감이 좋아서 다음에도 들를 예정입니다.'),
        (5, '근처에서 간단히 사 가기 좋은 빵집입니다. 직원분도 친절했어요.'),
        (6, '재고가 넉넉하고 메뉴 구성이 다양해서 고르는 재미가 있었습니다.'),
        (7, '마감 할인으로 샀는데 상태가 좋아서 데모용으로 보기 좋았습니다.'),
        (8, '빵 크기가 적당하고 당일 픽업 흐름을 테스트하기 좋았습니다.'),
        (9, '앱에서 보고 바로 방문했는데 메뉴 사진과 실제 상품이 비슷했습니다.'),
        (10, '다음에도 근처에 있으면 다시 주문하고 싶은 메뉴였습니다.'),
        (11, '포장 시간이 빠르고 빵이 눅눅하지 않아서 만족스러웠습니다.'),
        (12, '종류가 다양해서 가족들과 나눠 먹기 좋았습니다.');

    INSERT INTO users (email, name, password_hash, nickname, phone_number, is_boss)
    VALUES ('demo-user01@todaybread.com', '데모 유저', @pw, 'demo-user01', '010-9000-0001', FALSE);
    SET v_user_id = LAST_INSERT_ID();

    SET v_i = 1;
    WHILE v_i <= 120 DO
        INSERT INTO users (email, name, password_hash, nickname, phone_number, is_boss)
        VALUES (
            CONCAT('demo-boss', v_i, '@todaybread.com'),
            CONCAT('사장님 ', LPAD(v_i, 3, '0')),
            @pw,
            CONCAT('demo-boss', v_i),
            CONCAT('010-9200-', LPAD(v_i, 4, '0')),
            TRUE
        );
        SET v_i = v_i + 1;
    END WHILE;

    INSERT IGNORE INTO keyword (normalised_text) VALUES ('소금빵'), ('크루아상'), ('식빵'), ('베이글'), ('휘낭시에');

    INSERT INTO user_keyword (user_id, keyword_id, display_text)
    SELECT v_user_id, k.id, k.normalised_text
    FROM keyword k
    WHERE k.normalised_text IN ('소금빵', '크루아상', '식빵', '베이글', '휘낭시에');

    SET v_i = 1;
    WHILE v_i <= 120 DO
        SET v_district = '성북구';
        SET v_expected_status = 'SELLING';
        SET v_sales_pattern = 1 + ((v_i - 1) MOD 5);

        IF v_i <= 20 THEN
            SET v_offset_idx = v_i;
            SET v_lat = 37.5826000 + CAST(ELT(v_offset_idx,
                '0.0020', '-0.0035', '0.0045',
                '0.0120', '0.0000', '-0.0140', '0.0180', '-0.0190', '0.0060', '-0.0080',
                '0.0320', '-0.0320', '0.0200', '-0.0220', '0.0390', '-0.0390', '0.0120', '-0.0150', '0.0270', '-0.0300'
            ) AS DECIMAL(10,7));
            SET v_lng = 127.0106000 + CAST(ELT(v_offset_idx,
                '0.0000', '0.0040', '-0.0045',
                '0.0000', '0.0180', '0.0120', '-0.0040', '-0.0100', '-0.0250', '0.0290',
                '0.0000', '0.0100', '0.0300', '-0.0320', '0.0100', '-0.0100', '0.0480', '-0.0500', '-0.0350', '0.0360'
            ) AS DECIMAL(10,7));
            SET v_district = ELT(v_offset_idx,
                '성북구', '성북구', '성북구',
                '성북구', '종로구', '동대문구', '성북구', '동대문구', '종로구', '중구',
                '강북구', '종로구', '성동구', '용산구', '강북구', '중구', '광진구', '서대문구', '성동구', '은평구'
            );
        ELSE
            SET v_district_idx = ((v_i - 21) MOD 20) + 1;
            SELECT district, latitude, longitude
            INTO v_district, v_center_lat, v_center_lng
            FROM tmp_district_centers
            WHERE idx = v_district_idx;

            SET v_offset_idx = FLOOR((v_i - 21) / 20) + 1;
            SET v_lat = v_center_lat + CAST(ELT(v_offset_idx, '0.0000', '0.0032', '-0.0030', '0.0018', '-0.0022') AS DECIMAL(10,7));
            SET v_lng = v_center_lng + CAST(ELT(v_offset_idx, '0.0030', '-0.0028', '0.0020', '-0.0035', '0.0008') AS DECIMAL(10,7));
        END IF;

        IF v_i <= 3 THEN
            SET v_expected_status = 'SELLING';
        ELSEIF v_i BETWEEN 4 AND 7 THEN
            SET v_expected_status = 'SELLING';
        ELSEIF v_i BETWEEN 8 AND 9 THEN
            SET v_expected_status = 'OPEN_SOLD_OUT';
        ELSEIF v_i = 10 THEN
            SET v_expected_status = 'CLOSED';
        ELSEIF v_i BETWEEN 11 AND 12 THEN
            SET v_expected_status = 'SELLING';
        ELSEIF v_i BETWEEN 13 AND 17 THEN
            SET v_expected_status = 'OPEN_SOLD_OUT';
        ELSEIF v_i BETWEEN 18 AND 20 THEN
            SET v_expected_status = 'CLOSED';
        ELSE
            SET v_expected_status = 'SELLING';
        END IF;

        IF v_i <= 3 THEN
            SET v_menu_count = 3 + ((v_i + 1) MOD 3);
        ELSEIF v_i <= 20 THEN
            SET v_menu_count = 10 + ((v_i * 3) MOD 6);
        ELSE
            SET v_menu_count = 8 + ((v_i * 5) MOD 8);
        END IF;

        SET v_target_menu_count = IF(v_menu_count <= 5, 2, LEAST(3, GREATEST(2, CEIL(v_menu_count / 5))));

        SET v_prefix = ELT(((v_i * 7) MOD 24) + 1,
            '하루', '버터문', '오븐', '밀밭', '소담', '새벽',
            '브리오', '브레드온', '따숨', '크러스트', '구름', '담백',
            '호밀', '모닝', '리틀', '플랜트', '문라이트', '온도',
            '다정', '스윗', '브라운', '세이지', '밀도', '베이크'
        );
        SET v_branch = ELT(((v_i * 11) MOD 20) + 1,
            '한성대입구', '성북천', '혜화', '보문', '성신여대',
            '연남', '망원', '신촌', '공덕', '이태원',
            '강남역', '역삼', '잠실', '천호', '노원',
            '수유', '목동', '구로디지털', '서울대입구', '여의도'
        );

        IF v_i <= 20 THEN
            SET v_branch = ELT(v_i,
                '한성대입구', '성북천', '삼선교',
                '성신여대', '혜화', '보문', '길음', '안암', '동묘앞', '동대문',
                '미아사거리', '종로5가', '왕십리', '숙대입구', '수유',
                '을지로', '군자', '서대문', '뚝섬', '불광'
            );
        END IF;

        IF MOD(v_i, 4) = 0 THEN
            SET v_store_name = CONCAT(v_prefix, ' 베이커리 ', v_branch, '점');
        ELSEIF MOD(v_i, 4) = 1 THEN
            SET v_store_name = CONCAT('파리바게트 ', v_branch, '점');
        ELSEIF MOD(v_i, 4) = 2 THEN
            SET v_store_name = CONCAT('뚜레쥬르 ', v_branch, '점');
        ELSE
            SET v_store_name = CONCAT(v_prefix, ' 빵집 ', v_branch, '점');
        END IF;

        IF v_i <= 3 THEN
            SET v_store_desc = '한성대학교 1km 안에서 판매중 상태를 안정적으로 보여주는 데모 매장입니다.';
        ELSEIF v_i <= 20 AND v_expected_status = 'OPEN_SOLD_OUT' THEN
            SET v_store_desc = '영업시간 안이지만 오늘 준비한 빵이 모두 품절된 상태를 보여주는 데모 매장입니다.';
        ELSEIF v_i <= 20 AND v_expected_status = 'CLOSED' THEN
            SET v_store_desc = '휴무 상태를 안정적으로 확인할 수 있도록 한성대 5km 안에 배치한 데모 매장입니다.';
        ELSEIF v_i <= 20 THEN
            SET v_store_desc = '한성대학교 근처 조회와 할인율 분포를 보여주는 데모 베이커리입니다.';
        ELSE
            SET v_store_desc = '서울 전역 탐색과 매장 목록 페이징을 확인하기 위한 데모 베이커리입니다.';
        END IF;

        SELECT id INTO v_boss_id
        FROM users
        WHERE email = CONCAT('demo-boss', v_i, '@todaybread.com');

        INSERT INTO store (
            user_id, name, phone_number, description,
            address_line1, address_line2, latitude, longitude,
            is_active, rating_sum, review_count
        )
        VALUES (
            v_boss_id,
            v_store_name,
            CONCAT('02-7000-', LPAD(v_i, 4, '0')),
            v_store_desc,
            CONCAT('서울특별시 ', v_district, ' 데모로 ', v_i),
            CONCAT(v_branch, ' ', LPAD(v_i, 3, '0'), '호'),
            v_lat,
            v_lng,
            TRUE,
            0,
            0
        );
        SET v_store_id = LAST_INSERT_ID();

        INSERT INTO tmp_store_specs (store_no, store_id, distance_bucket, expected_status, sales_pattern, menu_count)
        VALUES (
            v_i,
            v_store_id,
            IF(v_i <= 3, 'WITHIN_1KM', IF(v_i <= 10, 'WITHIN_3KM', IF(v_i <= 20, 'WITHIN_5KM', 'SEOUL'))),
            v_expected_status,
            v_sales_pattern,
            v_menu_count
        );

        SET v_store_image_idx = 1 + ((v_i - 1) MOD 10);
        SET v_store_ext = ELT(v_store_image_idx, '.png', '.webp', '.webp', '.jpeg', '.jpg', '.jpg', '.jpg', '.jpg', '.jpg', '.jpeg');
        INSERT INTO store_image (store_id, original_filename, stored_filename, display_order)
        VALUES (
            v_store_id,
            CONCAT('seed_store_', LPAD(v_store_image_idx, 2, '0'), v_store_ext),
            CONCAT('seed_store_', LPAD(v_store_image_idx, 2, '0'), '_store_', LPAD(v_i, 3, '0'), v_store_ext),
            0
        );

        SET v_j = 1;
        WHILE v_j <= 7 DO
            IF v_expected_status = 'CLOSED' THEN
                INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time)
                VALUES (v_store_id, v_j, TRUE, NULL, NULL, NULL);
            ELSEIF v_j BETWEEN 1 AND 5 THEN
                INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time)
                VALUES (v_store_id, v_j, FALSE, '09:00:00', '22:00:00', '22:00:00');
            ELSEIF v_j = 6 THEN
                INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time)
                VALUES (v_store_id, v_j, FALSE, '09:00:00', '21:00:00', '21:00:00');
            ELSE
                INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time)
                VALUES (v_store_id, v_j, TRUE, NULL, NULL, NULL);
            END IF;
            SET v_j = v_j + 1;
        END WHILE;

        SET v_j = 1;
        WHILE v_j <= v_menu_count DO
            SET v_source_idx = 1 + ((v_i * 5 + v_j * 7) MOD 48);
            SELECT name, description, original_price
            INTO v_bread_name, v_bread_desc, v_original_price
            FROM tmp_bread_catalog
            WHERE idx = v_source_idx;

            IF v_i <= 20 AND v_j = 1 THEN
                SET v_discount_percent = 10;
            ELSEIF v_i <= 20 AND v_j = 2 THEN
                SET v_discount_percent = 80;
            ELSE
                SET v_discount_percent = ELT(1 + ((v_i + v_j * 3) MOD 11), 10, 15, 20, 25, 30, 35, 40, 50, 60, 70, 80);
            END IF;

            SET v_sale_price = GREATEST(100, FLOOR((v_original_price * (100 - v_discount_percent)) / 100 / 100) * 100);

            IF v_expected_status = 'OPEN_SOLD_OUT' THEN
                SET v_quantity = 0;
            ELSEIF v_expected_status = 'SELLING' AND v_j <= 2 THEN
                SET v_quantity = v_j;
            ELSE
                SET v_quantity = 1 + ((v_i * v_j * 13) MOD 20);
            END IF;

            INSERT INTO bread (
                store_id, name, original_price, sale_price,
                remaining_quantity, description, is_deleted, deleted_at
            )
            VALUES (
                v_store_id,
                v_bread_name,
                v_original_price,
                v_sale_price,
                v_quantity,
                CONCAT(v_discount_percent, '% 할인. ', v_bread_desc),
                FALSE,
                NULL
            );
            SET v_bread_id = LAST_INSERT_ID();

            INSERT INTO tmp_seed_breads (store_no, menu_idx, bread_id, bread_name, sale_price, is_review_target)
            VALUES (v_i, v_j, v_bread_id, v_bread_name, v_sale_price, v_j <= v_target_menu_count);

            SET v_bread_image_idx = 1 + ((v_bread_image_sequence - 1) MOD 10);
            SET v_bread_ext = ELT(v_bread_image_idx, '.jpeg', '.jpg', '.webp', '.jpg', '.jpg', '.jpg', '.jpeg', '.jpg', '.jpg', '.jpeg');
            INSERT INTO bread_image (bread_id, original_filename, stored_filename)
            VALUES (
                v_bread_id,
                CONCAT('seed_bread_', LPAD(v_bread_image_idx, 2, '0'), v_bread_ext),
                CONCAT('seed_bread_', LPAD(v_bread_image_idx, 2, '0'), '_bread_', v_bread_id, v_bread_ext)
            );
            SET v_bread_image_sequence = v_bread_image_sequence + 1;

            SET v_j = v_j + 1;
        END WHILE;

        SET v_i = v_i + 1;
    END WHILE;

    INSERT INTO favourite_store (user_id, store_id)
    SELECT v_user_id, store_id
    FROM tmp_store_specs
    WHERE store_no <= 20 OR store_no IN (31, 42, 53, 64, 75, 86, 97, 108, 119);

    SELECT store_id INTO v_store_id
    FROM tmp_store_specs
    WHERE store_no = 1;

    INSERT INTO cart (user_id, store_id)
    VALUES (v_user_id, v_store_id);
    SET v_cart_id = LAST_INSERT_ID();

    INSERT INTO cart_item (cart_id, bread_id, quantity)
    SELECT v_cart_id, bread_id, 1
    FROM tmp_seed_breads
    WHERE store_no = 1 AND menu_idx IN (1, 2);

    SET v_i = 1;
    WHILE v_i <= 120 DO
        SELECT store_id, sales_pattern, menu_count
        INTO v_store_id, v_sales_pattern, v_menu_count
        FROM tmp_store_specs
        WHERE store_no = v_i;

        SET v_target_menu_count = IF(v_menu_count <= 5, 2, LEAST(3, GREATEST(2, CEIL(v_menu_count / 5))));
        SET v_pickup_seq = 0;
        SET v_month = 1;

        WHILE v_month <= 5 DO
            SET v_max_day = ELT(v_month, 31, 28, 31, 30, 7);

            IF v_sales_pattern = 1 THEN
                SET v_month_order_count = IF(v_month = 5, 5 + (v_i MOD 3), 10 + ((v_i + v_month) MOD 4));
            ELSEIF v_sales_pattern = 2 THEN
                SET v_month_order_count = IF(v_month = 5, 4 + (v_i MOD 3), 8 + ((v_i + v_month) MOD 5));
            ELSEIF v_sales_pattern = 3 THEN
                SET v_month_order_count = IF(v_month = 1, 4 + (v_i MOD 2),
                    IF(v_month = 2, 6 + (v_i MOD 2),
                    IF(v_month = 3, 9 + (v_i MOD 2),
                    IF(v_month = 4, 12 + (v_i MOD 3), 6 + (v_i MOD 2)))));
            ELSEIF v_sales_pattern = 4 THEN
                SET v_month_order_count = IF(v_month = 5, 2 + (v_i MOD 2), 3 + ((v_i + v_month) MOD 4));
            ELSE
                SET v_month_order_count = IF(v_month = 5, 6 + (v_i MOD 2), 12 + ((v_i + v_month) MOD 4));
            END IF;

            SET v_month_order_count = LEAST(15, v_month_order_count);
            IF v_month = 5 THEN
                SET v_month_order_count = LEAST(7, v_month_order_count);
            END IF;

            SET v_j = 1;
            WHILE v_j <= v_month_order_count DO
                SET v_day_no = 1 + ((v_i * 7 + v_j * 5 + v_month * 3) MOD v_max_day);
                SET v_order_date = STR_TO_DATE(CONCAT('2026-', LPAD(v_month, 2, '0'), '-', LPAD(v_day_no, 2, '0')), '%Y-%m-%d');
                SET v_created_at = TIMESTAMP(v_order_date, CAST(CONCAT(LPAD(9 + ((v_i + v_j) MOD 12), 2, '0'), ':', LPAD((v_j * 7) MOD 60, 2, '0'), ':00') AS TIME));

                IF v_sales_pattern = 5 THEN
                    SET v_status = IF(MOD(v_j, 3) = 0 OR MOD(v_i + v_j + v_month, 11) = 0, 'CANCELLED', 'PICKED_UP');
                ELSE
                    SET v_status = IF(MOD(v_i + v_j + v_month, 9) = 0, 'CANCELLED', 'PICKED_UP');
                END IF;

                SET v_menu_idx = 1 + ((v_i + v_j + v_month) MOD v_target_menu_count);
                SELECT bread_id, bread_name, sale_price
                INTO v_order_bread_id, v_order_bread_name, v_order_bread_price
                FROM tmp_seed_breads
                WHERE store_no = v_i AND menu_idx = v_menu_idx;

                SET v_qty = 1 + ((v_i + v_j + v_month) MOD 3);
                SET v_order_number = CONCAT(CHAR(65 + ((v_i + v_month) MOD 26)), LPAD(v_j, 2, '0'), CHAR(65 + (v_month MOD 26)));

                INSERT INTO orders (
                    user_id, store_id, status, total_amount,
                    idempotency_key, order_number, order_date,
                    created_at, updated_at
                )
                VALUES (
                    v_user_id,
                    v_store_id,
                    v_status,
                    v_order_bread_price * v_qty,
                    CONCAT('seed-order-', v_i, '-', v_month, '-', v_j),
                    v_order_number,
                    v_order_date,
                    v_created_at,
                    v_created_at
                );
                SET v_order_id = LAST_INSERT_ID();

                INSERT INTO order_item (
                    order_id, bread_id, bread_name, bread_price,
                    quantity, created_at, updated_at
                )
                VALUES (
                    v_order_id,
                    v_order_bread_id,
                    v_order_bread_name,
                    v_order_bread_price,
                    v_qty,
                    v_created_at,
                    v_created_at
                );
                SET v_order_item_id = LAST_INSERT_ID();

                IF v_status = 'PICKED_UP' THEN
                    SET v_pickup_seq = v_pickup_seq + 1;
                    INSERT INTO tmp_seed_order_items (store_no, pickup_seq, order_item_id, bread_id, created_at)
                    VALUES (v_i, v_pickup_seq, v_order_item_id, v_order_bread_id, v_created_at);
                    SET v_payment_status = 'APPROVED';
                ELSE
                    SET v_payment_status = 'CANCELLED';
                END IF;

                INSERT INTO payment (
                    order_id, amount, status, paid_at, idempotency_key,
                    payment_key, method, cancel_reason, cancelled_at,
                    created_at, updated_at
                )
                VALUES (
                    v_order_id,
                    v_order_bread_price * v_qty,
                    v_payment_status,
                    DATE_ADD(v_created_at, INTERVAL 5 SECOND),
                    CONCAT('seed-payment-', v_i, '-', v_month, '-', v_j),
                    CONCAT('seed_payment_', LPAD(v_global_order_seq, 6, '0')),
                    'CARD',
                    IF(v_status = 'CANCELLED', '사용자 요청 취소', NULL),
                    IF(v_status = 'CANCELLED', DATE_ADD(v_created_at, INTERVAL 20 MINUTE), NULL),
                    v_created_at,
                    IF(v_status = 'CANCELLED', DATE_ADD(v_created_at, INTERVAL 20 MINUTE), v_created_at)
                );

                SET v_global_order_seq = v_global_order_seq + 1;
                SET v_j = v_j + 1;
            END WHILE;

            SET v_month = v_month + 1;
        END WHILE;

        SET v_i = v_i + 1;
    END WHILE;

    CREATE TEMPORARY TABLE tmp_review_candidates AS
    SELECT
        toi.store_no,
        toi.order_item_id,
        toi.bread_id,
        toi.created_at,
        ROW_NUMBER() OVER (PARTITION BY toi.store_no ORDER BY toi.created_at, toi.order_item_id) AS review_seq
    FROM tmp_seed_order_items toi
    JOIN tmp_seed_breads tb ON tb.bread_id = toi.bread_id
    WHERE tb.is_review_target = TRUE;

    SET v_i = 1;
    WHILE v_i <= 120 DO
        SELECT store_id INTO v_store_id
        FROM tmp_store_specs
        WHERE store_no = v_i;

        SET v_review_idx = 1;
        WHILE v_review_idx <= 10 DO
            SELECT order_item_id, bread_id, created_at
            INTO v_order_item_id, v_bread_id, v_created_at
            FROM tmp_review_candidates
            WHERE store_no = v_i AND review_seq = v_review_idx;

            IF DATE_ADD(v_created_at, INTERVAL 1 DAY) > '2026-05-07 22:00:00' THEN
                SET v_review_created_at = DATE_ADD(v_created_at, INTERVAL 1 HOUR);
            ELSE
                SET v_review_created_at = DATE_ADD(v_created_at, INTERVAL 1 DAY);
            END IF;

            IF v_i = 1 AND v_review_idx = 1 THEN
                SET v_rating = 1;
                SET v_content = '한성대 근처 데모용 나쁜 리뷰입니다. 빵이 많이 말라서 다음에는 개선되면 좋겠습니다.';
            ELSE
                SET v_rating = 3 + ((v_i + v_review_idx) MOD 3);
                SELECT content INTO v_content
                FROM tmp_review_contents
                WHERE idx = 1 + ((v_i * 3 + v_review_idx) MOD 12);
            END IF;

            INSERT INTO review (
                user_id, store_id, bread_id, order_item_id,
                rating, content, created_at, updated_at
            )
            VALUES (
                v_user_id,
                v_store_id,
                v_bread_id,
                v_order_item_id,
                v_rating,
                v_content,
                v_review_created_at,
                v_review_created_at
            );
            SET v_review_id = LAST_INSERT_ID();

            IF v_review_idx <= 5 THEN
                SET v_review_image_count = 1 + ((v_i + v_review_idx) MOD 2);
                SET v_k = 1;
                WHILE v_k <= v_review_image_count DO
                    SET v_review_image_idx = 1 + ((v_review_image_sequence - 1) MOD 5);
                    SET v_review_ext = ELT(v_review_image_idx, '.jpeg', '.jpeg', '.jpg', '.jpeg', '.jpg');
                    INSERT INTO review_image (review_id, original_filename, stored_filename, created_at, updated_at)
                    VALUES (
                        v_review_id,
                        CONCAT('seed_review_', LPAD(v_review_image_idx, 2, '0'), v_review_ext),
                        CONCAT('seed_review_', LPAD(v_review_image_idx, 2, '0'), '_review_', v_review_id, '_', v_k, v_review_ext),
                        v_review_created_at,
                        v_review_created_at
                    );
                    SET v_review_image_sequence = v_review_image_sequence + 1;
                    SET v_k = v_k + 1;
                END WHILE;
            END IF;

            SET v_review_idx = v_review_idx + 1;
        END WHILE;

        SET v_i = v_i + 1;
    END WHILE;

    UPDATE store s
    JOIN tmp_store_specs tss ON tss.store_id = s.id
    LEFT JOIN (
        SELECT store_id, COALESCE(SUM(rating), 0) AS rating_sum, COUNT(*) AS review_count
        FROM review
        GROUP BY store_id
    ) r ON r.store_id = s.id
    SET s.rating_sum = COALESCE(r.rating_sum, 0),
        s.review_count = COALESCE(r.review_count, 0);

    SELECT 'seed_users' AS metric, COUNT(*) AS value
    FROM users
    WHERE email = 'demo-user01@todaybread.com'
       OR email LIKE 'demo-boss%@todaybread.com';

    SELECT 'seed_stores' AS metric, COUNT(*) AS value
    FROM tmp_store_specs;

    SELECT 'hansung_radius_1km' AS metric, COUNT(*) AS value
    FROM store s
    JOIN tmp_store_specs tss ON tss.store_id = s.id
    WHERE 6371 * 2 * ASIN(SQRT(
        POWER(SIN(RADIANS(s.latitude - 37.5826000) / 2), 2) +
        COS(RADIANS(37.5826000)) * COS(RADIANS(s.latitude)) *
        POWER(SIN(RADIANS(s.longitude - 127.0106000) / 2), 2)
    )) <= 1;

    SELECT 'hansung_radius_3km' AS metric, COUNT(*) AS value
    FROM store s
    JOIN tmp_store_specs tss ON tss.store_id = s.id
    WHERE 6371 * 2 * ASIN(SQRT(
        POWER(SIN(RADIANS(s.latitude - 37.5826000) / 2), 2) +
        COS(RADIANS(37.5826000)) * COS(RADIANS(s.latitude)) *
        POWER(SIN(RADIANS(s.longitude - 127.0106000) / 2), 2)
    )) <= 3;

    SELECT 'hansung_radius_5km' AS metric, COUNT(*) AS value
    FROM store s
    JOIN tmp_store_specs tss ON tss.store_id = s.id
    WHERE 6371 * 2 * ASIN(SQRT(
        POWER(SIN(RADIANS(s.latitude - 37.5826000) / 2), 2) +
        COS(RADIANS(37.5826000)) * COS(RADIANS(s.latitude)) *
        POWER(SIN(RADIANS(s.longitude - 127.0106000) / 2), 2)
    )) <= 5;

    SELECT distance_bucket, expected_status, COUNT(*) AS store_count
    FROM tmp_store_specs
    GROUP BY distance_bucket, expected_status
    ORDER BY distance_bucket, expected_status;

    SELECT status, COUNT(*) AS order_count
    FROM orders o
    JOIN tmp_store_specs tss ON tss.store_id = o.store_id
    GROUP BY status
    ORDER BY status;

    SELECT DATE_FORMAT(order_date, '%Y-%m') AS sales_month, MAX(monthly_count) AS max_orders_per_store
    FROM (
        SELECT store_id, DATE_FORMAT(order_date, '%Y-%m') AS order_month, COUNT(*) AS monthly_count, MIN(order_date) AS order_date
        FROM orders
        WHERE store_id IN (SELECT store_id FROM tmp_store_specs)
        GROUP BY store_id, DATE_FORMAT(order_date, '%Y-%m')
    ) m
    GROUP BY DATE_FORMAT(order_date, '%Y-%m')
    ORDER BY sales_month;

    SELECT 'review_image_reviews' AS metric, COUNT(DISTINCT review_id) AS value
    FROM review_image ri
    JOIN review r ON r.id = ri.review_id
    JOIN tmp_store_specs tss ON tss.store_id = r.store_id;

    DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_users;
    DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_stores;
    DROP TEMPORARY TABLE IF EXISTS tmp_district_centers;
    DROP TEMPORARY TABLE IF EXISTS tmp_store_specs;
    DROP TEMPORARY TABLE IF EXISTS tmp_seed_breads;
    DROP TEMPORARY TABLE IF EXISTS tmp_seed_order_items;
    DROP TEMPORARY TABLE IF EXISTS tmp_review_candidates;
    DROP TEMPORARY TABLE IF EXISTS tmp_bread_catalog;
    DROP TEMPORARY TABLE IF EXISTS tmp_review_contents;
END //
DELIMITER ;

START TRANSACTION;
CALL seed_demo_data();
COMMIT;
DROP PROCEDURE IF EXISTS seed_demo_data;
