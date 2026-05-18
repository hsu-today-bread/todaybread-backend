# Scripts

이 문서는 `scripts/` 폴더의 개발 보조 스크립트가 무엇을 하는지 정리합니다.

## 전체 목록

| 경로 | 용도 |
|------|------|
| `scripts/mysql-connect.sh` | Docker MySQL 컨테이너에 접속하는 편의 스크립트 |
| `scripts/test-data.sh` | 개발용 seed 데이터와 seed 이미지를 준비하고 `test-data.sql`을 DB에 적용 |
| `scripts/test-data.sql` | 샘플 유저, 사장님, 관심지역, 키워드, 매장, 영업시간, 빵, 이미지, 즐겨찾기, 주문, 결제, 리뷰 데이터를 삽입 |
| `scripts/seed-images/` | `test-data.sh`가 `uploads/`로 복사할 실제 seed 이미지 원본 |

## `mysql-connect.sh`

Docker Compose로 실행 중인 MySQL 컨테이너에 `mysql` CLI로 접속합니다.

```bash
./scripts/mysql-connect.sh
```

이 스크립트는 편의를 위해 내부 fallback 값을 가지고 있습니다. 일반 로컬 개발 흐름에서는 `.env`의 MySQL 값과 맞춰 실행하는 것을 권장합니다.

| 환경 변수 | 설명 |
|-----------|------|
| `MYSQL_CONTAINER_NAME` | 접속할 Docker 컨테이너 이름 |
| `MYSQL_DATABASE` | 접속할 데이터베이스 |
| `MYSQL_USER` | MySQL 사용자 |
| `MYSQL_PASSWORD` | MySQL 비밀번호 |
| `MYSQL_DEFAULT_CHARSET` | CLI 문자셋 |
| `MYSQL_HOST` | 컨테이너 내부에서 접속할 host |

뒤에 MySQL CLI 옵션을 그대로 붙일 수 있습니다.

```bash
./scripts/mysql-connect.sh -e "SHOW TABLES;"
```

## `test-data.sh`

개발용 테스트 데이터를 DB에 넣는 실행 스크립트입니다. 기본 SQL 파일은 `scripts/test-data.sql`입니다.

```bash
./scripts/test-data.sh
```

다른 SQL 파일을 적용하려면 첫 번째 인자로 넘깁니다.

```bash
./scripts/test-data.sh ./path/to/custom-seed.sql
```

실행 흐름:

1. `todaybread-mysql` 컨테이너가 준비될 때까지 대기합니다.
2. Flyway가 만든 기본 테이블이 있는지 확인합니다. `users`, `interest_area`, `store`, `orders`, `review` 등 seed에 필요한 테이블이 없으면 중단합니다.
3. `uploads/` 디렉터리에 seed 이미지를 준비합니다.
4. 지정된 SQL 파일을 MySQL에 적용합니다.
5. SQL 적용 후 `store_image`, `bread_image`, `review_image`의 `stored_filename`에 맞는 이미지 파일을 생성합니다.

이미지 처리:

- `scripts/seed-images/seed_store_01.*` ~ `seed_store_10.*`이 있으면 매장 이미지 원본으로 사용합니다.
- `scripts/seed-images/seed_bread_01.*` ~ `seed_bread_10.*`이 있으면 빵 이미지 원본으로 사용합니다.
- `scripts/seed-images/seed_review_01.*` ~ `seed_review_05.*`이 있으면 리뷰 이미지 원본으로 사용합니다.
- 원본 이미지가 없으면 SVG placeholder를 생성합니다.
- `UPLOAD_DIR` 환경 변수는 Spring local profile과 seed 이미지 복사 경로를 맞추는 데 사용합니다.

토큰 주의:

- access token과 refresh token은 seed하지 않습니다.
- 로그인 API가 토큰을 발급하고 `refresh_token`에는 refresh token 해시를 저장합니다.

## `test-data.sql`

개발용 seed 데이터 본문입니다. 재실행 가능하도록 기존 demo seed 계정과 일부 구버전 `.local` seed 데이터를 먼저 정리한 뒤 다시 삽입합니다.

삽입하는 주요 데이터:

