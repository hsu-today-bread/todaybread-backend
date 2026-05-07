SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET collation_connection = 'utf8mb4_unicode_ci';

/*
 개발용 테스트 데이터 스크립트 (v4)

 기본 실행으로 서울 전역 100개 매장을 생성합니다.

 샘플 로그인 계정
 - 일반 유저:  demo-user@todaybread.com / todaybread123
 - 사장님 1~100: demo-boss1@todaybread.com ~ demo-boss100@todaybread.com / todaybread123

 근처 매장/빵 조회 기본 좌표
 - 한성대학교 기준
 - lat=37.5826000
 - lng=127.0106000
 - radius=3 / 5 / 10

 데이터 규칙
 - 한성대 3km 이내 15개, 5km 이내 20개 이상, 10km 이내 30개 이상
 - 한성대 3km 이내 15개는 영업 제한 10개 + 전체 품절 5개
 - 주문은 PICKED_UP 또는 CANCELLED만 생성합니다. 픽업 대기(CONFIRMED)는 생성하지 않습니다.
 - 4월 주문은 매장당 10개, 5월 주문은 매장당 3개 이하로 생성합니다.
 - 리뷰는 매장당 10개이며, 이미지 리뷰 5개 + 텍스트 리뷰 5개입니다.
 - 리뷰 10개 중 3개는 soft-deleted 빵에 대한 리뷰입니다.
 */

SET @pw = '$argon2id$v=19$m=16384,t=2,p=1$q74E5AXcCNxyfKx3iCCtEQ$12t7qic0oWGPI8R9E3T8uCO+q0sP+fsK1paO8Hc3hXY';

DROP PROCEDURE IF EXISTS seed_demo_data;

