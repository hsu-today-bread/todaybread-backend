SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET collation_connection = 'utf8mb4_unicode_ci';

/*
 개발용 테스트 데이터 스크립트 (v3)

 재실행 가능하도록, 아래 샘플 계정/매장에 해당하는 데이터만 먼저 정리한 뒤 다시 삽입합니다.

 샘플 로그인 계정
 - 일반 유저:  demo-user@todaybread.com / todaybread123
 - 사장님 1~20: demo-boss1@todaybread.com ~ demo-boss20@todaybread.com / todaybread123

 근처 매장/빵 조회 추천 좌표
 - 강남역 기준
 - lat=37.4980950
 - lng=127.0276100
 - radius=5
 - 한성대학교 기준
 - lat=37.5826000
 - lng=127.0106000
 - radius=2

 이미지
 - 빵 이미지 10장(seed_bread_01 ~ seed_bread_10)을 돌려서 사용
 - 매장 이미지 10장(seed_store_01 ~ seed_store_10)을 고유 파일명으로 복사해 돌려서 사용
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
SET @e_boss11 = 'demo-boss11@todaybread.com';
SET @e_boss12 = 'demo-boss12@todaybread.com';
SET @e_boss13 = 'demo-boss13@todaybread.com';
SET @e_boss14 = 'demo-boss14@todaybread.com';
SET @e_boss15 = 'demo-boss15@todaybread.com';
SET @e_boss16 = 'demo-boss16@todaybread.com';
SET @e_boss17 = 'demo-boss17@todaybread.com';
SET @e_boss18 = 'demo-boss18@todaybread.com';
SET @e_boss19 = 'demo-boss19@todaybread.com';
SET @e_boss20 = 'demo-boss20@todaybread.com';

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

/* 리뷰 이미지 삭제 */
DELETE ri FROM review_image ri
JOIN review r ON ri.review_id = r.id
JOIN store s  ON r.store_id   = s.id
JOIN users u  ON s.user_id    = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