- 일반 유저 20명: `demo-user01@todaybread.com` ~ `demo-user20@todaybread.com` / `todaybread123`
- 일반 유저별 관심지역 1개: 한성대학교, 반경 3km
- 일반 유저별 키워드 5개: 소금빵, 크루아상, 식빵, 베이글, 휘낭시에
- 사장님 120명: `demo-boss001@todaybread.com` ~ `demo-boss120@todaybread.com` / `todaybread123`
- 서울 전역 매장 120개
- 한성대학교 1km 이내 3개, 3km 이내 누적 10개, 5km 이내 누적 20개
- 한성대 5km 안 판매중 9개, 영업중 품절 7개, 휴무 4개
- 매장별 일반 영업시간: 평일 09:00~22:00, 토요일 09:00~21:00, 일요일 휴무
- 001번 사장님 매장(`demo-boss001`)은 FCM/주문 테스트용으로 매일 09:00~22:00 영업시간을 고정
- 매장별 메뉴 15개 이하, 한성대 1km 매장은 메뉴 3~5개
- 빵 이미지 레코드
- 유저별 0~5개 반복 즐겨찾기
- 장바구니는 비어 있는 상태로 유지
- 2026년 1월 1일부터 2026년 5월 7일까지의 주문/매출 내역
- 매장별 월 주문 날짜 5~15일, 2026년 5월은 5~7일만 생성
- 주문 상태에 맞는 결제 데이터 (`PICKED_UP`/`CANCELLED`만 생성)
- 매장별 리뷰 10개, 그중 이미지 리뷰 5개와 텍스트 리뷰 5개
- 리뷰는 `demo-user02`~`demo-user20`의 픽업 완료 주문상품에만 연결
- 이미지 리뷰는 리뷰당 1장 또는 2장

정상 실행 시 주요 검증 metric:

```text
seed_stores = 120
seed_reviews = 1200
seed_normal_users = 20
user01_orders = 10
user01_reviews = 0
demo_cart_rows = 0
favourite_pattern_mismatches = 0
invalid_review_order_links = 0
stores_without_reviews = 0
stores_below_10_reviews = 0
min_reviews_per_store = 10
max_reviews_per_store = 10
monthly_order_day_range_violations = 0
seed_store_images = 120
seed_bread_images = 생성된 빵 수와 동일
review_image_reviews = 600
seed_review_images = 900
```

추천 조회 좌표:

```text
Hansung Univ: lat=37.5826000, lng=127.0106000, radius=1
Hansung Univ: lat=37.5826000, lng=127.0106000, radius=3
Hansung Univ: lat=37.5826000, lng=127.0106000, radius=5
```

주의할 점:

- `test-data.sql`은 데이터 조작 스크립트입니다. 운영 DB에서 실행하면 안 됩니다.
- 기존 seed 계정과 관련된 주문, 결제, 장바구니, 이미지, 매장, 빵 데이터를 삭제한 뒤 다시 넣습니다.
- `refresh_token`은 삭제만 하고 새로 넣지 않습니다. 토큰은 로그인 API로 발급해야 합니다.
- 픽업 대기(`CONFIRMED`) 주문은 만들지 않습니다. 데모데이용 실시간 주문은 별도 스크립트에서 생성합니다.
- 현재 seed에는 `밀도 선릉점`, `시나몬 롤` 데이터가 없습니다. 프론트에서 해당 이름이 보이면 다른 DB, 캐시, 또는 별도 seed 데이터를 보고 있는지 확인해야 합니다.

## `seed-images/`

`test-data.sh`가 seed 이미지 원본으로 사용하는 파일 디렉터리입니다.

현재 규칙:

```text
scripts/seed-images/seed_store_01.png
scripts/seed-images/seed_store_02.webp
...
scripts/seed-images/seed_bread_01.jpeg
scripts/seed-images/seed_bread_02.jpg
...
scripts/seed-images/seed_review_01.jpeg
scripts/seed-images/seed_review_02.jpeg
...
```

파일 확장자는 `jpg`, `jpeg`, `png`, `webp`를 찾습니다. 같은 번호의 실제 이미지가 있으면 placeholder 대신 해당 이미지를 사용합니다.

## 일반 실행 순서

로컬 개발 DB를 처음 준비할 때는 보통 아래 순서로 실행합니다.

```bash
cp .env.example .env
docker compose up -d

set -a
source .env
set +a

./gradlew bootRun
```

서버가 뜬 뒤 다른 터미널에서 seed 데이터를 적용합니다.

```bash
./scripts/test-data.sh
```

주문/결제 흐름은 프론트엔드 토스 SDK와 백엔드 confirm API를 함께 사용해 확인합니다. 자세한 내용은 `docs/TOSS.md`를 참고합니다.