DELIMITER //
CREATE PROCEDURE seed_demo_data()
BEGIN
    DECLARE v_i INT DEFAULT 1;
    DECLARE v_j INT DEFAULT 1;
    DECLARE v_order_idx INT DEFAULT 1;
    DECLARE v_review_idx INT DEFAULT 1;
    DECLARE v_store_id BIGINT;
    DECLARE v_boss_id BIGINT;
    DECLARE v_user_id BIGINT;
    DECLARE v_cart_id BIGINT;
    DECLARE v_menu_count INT;
    DECLARE v_bread_id BIGINT;
    DECLARE v_bread_name VARCHAR(100);
    DECLARE v_bread_desc VARCHAR(255);
    DECLARE v_original_price INT;
    DECLARE v_sale_price INT;
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
    DECLARE v_suffix VARCHAR(30);
    DECLARE v_store_image_idx INT;
    DECLARE v_store_ext VARCHAR(10);
    DECLARE v_bread_image_idx INT;
    DECLARE v_bread_ext VARCHAR(10);
    DECLARE v_review_image_idx INT;
    DECLARE v_review_ext VARCHAR(10);
    DECLARE v_status VARCHAR(20);
    DECLARE v_order_id BIGINT;
    DECLARE v_order_item_id BIGINT;
    DECLARE v_order_date DATE;
    DECLARE v_created_at DATETIME(6);
    DECLARE v_order_number VARCHAR(4);
    DECLARE v_order_bread_id BIGINT;
    DECLARE v_order_bread_name VARCHAR(100);
    DECLARE v_order_bread_price INT;
    DECLARE v_order_bread_deleted BOOLEAN;
    DECLARE v_qty INT;
    DECLARE v_rating INT;
    DECLARE v_content VARCHAR(500);
    DECLARE v_review_id BIGINT;
    DECLARE v_review_created_at DATETIME(6);
    DECLARE v_source_idx INT;

    DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_users;
    DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_stores;
    DROP TEMPORARY TABLE IF EXISTS tmp_store_specs;
    DROP TEMPORARY TABLE IF EXISTS tmp_seed_breads;
    DROP TEMPORARY TABLE IF EXISTS tmp_seed_order_items;
    DROP TEMPORARY TABLE IF EXISTS tmp_bread_catalog;
    DROP TEMPORARY TABLE IF EXISTS tmp_review_contents;

    CREATE TEMPORARY TABLE tmp_cleanup_users (id BIGINT PRIMARY KEY);
    CREATE TEMPORARY TABLE tmp_cleanup_stores (id BIGINT PRIMARY KEY);

    INSERT INTO tmp_cleanup_users (id)
    SELECT id
    FROM users
    WHERE email = 'demo-user@todaybread.com'
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

    CREATE TEMPORARY TABLE tmp_store_specs (
        store_no INT PRIMARY KEY,
        store_id BIGINT NOT NULL,
        menu_count INT NOT NULL,
        is_hansung_near BOOLEAN NOT NULL,
        is_limited BOOLEAN NOT NULL,
        is_soldout BOOLEAN NOT NULL
    );

    CREATE TEMPORARY TABLE tmp_seed_breads (
        store_no INT NOT NULL,
        menu_idx INT NOT NULL,
        bread_id BIGINT NOT NULL,
        bread_name VARCHAR(100) NOT NULL,
        sale_price INT NOT NULL,
        is_deleted BOOLEAN NOT NULL,
        PRIMARY KEY (store_no, menu_idx),
        INDEX idx_tmp_seed_breads_store_deleted (store_no, is_deleted)
    );

    CREATE TEMPORARY TABLE tmp_seed_order_items (
        store_no INT NOT NULL,
        order_idx INT NOT NULL,
        order_item_id BIGINT NOT NULL,
        bread_id BIGINT NOT NULL,
        is_deleted BOOLEAN NOT NULL,
        PRIMARY KEY (store_no, order_idx)
    );

    CREATE TEMPORARY TABLE tmp_bread_catalog (
        idx INT PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        description VARCHAR(255) NOT NULL,
        original_price INT NOT NULL
    );

    INSERT INTO tmp_bread_catalog (idx, name, description, original_price) VALUES
        (1, '소금빵', '버터 향과 짭짤한 맛이 살아 있는 대표 메뉴입니다.', 3600),
        (2, '클래식 크루아상', '겹겹이 바삭한 버터 크루아상입니다.', 4600),
        (3, '통밀 캄파뉴', '천연 발효종으로 묵직하게 구운 식사빵입니다.', 7600),
        (4, '우유 식빵', '부드러운 결이 살아 있는 데일리 식빵입니다.', 5200),
        (5, '앙버터 바게트', '팥앙금과 버터를 채운 달콤한 바게트입니다.', 5800),
        (6, '올리브 치아바타', '올리브와 허브 향이 좋은 담백한 치아바타입니다.', 5400),
        (7, '크림치즈 베이글', '쫄깃한 베이글에 크림치즈를 듬뿍 넣었습니다.', 4300),
        (8, '갈릭 바게트', '마늘 버터를 바른 바삭한 바게트입니다.', 5000),
        (9, '초코 브리오슈', '초콜릿과 버터 풍미가 진한 브리오슈입니다.', 4800),
        (10, '블루베리 스콘', '블루베리가 톡톡 터지는 영국식 스콘입니다.', 3900),
        (11, '사워도우 부울', '48시간 발효해 산미가 깊은 사워도우입니다.', 8500),
        (12, '호밀빵', '고소한 호밀 향이 진한 건강빵입니다.', 6200),
        (13, '라즈베리 데니쉬', '새콤한 라즈베리잼을 올린 데니쉬입니다.', 4900),
        (14, '피칸 퀸아망', '카라멜과 피칸이 어울리는 바삭한 퀸아망입니다.', 5600),
        (15, '흑임자 식빵', '흑임자의 고소함을 살린 식빵입니다.', 5500),
        (16, '명란 바게트', '짭조름한 명란 크림을 바른 바게트입니다.', 5400),
        (17, '바질 포카치아', '바질과 토마토를 올린 촉촉한 포카치아입니다.', 5800),
        (18, '단팥빵', '직접 끓인 팥앙금이 들어간 전통 단팥빵입니다.', 3200),
        (19, '시나몬 롤', '시나몬 향과 글레이즈가 어울리는 롤빵입니다.', 4800),
        (20, '레몬 파운드', '상큼한 레몬 글레이즈를 올린 파운드케이크입니다.', 5000),
        (21, '프레첼', '짭짤하고 쫄깃한 독일식 프레첼입니다.', 3800),
        (22, '밤식빵', '달콤한 밤을 듬뿍 넣은 식빵입니다.', 6800),
        (23, '마늘 크림치즈빵', '마늘과 크림치즈가 조화로운 인기 메뉴입니다.', 4400),
        (24, '모카빵', '커피 향이 진한 부드러운 모카빵입니다.', 3500),
        (25, '고구마 식빵', '달콤한 고구마 무스를 넣은 식빵입니다.', 5600),
        (26, '치즈 바게트', '고소한 치즈가 녹아든 바게트입니다.', 5200),
        (27, '카라멜 소금빵', '달콤한 카라멜과 짭짤한 버터가 만난 메뉴입니다.', 3900),
        (28, '얼그레이 파운드', '얼그레이 향이 은은한 파운드케이크입니다.', 5200),
        (29, '통밀 베이글', '통밀로 구워 고소하고 쫄깃한 베이글입니다.', 4200),
        (30, '호두 무화과빵', '무화과와 호두를 듬뿍 넣은 식사빵입니다.', 6800),
        (31, '에그 타르트', '바삭한 페이스트리와 커스터드가 어울립니다.', 3800),
        (32, '카레빵', '매콤한 카레를 채운 든든한 튀김빵입니다.', 3800),
        (33, '마카다미아 쿠키빵', '마카다미아를 넣어 고소한 쿠키빵입니다.', 4200),
        (34, '쑥 단팥빵', '쑥 향과 팥앙금이 어우러진 한국식 빵입니다.', 3800),
        (35, '플레인 치아바타', '샌드위치로 먹기 좋은 쫄깃한 치아바타입니다.', 5000),
        (36, '초코 바나나 머핀', '초코와 바나나가 들어간 촉촉한 머핀입니다.', 4000),
        (37, '레몬 크림빵', '상큼한 레몬 크림을 넣은 부드러운 빵입니다.', 4100),
        (38, '아몬드 크루아상', '아몬드 크림과 슬라이스가 올라간 크루아상입니다.', 5200),
        (39, '귀리 쿠키', '귀리와 건포도가 들어간 건강 쿠키입니다.', 3200),
        (40, '트러플 포카치아', '트러플 오일을 더한 프리미엄 포카치아입니다.', 8000);

    CREATE TEMPORARY TABLE tmp_review_contents (
        idx INT PRIMARY KEY,
        content VARCHAR(500) NOT NULL
    );

    INSERT INTO tmp_review_contents (idx, content) VALUES
        (1, '기대보다 아쉬웠어요. 식감이 조금 말라서 다음에는 개선되면 좋겠습니다.'),
        (2, '갓 구운 빵 향이 좋고 포장도 깔끔해서 만족했습니다.'),
        (3, '가격 대비 양과 맛이 괜찮아서 친구에게도 추천하고 싶어요.'),
        (4, '버터 풍미가 진하고 식감이 좋아서 다음에도 들를 예정입니다.'),
        (5, '근처에서 간단히 사 가기 좋은 빵집입니다. 직원분도 친절했어요.'),
        (6, '재고가 넉넉하고 메뉴 구성이 다양해서 고르는 재미가 있었습니다.'),
        (7, '할인된 가격으로 먹기에는 충분히 만족스러운 품질이었습니다.'),
        (8, '예전에 먹었던 메뉴인데 다시 생각날 만큼 맛이 좋았습니다.'),
        (9, '단종된 메뉴라 아쉽지만 당시에는 풍미가 좋아 기억에 남았습니다.'),
        (10, '추억의 메뉴를 다시 리뷰할 수 있어서 좋았습니다. 재출시되면 좋겠어요.');

    INSERT INTO users (email, name, password_hash, nickname, phone_number, is_boss)
    VALUES ('demo-user@todaybread.com', '데모 유저', @pw, 'demo-user', '010-9000-0001', FALSE);
    SET v_user_id = LAST_INSERT_ID();

    SET v_i = 1;
    WHILE v_i <= 100 DO
        INSERT INTO users (email, name, password_hash, nickname, phone_number, is_boss)
        VALUES (
            CONCAT('demo-boss', v_i, '@todaybread.com'),
            CONCAT('사장님 ', LPAD(v_i, 3, '0')),
            @pw,
            CONCAT('demo-boss', v_i),
            CONCAT('010-9100-', LPAD(v_i, 4, '0')),
            TRUE
        );
        SET v_i = v_i + 1;
    END WHILE;

    INSERT IGNORE INTO keyword (normalised_text) VALUES ('크루아상'), ('사워도우'), ('베이글');

    INSERT INTO user_keyword (user_id, keyword_id, display_text)
    SELECT v_user_id, k.id, k.normalised_text
    FROM keyword k
    WHERE k.normalised_text IN ('크루아상', '사워도우', '베이글');

    SET v_i = 1;
    WHILE v_i <= 100 DO
        SET v_district = '성북구';
        SET v_lat = 37.5826000;
        SET v_lng = 127.0106000;

        IF v_i <= 15 THEN
            SET v_lat = 37.5826000 + ((((v_i - 1) MOD 5) - 2) * 0.0047) + ((FLOOR((v_i - 1) / 5) - 1) * 0.0031);
            SET v_lng = 127.0106000 + ((((v_i * 2) MOD 5) - 2) * 0.0052);
            SET v_district = '성북구';
        ELSEIF v_i <= 20 THEN
            SET v_offset_idx = v_i - 15;
            SET v_lat = 37.5826000 + CAST(ELT(v_offset_idx, '0.0320', '-0.0310', '0.0200', '-0.0180', '0.0350') AS DECIMAL(10,7));
            SET v_lng = 127.0106000 + CAST(ELT(v_offset_idx, '0.0100', '-0.0120', '0.0340', '0.0380', '-0.0180') AS DECIMAL(10,7));
            SET v_district = ELT(v_offset_idx, '종로구', '동대문구', '성북구', '중구', '성동구');
        ELSEIF v_i <= 35 THEN
            SET v_offset_idx = v_i - 20;
            SET v_lat = 37.5826000 + CAST(ELT(v_offset_idx,
                '0.0550', '-0.0520', '0.0700', '-0.0650', '0.0300',
                '-0.0300', '0.0800', '-0.0800', '0.0450', '-0.0450',
                '0.0150', '-0.0150', '0.0600', '-0.0600', '0.0750'
            ) AS DECIMAL(10,7));
            SET v_lng = 127.0106000 + CAST(ELT(v_offset_idx,
                '0.0200', '-0.0200', '-0.0400', '0.0380', '0.0750',
                '-0.0780', '0.0050', '-0.0060', '-0.0620', '0.0640',
                '0.0900', '-0.0920', '0.0500', '-0.0500', '0.0300'
            ) AS DECIMAL(10,7));
            SET v_district = ELT(v_offset_idx,
                '강북구', '종로구', '도봉구', '중구', '동대문구',
                '서대문구', '노원구', '용산구', '마포구', '광진구',
                '성동구', '은평구', '중랑구', '서초구', '강북구'
            );
        ELSE
            SET v_district_idx = ((v_i - 36) MOD 25) + 1;
            SET v_district = ELT(v_district_idx,
                '종로구', '중구', '용산구', '성동구', '광진구',
                '동대문구', '중랑구', '성북구', '강북구', '도봉구',
                '노원구', '은평구', '서대문구', '마포구', '양천구',
                '강서구', '구로구', '금천구', '영등포구', '동작구',
                '관악구', '서초구', '강남구', '송파구', '강동구'
            );
            SET v_center_lat = CAST(ELT(v_district_idx,
                '37.5735', '37.5636', '37.5326', '37.5633', '37.5384',
                '37.5744', '37.6063', '37.5894', '37.6396', '37.6688',
                '37.6542', '37.6176', '37.5791', '37.5663', '37.5169',
                '37.5509', '37.4955', '37.4569', '37.5264', '37.5124',
                '37.4784', '37.4837', '37.5172', '37.5145', '37.5301'
            ) AS DECIMAL(10,7));
            SET v_center_lng = CAST(ELT(v_district_idx,
                '126.9788', '126.9976', '126.9900', '127.0368', '127.0823',
                '127.0396', '127.0927', '127.0167', '127.0257', '127.0471',
                '127.0568', '126.9227', '126.9368', '126.9015', '126.8665',
                '126.8495', '126.8877', '126.8955', '126.8963', '126.9393',
                '126.9516', '127.0324', '127.0473', '127.1059', '127.1238'
            ) AS DECIMAL(10,7));
            SET v_lat = v_center_lat + (((v_i MOD 5) - 2) * 0.0021);
            SET v_lng = v_center_lng + ((((v_i + 2) MOD 5) - 2) * 0.0023);
        END IF;

        SET v_prefix = ELT(((v_i * 7) MOD 20) + 1,
            '오븐', '버터', '구름', '새벽', '밀밭',
            '크러스트', '달콤', '담백', '소담', '호밀',
            '브리오', '베이크', '소금', '문', '하루',
            '아침', '리틀', '플랜트', '골목', '따숨'
        );
        SET v_suffix = ELT(((v_i * 11) MOD 16) + 1,
            '베이커리', '브레드', '빵공방', '베이크샵',
            '크루아상', '사워도우', '오븐하우스', '브레드랩',
            '파티스리', '식빵연구소', '소금빵집', '브레드마켓',
            '빵집', '도우룸', '베이크랩', '빵상점'
        );

        IF v_i <= 10 THEN
            SET v_store_name = CONCAT('한성 야간 ', v_prefix, ' ', v_suffix);
            SET v_store_desc = '한성대학교 근처에서 저녁 시간대 위주로 운영하는 데모 베이커리입니다.';
        ELSEIF v_i <= 15 THEN
            SET v_store_name = CONCAT('한성 매진 ', v_prefix, ' ', v_suffix);
            SET v_store_desc = '영업 중이지만 오늘 준비한 빵이 모두 품절된 상태를 보여주는 데모 매장입니다.';
        ELSEIF v_i <= 35 THEN
            SET v_store_name = CONCAT('한성 근처 ', v_prefix, ' ', v_suffix);
            SET v_store_desc = '한성대학교 기준 근거리 탐색과 메뉴 다양성을 보여주는 데모 베이커리입니다.';
        ELSE
            SET v_store_name = CONCAT(v_district, ' ', v_prefix, ' ', v_suffix);
            SET v_store_desc = '서울 전역 탐색과 페이징 확인을 위한 데모 베이커리입니다.';
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
            CONCAT('02-91', LPAD(v_i, 6, '0')),
            v_store_desc,
            CONCAT('서울특별시 ', v_district, ' 데모로 ', v_i),
            CONCAT(LPAD(v_i, 3, '0'), '호'),
            v_lat,
            v_lng,
            TRUE,
            0,
            0
        );
        SET v_store_id = LAST_INSERT_ID();

        SET v_menu_count = IF(v_i <= 35, 5 + ((v_i * 7) MOD 6), 3);

        INSERT INTO tmp_store_specs (store_no, store_id, menu_count, is_hansung_near, is_limited, is_soldout)
        VALUES (v_i, v_store_id, v_menu_count, v_i <= 35, v_i <= 10, v_i BETWEEN 11 AND 15);

        SET v_store_image_idx = 1 + ((v_i * 7) MOD 10);
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
            IF v_i <= 10 AND v_j NOT IN (1, 3, 5) THEN
                INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time)
                VALUES (v_store_id, v_j, TRUE, NULL, NULL, NULL);
            ELSEIF v_i <= 10 THEN
                INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time)
                VALUES (v_store_id, v_j, FALSE, '18:00:00', '22:00:00', '21:30:00');
            ELSE
                INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time)
                VALUES (v_store_id, v_j, FALSE, '00:00:00', '23:59:00', '23:58:00');
            END IF;
            SET v_j = v_j + 1;
        END WHILE;

        SET v_j = 1;
        WHILE v_j <= v_menu_count DO
            SET v_source_idx = 1 + ((v_i * 5 + v_j * 7) MOD 40);
            SELECT name, description, original_price
            INTO v_bread_name, v_bread_desc, v_original_price
            FROM tmp_bread_catalog
            WHERE idx = v_source_idx;

            IF v_i <= 15 AND v_j = 1 THEN
                SET v_sale_price = GREATEST(1000, FLOOR((v_original_price * 0.50) / 100) * 100);
            ELSEIF v_i <= 15 AND v_j = 2 THEN
                SET v_sale_price = GREATEST(1000, FLOOR((v_original_price * 0.70) / 100) * 100);
            ELSEIF v_i <= 15 AND v_j = 3 THEN
                SET v_sale_price = GREATEST(1000, FLOOR((v_original_price * 0.90) / 100) * 100);
            ELSE
                SET v_sale_price = GREATEST(1000, v_original_price - (500 + ((v_i * v_j * 137) MOD 1500)));
            END IF;

            IF v_i BETWEEN 11 AND 15 THEN
                SET v_quantity = 0;
            ELSEIF v_i <= 15 AND v_j <= 2 THEN
                SET v_quantity = v_j;
            ELSE
                SET v_quantity = 1 + ((v_i * v_j * 13) MOD 20);
            END IF;

            INSERT INTO bread (
                store_id, name, original_price, sale_price,
                remaining_quantity, description, is_deleted, deleted_at
            )
            VALUES (
                v_store_id, v_bread_name, v_original_price, v_sale_price,
                v_quantity, v_bread_desc, FALSE, NULL
            );
            SET v_bread_id = LAST_INSERT_ID();

            INSERT INTO tmp_seed_breads (store_no, menu_idx, bread_id, bread_name, sale_price, is_deleted)
            VALUES (v_i, v_j, v_bread_id, v_bread_name, v_sale_price, FALSE);

            SET v_bread_image_idx = 1 + ((v_i * 3 + v_j * 5) MOD 10);
            SET v_bread_ext = ELT(v_bread_image_idx, '.jpeg', '.jpg', '.webp', '.jpg', '.jpg', '.jpg', '.jpeg', '.jpg', '.jpg', '.jpeg');
            INSERT INTO bread_image (bread_id, original_filename, stored_filename)
            VALUES (
                v_bread_id,
                CONCAT('seed_bread_', LPAD(v_bread_image_idx, 2, '0'), v_bread_ext),
                CONCAT('seed_bread_', LPAD(v_bread_image_idx, 2, '0'), '_bread_', v_bread_id, v_bread_ext)
            );

            SET v_j = v_j + 1;
        END WHILE;

        SET v_j = 1;
        WHILE v_j <= 3 DO
            SET v_source_idx = 1 + ((v_i * 11 + v_j * 3) MOD 40);
            SELECT name, description, original_price
            INTO v_bread_name, v_bread_desc, v_original_price
            FROM tmp_bread_catalog
            WHERE idx = v_source_idx;

            SET v_sale_price = GREATEST(1000, v_original_price - 800);

            INSERT INTO bread (
                store_id, name, original_price, sale_price,
                remaining_quantity, description, is_deleted, deleted_at,
                created_at, updated_at
            )
            VALUES (
                v_store_id,
                CONCAT('단종 ', v_bread_name),
                v_original_price,
                v_sale_price,
                0,
                CONCAT('과거 판매 후 현재는 soft-delete된 리뷰용 메뉴입니다. ', v_bread_desc),
                TRUE,
                '2026-05-06 12:00:00',
                '2026-03-20 09:00:00',
                '2026-05-06 12:00:00'
            );
            SET v_bread_id = LAST_INSERT_ID();

            INSERT INTO tmp_seed_breads (store_no, menu_idx, bread_id, bread_name, sale_price, is_deleted)
            VALUES (v_i, v_menu_count + v_j, v_bread_id, CONCAT('단종 ', v_bread_name), v_sale_price, TRUE);

            SET v_bread_image_idx = 1 + ((v_i * 2 + v_j * 7) MOD 10);
            SET v_bread_ext = ELT(v_bread_image_idx, '.jpeg', '.jpg', '.webp', '.jpg', '.jpg', '.jpg', '.jpeg', '.jpg', '.jpg', '.jpeg');
            INSERT INTO bread_image (bread_id, original_filename, stored_filename)
            VALUES (
                v_bread_id,
                CONCAT('seed_bread_', LPAD(v_bread_image_idx, 2, '0'), v_bread_ext),
                CONCAT('seed_bread_', LPAD(v_bread_image_idx, 2, '0'), '_bread_', v_bread_id, v_bread_ext)
            );

            SET v_j = v_j + 1;
        END WHILE;

        SET v_i = v_i + 1;
    END WHILE;

    INSERT INTO favourite_store (user_id, store_id)
    SELECT v_user_id, store_id
    FROM tmp_store_specs
    WHERE store_no IN (1, 11, 16, 21, 36);

    SELECT store_id INTO v_store_id FROM tmp_store_specs WHERE store_no = 16;
    INSERT INTO cart (user_id, store_id) VALUES (v_user_id, v_store_id);
    SET v_cart_id = LAST_INSERT_ID();

    INSERT INTO cart_item (cart_id, bread_id, quantity)
    SELECT v_cart_id, bread_id, CASE WHEN menu_idx = 1 THEN 1 ELSE 2 END
    FROM tmp_seed_breads
    WHERE store_no = 16 AND is_deleted = FALSE
    ORDER BY menu_idx
    LIMIT 2;

    SET v_i = 1;
    WHILE v_i <= 100 DO
        SELECT store_id, menu_count INTO v_store_id, v_menu_count
        FROM tmp_store_specs
        WHERE store_no = v_i;

        SET v_order_idx = 1;
        WHILE v_order_idx <= 13 DO
            IF v_order_idx IN (8, 10) THEN
                SET v_status = 'CANCELLED';
            ELSE
                SET v_status = 'PICKED_UP';
            END IF;

            IF v_order_idx <= 10 THEN
                SET v_order_date = DATE_ADD('2026-04-01', INTERVAL (((v_order_idx - 1) * 3 + v_i) MOD 28) DAY);
            ELSE
                SET v_order_date = DATE_ADD('2026-05-01', INTERVAL LEAST(6, ((v_order_idx - 11) * 2 + (v_i MOD 2))) DAY);
            END IF;

            SET v_created_at = TIMESTAMP(v_order_date, MAKETIME(10 + ((v_i + v_order_idx) MOD 9), ((v_i * v_order_idx) MOD 60), 0));
            SET v_order_number = CONCAT(CHAR(65 + (v_i MOD 26)), LPAD(v_order_idx, 3, '0'));

            IF v_order_idx >= 11 THEN
                SELECT bread_id, bread_name, sale_price, is_deleted
                INTO v_order_bread_id, v_order_bread_name, v_order_bread_price, v_order_bread_deleted
                FROM tmp_seed_breads
                WHERE store_no = v_i AND menu_idx = v_menu_count + (v_order_idx - 10);
            ELSE
                SELECT bread_id, bread_name, sale_price, is_deleted
                INTO v_order_bread_id, v_order_bread_name, v_order_bread_price, v_order_bread_deleted
                FROM tmp_seed_breads
                WHERE store_no = v_i AND menu_idx = 1 + ((v_order_idx + v_i) MOD v_menu_count);
            END IF;

            SET v_qty = 1 + ((v_i + v_order_idx) MOD 3);

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
                CONCAT('seed-order-', LPAD(v_i, 3, '0'), '-', LPAD(v_order_idx, 2, '0')),
                v_order_number,
                v_order_date,
                v_created_at,
                v_created_at
            );
            SET v_order_id = LAST_INSERT_ID();

            INSERT INTO order_item (
                order_id, bread_id, bread_name, bread_price, quantity,
                created_at, updated_at
            )
            VALUES (
                v_order_id, v_order_bread_id, v_order_bread_name,
                v_order_bread_price, v_qty, v_created_at, v_created_at
            );
            SET v_order_item_id = LAST_INSERT_ID();

            INSERT INTO tmp_seed_order_items (store_no, order_idx, order_item_id, bread_id, is_deleted)
            VALUES (v_i, v_order_idx, v_order_item_id, v_order_bread_id, v_order_bread_deleted);

            SET v_order_idx = v_order_idx + 1;
        END WHILE;

        SET v_i = v_i + 1;
    END WHILE;

    INSERT INTO payment (
        order_id, amount, status, paid_at,
        idempotency_key, payment_key, method,
        cancel_reason, cancelled_at,
        created_at, updated_at
    )
    SELECT
        o.id,
        o.total_amount,
        CASE WHEN o.status = 'CANCELLED' THEN 'CANCELLED' ELSE 'APPROVED' END,
        o.created_at,
        CONCAT('seed-pay-', o.id),
        CONCAT('seed_pay_', o.id),
        'STUB',
        CASE WHEN o.status = 'CANCELLED' THEN '테스트 취소' ELSE NULL END,
        CASE WHEN o.status = 'CANCELLED' THEN DATE_ADD(o.created_at, INTERVAL 30 MINUTE) ELSE NULL END,
        o.created_at,
        o.updated_at
    FROM orders o
    JOIN tmp_store_specs ss ON o.store_id = ss.store_id;

    SET v_i = 1;
    WHILE v_i <= 100 DO
        SELECT store_id INTO v_store_id
        FROM tmp_store_specs
        WHERE store_no = v_i;

        SET v_review_idx = 1;
        WHILE v_review_idx <= 10 DO
            IF v_review_idx <= 7 THEN
                SET v_order_idx = v_review_idx;
            ELSE
                SET v_order_idx = v_review_idx + 3;
            END IF;

            SELECT toi.order_item_id, toi.bread_id, o.created_at
            INTO v_order_item_id, v_bread_id, v_created_at
            FROM tmp_seed_order_items toi
            JOIN order_item oi ON toi.order_item_id = oi.id
            JOIN orders o ON oi.order_id = o.id
            WHERE toi.store_no = v_i AND toi.order_idx = v_order_idx;

            SET v_rating = CASE WHEN v_review_idx = 1 THEN 1 ELSE 3 + ((v_i + v_review_idx) MOD 3) END;

            SELECT content INTO v_content
            FROM tmp_review_contents
            WHERE idx = v_review_idx;

            SET v_review_created_at = DATE_ADD(v_created_at, INTERVAL 6 HOUR);

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
                SET v_review_image_idx = 1 + ((v_i + v_review_idx) MOD 5);
                SET v_review_ext = ELT(v_review_image_idx, '.jpeg', '.jpeg', '.jpg', '.jpeg', '.jpg');
                INSERT INTO review_image (review_id, original_filename, stored_filename, created_at, updated_at)
                VALUES (
                    v_review_id,
                    CONCAT('seed_review_', LPAD(v_review_image_idx, 2, '0'), v_review_ext),
                    CONCAT('seed_review_', LPAD(v_review_image_idx, 2, '0'), '_review_', v_review_id, '_1', v_review_ext),
                    v_review_created_at,
                    v_review_created_at
                );
            END IF;

            SET v_review_idx = v_review_idx + 1;
        END WHILE;

        SET v_i = v_i + 1;
    END WHILE;

    UPDATE store s
    JOIN tmp_store_specs ss ON s.id = ss.store_id
    SET
        s.rating_sum = (SELECT COALESCE(SUM(r.rating), 0) FROM review r WHERE r.store_id = s.id),
        s.review_count = (SELECT COUNT(*) FROM review r WHERE r.store_id = s.id);

    DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_users;
    DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_stores;
    DROP TEMPORARY TABLE IF EXISTS tmp_store_specs;
    DROP TEMPORARY TABLE IF EXISTS tmp_seed_breads;
    DROP TEMPORARY TABLE IF EXISTS tmp_seed_order_items;
    DROP TEMPORARY TABLE IF EXISTS tmp_bread_catalog;
    DROP TEMPORARY TABLE IF EXISTS tmp_review_contents;
END //
DELIMITER ;

START TRANSACTION;
CALL seed_demo_data();
COMMIT;

DROP PROCEDURE IF EXISTS seed_demo_data;