/* 리뷰 삭제 */
DELETE r FROM review r
JOIN store s ON r.store_id = s.id
JOIN users u ON s.user_id  = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE fs FROM favourite_store fs
LEFT JOIN users u  ON fs.user_id  = u.id
LEFT JOIN store s  ON fs.store_id = s.id
LEFT JOIN users su ON s.user_id   = su.id
WHERE u.email IN (@e_user, @old_user)
   OR su.email IN (
       @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
       @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
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
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE p FROM payment p
JOIN orders o ON p.order_id = o.id
JOIN store s  ON o.store_id = s.id
JOIN users u  ON s.user_id  = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE o FROM orders o
JOIN store s ON o.store_id = s.id
JOIN users u ON s.user_id  = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE ci FROM cart_item ci
JOIN cart c ON ci.cart_id = c.id
LEFT JOIN users cu ON c.user_id = cu.id
LEFT JOIN store s  ON c.store_id = s.id
LEFT JOIN users su ON s.user_id  = su.id
WHERE cu.email IN (@e_user, @old_user,
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20)
   OR su.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE c FROM cart c
LEFT JOIN users cu ON c.user_id = cu.id
LEFT JOIN store s  ON c.store_id = s.id
LEFT JOIN users su ON s.user_id  = su.id
WHERE cu.email IN (@e_user, @old_user,
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20)
   OR su.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE bi FROM bread_image bi
JOIN bread b ON bi.bread_id = b.id
JOIN store s ON b.store_id  = s.id
JOIN users u ON s.user_id   = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE b FROM bread b
JOIN store s ON b.store_id = s.id
JOIN users u ON s.user_id  = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE si FROM store_image si
JOIN store s ON si.store_id = s.id
JOIN users u ON s.user_id   = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE sbh FROM store_business_hours sbh
JOIN store s ON sbh.store_id = s.id
JOIN users u ON s.user_id    = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE s FROM store s
JOIN users u ON s.user_id = u.id
WHERE u.email IN (
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE rt FROM refresh_token rt
JOIN users u ON rt.user_id = u.id
WHERE u.email IN (
    @e_user, @old_user,
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE prt FROM password_reset_token prt
JOIN users u ON prt.user_id = u.id
WHERE u.email IN (
    @e_user, @old_user,
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

DELETE FROM keyword WHERE normalised_text IN ('크루아상', '사워도우', '베이글');

DELETE FROM users WHERE email IN (
    @e_user, @old_user,
    @e_boss1,@e_boss2,@e_boss3,@e_boss4,@e_boss5,
    @e_boss6,@e_boss7,@e_boss8,@e_boss9,@e_boss10,
    @e_boss11,@e_boss12,@e_boss13,@e_boss14,@e_boss15,
    @e_boss16,@e_boss17,@e_boss18,@e_boss19,@e_boss20,
    @old_boss1,@old_boss2,@old_boss3,@old_boss4,@old_boss5
);

/* ============================================================
   유저 삽입 (일반 유저 1 + 사장님 20)
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
    (@e_boss10, '사장님 10',    @pw, 'demo-boss10', '010-9000-1010', TRUE),
    (@e_boss11, '사장님 11',    @pw, 'demo-boss11', '010-9000-1011', TRUE),
    (@e_boss12, '사장님 12',    @pw, 'demo-boss12', '010-9000-1012', TRUE),
    (@e_boss13, '사장님 13',    @pw, 'demo-boss13', '010-9000-1013', TRUE),
    (@e_boss14, '사장님 14',    @pw, 'demo-boss14', '010-9000-1014', TRUE),
    (@e_boss15, '사장님 15',    @pw, 'demo-boss15', '010-9000-1015', TRUE),
    (@e_boss16, '사장님 16',    @pw, 'demo-boss16', '010-9000-1016', TRUE),
    (@e_boss17, '사장님 17',    @pw, 'demo-boss17', '010-9000-1017', TRUE),
    (@e_boss18, '사장님 18',    @pw, 'demo-boss18', '010-9000-1018', TRUE),
    (@e_boss19, '사장님 19',    @pw, 'demo-boss19', '010-9000-1019', TRUE),
    (@e_boss20, '사장님 20',    @pw, 'demo-boss20', '010-9000-1020', TRUE);

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
SET @b11  = (SELECT id FROM users WHERE email = @e_boss11);
SET @b12  = (SELECT id FROM users WHERE email = @e_boss12);
SET @b13  = (SELECT id FROM users WHERE email = @e_boss13);
SET @b14  = (SELECT id FROM users WHERE email = @e_boss14);
SET @b15  = (SELECT id FROM users WHERE email = @e_boss15);
SET @b16  = (SELECT id FROM users WHERE email = @e_boss16);
SET @b17  = (SELECT id FROM users WHERE email = @e_boss17);
SET @b18  = (SELECT id FROM users WHERE email = @e_boss18);
SET @b19  = (SELECT id FROM users WHERE email = @e_boss19);
SET @b20  = (SELECT id FROM users WHERE email = @e_boss20);

/* ============================================================
   키워드 (normalised_text 최대 10자 제약 준수)
   ============================================================ */

INSERT INTO keyword (normalised_text) VALUES
    ('크루아상'),
    ('사워도우'),
    ('베이글');

INSERT INTO user_keyword (user_id, keyword_id, display_text)
SELECT @uid, k.id, k.normalised_text
FROM keyword k
WHERE k.normalised_text IN ('크루아상','사워도우','베이글');

/* ============================================================
   매장 삽입 (20개)
   - 기존 10개: 강남역(37.4981, 127.0276) 반경 ~5km
   - 추가 5개: 한성대학교(37.5826, 127.0106) 인근
   - 추가 5개: 서울 전역 샘플
   rating_sum, review_count 는 리뷰 삽입 후 업데이트
   ============================================================ */

INSERT INTO store (user_id, name, phone_number, description, address_line1, address_line2, latitude, longitude, is_active, rating_sum, review_count) VALUES
    (@b1,  '르뺑드마리 강남점',   '02-9000-3001', '프랑스식 정통 빵을 굽는 강남역 근처 베이커리입니다.',       '서울특별시 강남구 테헤란로 123',   '1층',     37.4981000, 127.0276000, TRUE, 0, 0),
    (@b2,  '밀도 선릉점',         '02-9000-3002', '매일 아침 갓 구운 식빵과 샌드위치를 판매합니다.',           '서울특별시 강남구 선릉로 431',     '2층',     37.5045000, 127.0489000, TRUE, 0, 0),
    (@b3,  '나폴레옹과자점 역삼점','02-9000-3003', '40년 전통의 수제 과자와 빵을 만듭니다.',                   '서울특별시 강남구 역삼로 210',     '1층',     37.5000000, 127.0365000, TRUE, 0, 0),
    (@b4,  '오월의종 삼성점',      '02-9000-3004', '천연 발효종으로 만든 건강한 빵을 판매합니다.',              '서울특별시 강남구 삼성로 512',     '지하 1층', 37.5088000, 127.0630000, TRUE, 0, 0),
    (@b5,  '빵명장 대치점',        '02-9000-3005', '장인이 직접 구운 프리미엄 빵을 만나보세요.',               '서울특별시 강남구 대치로 85',      '2층',     37.4945000, 127.0580000, TRUE, 0, 0),
    (@b6,  '쿠헨 논현점',          '02-9000-3006', '독일식 호밀빵과 프레첼 전문 베이커리입니다.',              '서울특별시 강남구 논현로 654',     '1층',     37.5110000, 127.0230000, TRUE, 0, 0),
    (@b7,  '아티장베이커리 청담점', '02-9000-3007', '유기농 밀가루로 만든 수제 빵을 판매합니다.',               '서울특별시 강남구 청담동 12-3',    '1층',     37.5200000, 127.0470000, TRUE, 0, 0),
    (@b8,  '브레드랩 서초점',       '02-9000-3008', '실험적인 레시피로 새로운 빵을 연구합니다.',               '서울특별시 서초구 서초대로 321',   '3층',     37.4920000, 127.0100000, TRUE, 0, 0),
    (@b9,  '밀밭제과 잠실점',       '02-9000-3009', '동네 주민이 사랑하는 따뜻한 동네 빵집입니다.',            '서울특별시 송파구 올림픽로 240',   '1층',     37.5130000, 127.0850000, TRUE, 0, 0),
    (@b10, '뚜레쥬르 양재점',       '02-9000-3010', '매일 신선한 빵과 케이크를 만듭니다.',                    '서울특별시 서초구 양재대로 55',    '1층',     37.4840000, 127.0340000, TRUE, 0, 0),
    (@b11, '한성 베이커리 정문점',  '02-9000-3011', '한성대학교 정문 근처에서 매일 소량 생산하는 동네 빵집입니다.', '서울특별시 성북구 삼선교로16길 116', '1층', 37.5826000, 127.0106000, TRUE, 0, 0),
    (@b12, '삼선동 소금빵집',       '02-9000-3012', '삼선동 골목에서 버터 향 가득한 소금빵을 굽습니다.',       '서울특별시 성북구 삼선동2가 39',   '1층',     37.5834000, 127.0092000, TRUE, 0, 0),
    (@b13, '성북천 브레드하우스',   '02-9000-3013', '성북천 산책길에 들르기 좋은 작은 베이커리입니다.',       '서울특별시 성북구 동소문로 45',    '1층',     37.5809000, 127.0117000, TRUE, 0, 0),
    (@b14, '동소문 크루아상',       '02-9000-3014', '한성대입구역 인근에서 바삭한 크루아상을 선보입니다.',     '서울특별시 성북구 동소문로 22',    '2층',     37.5848000, 127.0081000, TRUE, 0, 0),
    (@b15, '혜화문 베이크샵',       '02-9000-3015', '혜화문과 성북동 사이에서 건강한 발효빵을 만듭니다.',     '서울특별시 성북구 성북로 18',      '1층',     37.5870000, 127.0068000, TRUE, 0, 0),
    (@b16, '연남 사워도우',         '02-9000-3016', '연남동 골목 감성의 천연 발효 사워도우 전문점입니다.',     '서울특별시 마포구 동교로 257',     '1층',     37.5662000, 126.9257000, TRUE, 0, 0),
    (@b17, '성수 버터랩',           '02-9000-3017', '성수동 공방 스타일의 버터 페이스트리 전문 베이커리입니다.', '서울특별시 성동구 연무장길 33',    '1층',     37.5446000, 127.0558000, TRUE, 0, 0),
    (@b18, '용산 한강로 베이커리',  '02-9000-3018', '용산역 인근에서 식사빵과 샌드위치 빵을 굽습니다.',       '서울특별시 용산구 한강대로 95',    '1층',     37.5299000, 126.9648000, TRUE, 0, 0),
    (@b19, '은평 북한산 빵공방',    '02-9000-3019', '북한산 자락의 여유로운 분위기를 담은 빵공방입니다.',      '서울특별시 은평구 진관동 65',      '1층',     37.6177000, 126.9227000, TRUE, 0, 0),
    (@b20, '여의도 브레드마켓',     '02-9000-3020', '여의도 직장인을 위한 식사빵과 디저트빵을 판매합니다.',    '서울특별시 영등포구 여의대로 108', '지하 1층', 37.5266000, 126.9244000, TRUE, 0, 0);

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
SET @s11 = (SELECT id FROM store WHERE user_id = @b11);
SET @s12 = (SELECT id FROM store WHERE user_id = @b12);
SET @s13 = (SELECT id FROM store WHERE user_id = @b13);
SET @s14 = (SELECT id FROM store WHERE user_id = @b14);
SET @s15 = (SELECT id FROM store WHERE user_id = @b15);
SET @s16 = (SELECT id FROM store WHERE user_id = @b16);
SET @s17 = (SELECT id FROM store WHERE user_id = @b17);
SET @s18 = (SELECT id FROM store WHERE user_id = @b18);
SET @s19 = (SELECT id FROM store WHERE user_id = @b19);
SET @s20 = (SELECT id FROM store WHERE user_id = @b20);

/* ============================================================
   매장 이미지
   기존 seed_store_01~10 원본을 고유 stored_filename으로 돌려서 사용
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
    (@s10, 'seed-store-10.svg', 'seed_store_10.svg', 0),
    (@s11, 'seed-store-03.svg', 'seed_store_03_hansung_01.svg', 0),
    (@s12, 'seed-store-07.svg', 'seed_store_07_hansung_02.svg', 0),
    (@s13, 'seed-store-02.svg', 'seed_store_02_hansung_03.svg', 0),
    (@s14, 'seed-store-09.svg', 'seed_store_09_hansung_04.svg', 0),
    (@s15, 'seed-store-05.svg', 'seed_store_05_hansung_05.svg', 0),
    (@s16, 'seed-store-08.svg', 'seed_store_08_seoul_01.svg', 0),
    (@s17, 'seed-store-04.svg', 'seed_store_04_seoul_02.svg', 0),
    (@s18, 'seed-store-10.svg', 'seed_store_10_seoul_03.svg', 0),
    (@s19, 'seed-store-01.svg', 'seed_store_01_seoul_04.svg', 0),
    (@s20, 'seed-store-06.svg', 'seed_store_06_seoul_05.svg', 0);

/* ============================================================
   영업시간 — 개발 편의상 모든 요일 00:00~23:59 영업
   ============================================================ */

INSERT INTO store_business_hours (store_id, day_of_week, is_closed, start_time, end_time, last_order_time)
SELECT s.id, d.day, FALSE, '00:00:00', '23:59:00', '23:58:00'
FROM store s
JOIN (SELECT 1 AS day UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7) d
WHERE s.id IN (
    @s1,@s2,@s3,@s4,@s5,@s6,@s7,@s8,@s9,@s10,
    @s11,@s12,@s13,@s14,@s15,@s16,@s17,@s18,@s19,@s20
);

/* ============================================================
   빵 삽입
   - 기존 10개 매장: 각 7개, 총 70개
   - 추가 10개 매장: 각 5개, 총 50개
   빵 이미지 10장(seed_bread_01~10)을 돌려서 사용
   is_deleted = FALSE, deleted_at = NULL (기본값)
   ============================================================ */

/* ── 가게 1: 르뺑드마리 강남점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description, is_deleted) VALUES
    (@s1, '클래식 크루아상',     4200, 3200, 12, '겹겹이 바삭한 정통 프랑스식 크루아상입니다.', FALSE),
    (@s1, '통밀 캄파뉴',         7500, 5500,  3, '천연 발효종으로 만든 묵직한 캄파뉴입니다.', FALSE),
    (@s1, '에그 타르트',         3800, 2800,  8, '바삭한 페이스트리에 부드러운 커스터드가 가득합니다.', FALSE),
    (@s1, '갈릭 바게트',         5000, 3500, 15, '마늘 버터가 듬뿍 발린 바삭한 바게트입니다.', FALSE),
    (@s1, '초코 브리오슈',       4500, 3200,  0, '진한 초콜릿이 들어간 부드러운 브리오슈입니다.', FALSE),
    (@s1, '블루베리 스콘',       3500, 2500,  6, '블루베리가 톡톡 터지는 영국식 스콘입니다.', FALSE),
    (@s1, '호두 크랜베리 빵',    5500, 4000,  4, '호두와 크랜베리가 듬뿍 들어간 건강빵입니다.', FALSE);

/* ── 가게 2: 밀도 선릉점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description, is_deleted) VALUES
    (@s2, '우유 식빵',           4500, 3200, 10, '부드럽고 달콤한 프리미엄 우유식빵입니다.', FALSE),
    (@s2, '크림치즈 베이글',     4000, 2800,  7, '크림치즈가 듬뿍 들어간 쫄깃한 베이글입니다.', FALSE),
    (@s2, '시나몬 롤',           4800, 3500,  5, '달콤한 시나몬 향이 가득한 롤빵입니다.', FALSE),
    (@s2, '올리브 포카치아',     5200, 3800, 11, '올리브와 로즈마리가 올라간 이탈리안 포카치아입니다.', FALSE),
    (@s2, '단팥 크루아상',       4200, 3000,  0, '달콤한 팥앙금이 들어간 크루아상입니다.', FALSE),
    (@s2, '레몬 파운드케이크',   5000, 3500,  9, '상큼한 레몬 글레이즈가 올라간 파운드케이크입니다.', FALSE),
    (@s2, '흑미 식빵',           5500, 4000,  2, '흑미로 만든 건강한 식빵입니다.', FALSE);

/* ── 가게 3: 나폴레옹과자점 역삼점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description, is_deleted) VALUES
    (@s3, '소보로빵',            3000, 2100, 14, '바삭한 소보로 토핑이 올라간 추억의 빵입니다.', FALSE),
    (@s3, '카레빵',              3800, 2700,  6, '매콤한 카레가 가득 찬 튀김빵입니다.', FALSE),
    (@s3, '크림빵',              3200, 2300, 13, '부드러운 커스터드 크림이 가득합니다.', FALSE),
    (@s3, '피자빵',              4000, 2800,  0, '토마토소스와 치즈가 올라간 피자빵입니다.', FALSE),
    (@s3, '꽈배기 도넛',         2800, 2000, 10, '달콤한 설탕이 묻은 꽈배기 도넛입니다.', FALSE),
    (@s3, '햄치즈 샌드위치빵',   4500, 3200,  8, '햄과 치즈가 들어간 든든한 샌드위치빵입니다.', FALSE),
    (@s3, '밤식빵',              6800, 5000,  1, '달콤한 밤이 듬뿍 들어간 식빵입니다.', FALSE);

/* ── 가게 4: 오월의종 삼성점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description, is_deleted) VALUES
    (@s4, '사워도우 부울',       8500, 6500,  4, '48시간 발효한 정통 사워도우입니다.', FALSE),
    (@s4, '호밀빵',              6000, 4200,  7, '100% 호밀로 만든 독일식 빵입니다.', FALSE),
    (@s4, '치아바타',            5200, 3900,  9, '겉은 바삭하고 속은 쫄깃한 이탈리안 치아바타입니다.', FALSE),
    (@s4, '무화과 호두빵',       7000, 5200,  2, '무화과와 호두가 박힌 건강한 빵입니다.', FALSE),
    (@s4, '올리브 치아바타',     5500, 4000, 11, '블랙 올리브가 들어간 치아바타입니다.', FALSE),
    (@s4, '통밀 바게트',         4800, 3500,  0, '통밀로 만든 바삭한 바게트입니다.', FALSE),
    (@s4, '크랜베리 호밀빵',     6500, 4800,  5, '크랜베리가 들어간 새콤달콤한 호밀빵입니다.', FALSE);

/* ── 가게 5: 빵명장 대치점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description, is_deleted) VALUES
    (@s5, '앙버터 바게트',       5800, 4300,  6, '달콤한 팥과 버터의 환상 조합입니다.', FALSE),
    (@s5, '마카다미아 쿠키빵',   4200, 3000, 10, '마카다미아가 듬뿍 들어간 쿠키빵입니다.', FALSE),
    (@s5, '녹차 크루아상',       4500, 3200,  3, '녹차 크림이 들어간 크루아상입니다.', FALSE),
    (@s5, '모카빵',              3500, 2500, 15, '커피 향이 진한 모카 크림빵입니다.', FALSE),
    (@s5, '치즈 스틱',           3000, 2100,  0, '쭉 늘어나는 모짜렐라 치즈 스틱입니다.', FALSE),
    (@s5, '딸기 크림빵',         4000, 2800,  8, '신선한 딸기 크림이 가득한 빵입니다.', FALSE),
    (@s5, '소금빵',              3500, 2500, 12, '겉은 바삭하고 속은 촉촉한 소금빵입니다.', FALSE);

/* ── 가게 6: 쿠헨 논현점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description, is_deleted) VALUES
    (@s6, '프레첼',              3800, 2800, 11, '독일식 정통 프레첼입니다.', FALSE),
    (@s6, '라우겐 롤',           4200, 3000,  7, '라우겐 반죽으로 만든 쫄깃한 롤빵입니다.', FALSE),
    (@s6, '호밀 사워도우',       7000, 5200,  4, '독일식 호밀 사워도우입니다.', FALSE),
    (@s6, '버터 쿠헨',           5500, 4000,  9, '버터가 듬뿍 들어간 독일식 케이크빵입니다.', FALSE),
    (@s6, '슈톨렌',              9000, 6500,  1, '과일과 견과류가 가득한 독일 전통빵입니다.', FALSE),
    (@s6, '카이저 롤',           3500, 2500, 13, '바삭한 겉면의 오스트리아식 롤빵입니다.', FALSE),
    (@s6, '흑맥주빵',            6000, 4200,  0, '흑맥주로 반죽한 풍미 깊은 빵입니다.', FALSE);

/* ── 가게 7: 아티장베이커리 청담점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description, is_deleted) VALUES
    (@s7, '트러플 포카치아',     8000, 6000,  3, '트러플 오일을 넣은 프리미엄 포카치아입니다.', FALSE),
    (@s7, '얼그레이 스콘',       4500, 3200,  8, '얼그레이 향이 은은한 영국식 스콘입니다.', FALSE),
    (@s7, '크로플',              4500, 3200, 10, '크루아상 반죽으로 구운 바삭한 와플입니다.', FALSE),
    (@s7, '유자 파운드케이크',   5500, 4000,  5, '상큼한 유자가 들어간 파운드케이크입니다.', FALSE),
    (@s7, '피스타치오 크루아상', 5800, 4200,  0, '피스타치오 크림이 가득한 크루아상입니다.', FALSE),
    (@s7, '바닐라 브리오슈',     4200, 3000, 14, '바닐라빈이 들어간 부드러운 브리오슈입니다.', FALSE),
    (@s7, '잣 타르트',           6000, 4500,  2, '고소한 잣이 올라간 미니 타르트입니다.', FALSE);

/* ── 가게 8: 브레드랩 서초점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description, is_deleted) VALUES
    (@s8, '비건 통밀빵',         5000, 3500,  7, '동물성 재료 없이 만든 건강한 통밀빵입니다.', FALSE),
    (@s8, '글루텐프리 머핀',     4500, 3200,  4, '쌀가루로 만든 글루텐프리 블루베리 머핀입니다.', FALSE),
    (@s8, '흑임자 식빵',         5500, 4000, 11, '고소한 흑임자가 들어간 식빵입니다.', FALSE),
    (@s8, '당근 케이크빵',       4800, 3500,  6, '당근과 호두가 들어간 건강한 케이크빵입니다.', FALSE),
    (@s8, '귀리 쿠키',           3200, 2300, 15, '귀리와 건포도가 들어간 건강 쿠키입니다.', FALSE),
    (@s8, '고구마 크루아상',     5500, 3800,  0, '달콤한 고구마 무스가 들어간 크루아상입니다.', FALSE),
    (@s8, '두부 도넛',           3000, 2100,  9, '두부로 만든 담백한 도넛입니다.', FALSE);

/* ── 가게 9: 밀밭제과 잠실점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description, is_deleted) VALUES
    (@s9, '단팥빵',              3000, 2100, 13, '달콤한 팥소가 가득한 전통 단팥빵입니다.', FALSE),
    (@s9, '고로케',              3500, 2500,  8, '바삭한 튀김옷 속 크림 고로케입니다.', FALSE),
    (@s9, '메론빵',              3800, 2700, 10, '메론 모양의 달콤한 빵입니다.', FALSE),
    (@s9, '찹쌀 도넛',           2800, 2000,  5, '쫄깃한 찹쌀 반죽의 도넛입니다.', FALSE),
    (@s9, '마늘 크림치즈빵',     4200, 3000, 14, '마늘과 크림치즈가 조화로운 빵입니다.', FALSE),
    (@s9, '팥앙금빵',            3500, 2500,  0, '직접 만든 팥앙금이 들어간 부드러운 빵입니다.', FALSE),
    (@s9, '옥수수빵',            3200, 2300,  7, '달콤한 옥수수 크림이 들어간 빵입니다.', FALSE);

/* ── 가게 10: 뚜레쥬르 양재점 (7개) ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description, is_deleted) VALUES
    (@s10, '허니 브레드',        5000, 3500, 11, '꿀이 듬뿍 발린 달콤한 식빵입니다.', FALSE),
    (@s10, '크로크무슈',         5500, 4000,  6, '햄과 치즈가 올라간 프랑스식 토스트입니다.', FALSE),
    (@s10, '티라미수 크림빵',    4500, 3200,  3, '티라미수 크림이 가득한 빵입니다.', FALSE),
    (@s10, '플레인 베이글',      3500, 2400, 15, '쫄깃한 식감의 기본 베이글입니다.', FALSE),
    (@s10, '초코 머핀',          3800, 2700,  0, '진한 초콜릿 머핀입니다.', FALSE),
    (@s10, '마늘 바게트',        5000, 3500,  9, '마늘 버터가 듬뿍 발린 바게트입니다.', FALSE),
    (@s10, '호두파이',           6000, 4200,  4, '고소한 호두가 가득한 미니 파이입니다.', FALSE);

/* ── 한성대학교 근처 추가 매장 5개: 각 5개 ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description, is_deleted) VALUES
    (@s11, '한성 소금빵',         3600, 2600, 18, '버터 향이 진한 한성대 정문 대표 소금빵입니다.', FALSE),
    (@s11, '캠퍼스 크루아상',     4300, 3100, 12, '등교길에 먹기 좋은 바삭한 크루아상입니다.', FALSE),
    (@s11, '삼선동 단팥빵',       3200, 2300, 15, '직접 끓인 팥앙금이 들어간 부드러운 단팥빵입니다.', FALSE),
    (@s11, '치즈 바게트',         5200, 3800,  8, '고소한 치즈가 녹아든 바삭한 바게트입니다.', FALSE),
    (@s11, '초코 스콘',           3500, 2500, 10, '초코칩을 넣어 달콤하게 구운 스콘입니다.', FALSE),
    (@s12, '버터 소금빵',         3700, 2700, 16, '겉은 바삭하고 속은 촉촉한 버터 소금빵입니다.', FALSE),
    (@s12, '갈릭 크림치즈빵',     4400, 3200,  9, '마늘 크림치즈가 가득한 인기 메뉴입니다.', FALSE),
    (@s12, '시나몬 큐브식빵',     4800, 3500, 11, '시나몬 향이 은은한 작은 큐브 식빵입니다.', FALSE),
    (@s12, '감자 치아바타',       5000, 3600,  7, '포슬한 감자와 치아바타의 담백한 조합입니다.', FALSE),
    (@s12, '꿀호떡 브레드',       3900, 2800, 13, '꿀과 견과류가 들어간 달콤한 간식빵입니다.', FALSE),
    (@s13, '성북천 사워도우',     7800, 5800,  6, '천천히 발효해 깊은 산미를 낸 사워도우입니다.', FALSE),
    (@s13, '올리브 포카치아',     5400, 3900, 10, '올리브와 허브가 올라간 촉촉한 포카치아입니다.', FALSE),
    (@s13, '호두 무화과빵',       6800, 5000,  5, '무화과와 호두를 듬뿍 넣은 식사빵입니다.', FALSE),
    (@s13, '통밀 베이글',         4200, 3000, 14, '통밀로 구워 고소하고 쫄깃한 베이글입니다.', FALSE),
    (@s13, '레몬 파운드',         5000, 3600,  8, '레몬 글레이즈를 올린 산뜻한 파운드케이크입니다.', FALSE),
    (@s14, '동소문 크루아상',     4500, 3300, 15, '한성대입구역 근처에서 갓 구운 대표 크루아상입니다.', FALSE),
    (@s14, '아몬드 크루아상',     5200, 3900,  8, '아몬드 크림과 슬라이스가 올라간 달콤한 크루아상입니다.', FALSE),
    (@s14, '라즈베리 데니쉬',     4900, 3500,  9, '새콤한 라즈베리잼을 올린 데니쉬입니다.', FALSE),
    (@s14, '바닐라 브리오슈',     4300, 3100, 12, '바닐라빈 향이 부드러운 브리오슈입니다.', FALSE),
    (@s14, '카라멜 퀸아망',       5200, 3800,  6, '겹겹의 버터와 카라멜이 바삭한 퀸아망입니다.', FALSE),
    (@s15, '혜화문 캄파뉴',       7600, 5600,  7, '발효종으로 묵직하게 구운 건강한 캄파뉴입니다.', FALSE),
    (@s15, '성북 호밀빵',         6200, 4500,  9, '호밀 향이 진한 담백한 식사빵입니다.', FALSE),
    (@s15, '쑥 단팥빵',           3800, 2700, 11, '쑥 향과 팥앙금이 어우러진 한국식 빵입니다.', FALSE),
    (@s15, '밤 크림빵',           4300, 3100, 10, '달콤한 밤 크림을 넣은 부드러운 빵입니다.', FALSE),
    (@s15, '참깨 베이글',         4200, 3000, 13, '참깨를 듬뿍 묻혀 고소한 베이글입니다.', FALSE);

/* ── 서울 전역 추가 매장 5개: 각 5개 ── */
INSERT INTO bread (store_id, name, original_price, sale_price, remaining_quantity, description, is_deleted) VALUES
    (@s16, '연남 사워도우',       8200, 6200,  6, '연남동 스타일의 산미 있는 천연발효 사워도우입니다.', FALSE),
    (@s16, '바질 토마토 포카치아', 5800, 4200, 10, '바질과 토마토가 올라간 향긋한 포카치아입니다.', FALSE),
    (@s16, '크랜베리 깜빠뉴',     7200, 5300,  5, '크랜베리와 견과류가 들어간 달콤한 깜빠뉴입니다.', FALSE),
    (@s16, '플레인 치아바타',     5000, 3600, 12, '샌드위치로 먹기 좋은 쫄깃한 치아바타입니다.', FALSE),
    (@s16, '연남 말차스콘',       4300, 3100,  9, '말차 향이 진한 촉촉한 스콘입니다.', FALSE),
    (@s17, '성수 버터 크루아상',  4600, 3400, 16, '버터 풍미가 진한 성수동 대표 크루아상입니다.', FALSE),
    (@s17, '더블초코 데니쉬',     5200, 3800,  8, '진한 초콜릿 크림을 넣은 데니쉬입니다.', FALSE),
    (@s17, '카라멜 소금빵',       3900, 2900, 14, '달콤한 카라멜과 짭짤한 버터가 만난 소금빵입니다.', FALSE),
    (@s17, '피칸 퀸아망',         5600, 4100,  7, '피칸과 카라멜이 올라간 바삭한 퀸아망입니다.', FALSE),
    (@s17, '얼그레이 파운드',     5200, 3700,  9, '얼그레이 향이 은은한 파운드케이크입니다.', FALSE),
    (@s18, '한강로 우유식빵',     5000, 3600, 13, '부드러운 식감의 데일리 우유식빵입니다.', FALSE),
    (@s18, '햄치즈 브레드',       4800, 3500, 10, '햄과 치즈를 넣어 식사 대용으로 좋은 빵입니다.', FALSE),
    (@s18, '명란 바게트',         5400, 3900,  8, '짭조름한 명란 크림을 바른 바게트입니다.', FALSE),
    (@s18, '고구마 식빵',         5600, 4100,  7, '달콤한 고구마 무스가 들어간 식빵입니다.', FALSE),
    (@s18, '블루베리 머핀',       3800, 2700, 12, '블루베리가 가득한 촉촉한 머핀입니다.', FALSE),
    (@s19, '북한산 통밀빵',       6200, 4500, 10, '통밀의 고소함을 살린 건강 식사빵입니다.', FALSE),
    (@s19, '꿀 견과 브레드',      5800, 4200,  9, '꿀과 견과류를 넣어 달콤하고 든든한 빵입니다.', FALSE),
    (@s19, '단호박 깜빠뉴',       7000, 5200,  6, '단호박과 발효종이 어우러진 담백한 깜빠뉴입니다.', FALSE),
    (@s19, '흑임자 크림빵',       4200, 3000, 12, '고소한 흑임자 크림이 들어간 빵입니다.', FALSE),
    (@s19, '팥버터 모닝롤',       3900, 2800, 14, '팥앙금과 버터를 넣은 작은 모닝롤입니다.', FALSE),
    (@s20, '여의도 베이글',       4300, 3100, 18, '출근길에 먹기 좋은 담백한 플레인 베이글입니다.', FALSE),
    (@s20, '에그마요 샌드빵',     5200, 3800, 11, '에그마요를 넣은 든든한 샌드위치 빵입니다.', FALSE),
    (@s20, '올리브 치즈빵',       5000, 3600,  9, '올리브와 치즈를 듬뿍 넣은 짭짤한 빵입니다.', FALSE),
    (@s20, '시그니처 소금빵',     3700, 2700, 15, '직장인 간식으로 인기 있는 버터 소금빵입니다.', FALSE),
    (@s20, '초코 바나나 머핀',    4000, 2900, 10, '초코와 바나나가 들어간 달콤한 머핀입니다.', FALSE);

/* ============================================================
   빵 이미지 — 10장(seed_bread_01~10)을 돌려서 사용
   bread_image.bread_id 는 UNIQUE 이므로 빵 1개당 이미지 1개
   ============================================================ */

INSERT INTO bread_image (bread_id, original_filename, stored_filename)
SELECT b.id,
       CONCAT('seed-bread-', LPAD(((ROW_NUMBER() OVER (ORDER BY b.id) - 1) % 10) + 1, 2, '0'), '.svg'),
       CONCAT('seed_bread_', LPAD(((ROW_NUMBER() OVER (ORDER BY b.id) - 1) % 10) + 1, 2, '0'), '_', b.id, '.svg')
FROM bread b
WHERE b.store_id IN (
    @s1,@s2,@s3,@s4,@s5,@s6,@s7,@s8,@s9,@s10,
    @s11,@s12,@s13,@s14,@s15,@s16,@s17,@s18,@s19,@s20
);

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
FROM bread b WHERE b.store_id = @s1 AND b.is_deleted = FALSE ORDER BY b.id LIMIT 2;

/* ============================================================
   주문 / 매출 데이터 (2026-01-01 ~ 현재)
   기존 10개 매장(@s1~@s10)에만 생성하여 기존 매출/주문 범위를 유지
   각 가게별 월 6~10건, 상태 혼합 (PICKED_UP / CONFIRMED / CANCELLED)
   주문번호 UNIQUE 제약: (store_id, order_date, order_number)
   ============================================================ */

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
    DECLARE v_day_seq INT;

    /* 기존 10개 가게 ID만 임시 테이블에 — 추가 매장은 주문/매출 데이터 제외 */
    DROP TEMPORARY TABLE IF EXISTS tmp_stores;
    CREATE TEMPORARY TABLE tmp_stores (idx INT AUTO_INCREMENT PRIMARY KEY, store_id BIGINT);
    INSERT INTO tmp_stores (store_id) VALUES
        (@s1),(@s2),(@s3),(@s4),(@s5),(@s6),(@s7),(@s8),(@s9),(@s10);

    SET v_store_idx = 1;

    WHILE v_store_idx <= 10 DO
        SELECT store_id INTO v_store_id FROM tmp_stores WHERE idx = v_store_idx;

        /* 해당 가게의 빵 개수 (삭제되지 않은 것만) */
        SELECT COUNT(*) INTO v_bread_count FROM bread WHERE store_id = v_store_id AND is_deleted = FALSE;

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
            SET v_day_seq = 0;

            WHILE v_i < v_order_count DO
                /* 날짜를 순차적으로 분배하여 같은 날 주문번호 충돌 방지 */
                SET v_order_date = DATE_ADD(v_month_start,
                    INTERVAL FLOOR(v_i * (DATEDIFF(v_month_end, v_month_start) + 1) / v_order_count) DAY);

                /* 상태: 80% PICKED_UP, 10% CONFIRMED, 10% CANCELLED */
                SET v_rand = RAND();
                IF v_rand < 0.10 THEN
                    SET v_status = 'CANCELLED';
                ELSEIF v_rand < 0.20 THEN
                    SET v_status = 'CONFIRMED';
                ELSE
                    SET v_status = 'PICKED_UP';
                END IF;

                /* 주문번호 4자리 영숫자 — store+month+seq 기반으로 유니크 보장 */
                SET v_order_num = UPPER(SUBSTR(MD5(CONCAT(v_store_id, v_cur_month, v_i)), 1, 4));

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
                    /* 랜덤 빵 선택 (삭제되지 않은 것만) */
                    SET v_bread_offset = FLOOR(RAND() * v_bread_count);
                    SELECT id, name, sale_price INTO v_bread_id, v_bread_name, v_bread_price
                    FROM bread WHERE store_id = v_store_id AND is_deleted = FALSE
                    ORDER BY id LIMIT 1 OFFSET v_bread_offset;

                    IF v_bread_id IS NOT NULL THEN
                        SET v_qty = 1 + FLOOR(RAND() * 4);
                        SET v_total = v_total + (v_bread_price * v_qty);

                        INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity)
                        VALUES (v_order_id, v_bread_id, v_bread_name, v_bread_price, v_qty);
                    END IF;

                    SET v_j = v_j + 1;
                END WHILE;

                /* total_amount 업데이트 (최소 1 보장 — CHECK 제약) */
                IF v_total > 0 THEN
                    UPDATE orders SET total_amount = v_total WHERE id = v_order_id;
                END IF;

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
   기존 10개 가게별 오늘 날짜 CONFIRMED 주문 2건 (픽업 대기 테스트용)
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
        FROM bread WHERE store_id = v_store_id AND is_deleted = FALSE ORDER BY id LIMIT 1;

        SET v_qty = 1 + FLOOR(RAND() * 3);
        /* 주문번호: 가게별 고유하게 생성 (T + idx 기반) */
        SET v_order_num = CONCAT('T', LPAD(v_idx, 2, '0'), '1');

        INSERT INTO orders (user_id, store_id, status, total_amount, order_number, order_date, created_at)
        VALUES (@uid, v_store_id, 'CONFIRMED', v_bread_price * v_qty, v_order_num, CURDATE(), NOW());
        SET v_order_id = LAST_INSERT_ID();
        INSERT INTO order_item (order_id, bread_id, bread_name, bread_price, quantity)
        VALUES (v_order_id, v_bread_id, v_bread_name, v_bread_price, v_qty);

        /* 두 번째 픽업 대기 주문 */
        SELECT id, name, sale_price INTO v_bread_id, v_bread_name, v_bread_price
        FROM bread WHERE store_id = v_store_id AND is_deleted = FALSE ORDER BY id LIMIT 1 OFFSET 1;

        SET v_qty = 1 + FLOOR(RAND() * 2);
        SET v_order_num = CONCAT('T', LPAD(v_idx, 2, '0'), '2');

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
   결제 데이터 — 기존 주문(CONFIRMED/PICKED_UP)에 결제 레코드 매칭
   ============================================================ */

INSERT INTO payment (order_id, amount, status, paid_at, idempotency_key, payment_key, method)
SELECT
    o.id,
    o.total_amount,
    'APPROVED',
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

/* ============================================================
   리뷰 데이터 — 기존 10개 매장의 PICKED_UP 주문 order_item에 대해 리뷰 생성
   각 가게별 3~5개 리뷰, rating_sum/review_count도 갱신
   ============================================================ */

DROP PROCEDURE IF EXISTS seed_reviews;

DELIMITER //
CREATE PROCEDURE seed_reviews()
BEGIN
    DECLARE v_store_id BIGINT;
    DECLARE v_idx INT DEFAULT 1;
    DECLARE v_order_item_id BIGINT;
    DECLARE v_bread_id BIGINT;
    DECLARE v_rating INT;
    DECLARE v_review_count INT;
    DECLARE v_rating_sum INT;
    DECLARE v_review_id BIGINT;
    DECLARE v_limit INT;
    DECLARE v_offset INT;
    DECLARE v_i INT;
    DECLARE v_content VARCHAR(500);

    DROP TEMPORARY TABLE IF EXISTS tmp_stores3;
    CREATE TEMPORARY TABLE tmp_stores3 (idx INT AUTO_INCREMENT PRIMARY KEY, store_id BIGINT);
    INSERT INTO tmp_stores3 (store_id) VALUES
        (@s1),(@s2),(@s3),(@s4),(@s5),(@s6),(@s7),(@s8),(@s9),(@s10);

    /* 리뷰 내용 템플릿 */
    DROP TEMPORARY TABLE IF EXISTS tmp_contents;
    CREATE TEMPORARY TABLE tmp_contents (idx INT AUTO_INCREMENT PRIMARY KEY, content VARCHAR(500));
    INSERT INTO tmp_contents (content) VALUES
        ('빵이 정말 맛있어요! 매일 오고 싶은 빵집입니다. 강력 추천합니다.'),
        ('갓 구운 빵 향기가 너무 좋았어요. 다음에도 꼭 다시 방문할게요.'),
        ('가격 대비 퀄리티가 훌륭합니다. 친구들에게도 추천했어요.'),
        ('빵이 부드럽고 맛있었어요. 포장도 깔끔하고 직원분도 친절하셨습니다.'),
        ('여기 빵은 항상 신선해서 좋아요. 단골이 될 것 같습니다.');

    WHILE v_idx <= 10 DO
        SELECT store_id INTO v_store_id FROM tmp_stores3 WHERE idx = v_idx;

        /* 가게별 리뷰 수: 3~5개 */
        SET v_limit = 3 + FLOOR(RAND() * 3);
        SET v_i = 0;
        SET v_review_count = 0;
        SET v_rating_sum = 0;

        /* PICKED_UP 주문의 order_item 중에서 리뷰 생성 */
        WHILE v_i < v_limit DO
            SELECT oi.id, oi.bread_id
            INTO v_order_item_id, v_bread_id
            FROM order_item oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.store_id = v_store_id
              AND o.status = 'PICKED_UP'
              AND o.user_id = @uid
              AND NOT EXISTS (SELECT 1 FROM review r WHERE r.order_item_id = oi.id AND r.user_id = @uid)
            ORDER BY oi.id
            LIMIT 1 OFFSET v_i;

            IF v_order_item_id IS NOT NULL THEN
                SET v_rating = 3 + FLOOR(RAND() * 3); /* 3~5점 */

                SELECT content INTO v_content FROM tmp_contents WHERE idx = (v_i % 5) + 1;

                INSERT INTO review (user_id, store_id, bread_id, order_item_id, rating, content)
                VALUES (@uid, v_store_id, v_bread_id, v_order_item_id, v_rating, v_content);

                SET v_review_count = v_review_count + 1;
                SET v_rating_sum = v_rating_sum + v_rating;
            END IF;

            SET v_order_item_id = NULL;
            SET v_bread_id = NULL;
            SET v_i = v_i + 1;
        END WHILE;

        /* store의 rating_sum, review_count 갱신 */
        IF v_review_count > 0 THEN
            UPDATE store SET rating_sum = v_rating_sum, review_count = v_review_count
            WHERE id = v_store_id;
        END IF;

        SET v_idx = v_idx + 1;
    END WHILE;

    DROP TEMPORARY TABLE IF EXISTS tmp_stores3;
    DROP TEMPORARY TABLE IF EXISTS tmp_contents;
END //
DELIMITER ;

CALL seed_reviews();
DROP PROCEDURE IF EXISTS seed_reviews;

COMMIT;
