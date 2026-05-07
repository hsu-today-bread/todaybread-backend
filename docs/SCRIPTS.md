# Scripts

이 문서는 `scripts/` 폴더의 개발 보조 스크립트가 무엇을 하는지 정리합니다.

## 전체 목록

| 경로 | 용도 |
|------|------|
| `scripts/mysql-connect.sh` | Docker MySQL 컨테이너에 접속하는 편의 스크립트 |
| `scripts/test-data.sh` | 개발용 seed 데이터와 seed 이미지를 준비하고 `test-data.sql`을 DB에 적용 |
| `scripts/test-data.sql` | 샘플 유저, 사장님, 매장, 영업시간, 빵, 이미지, 장바구니, 주문, 결제 데이터를 삽입 |
| `scripts/test-order.sh` | 로그인부터 주문 생성, 결제, 주문 상태 확인, 취소까지 확인하는 API 흐름 테스트 |
| `scripts/seed-images/` | `test-data.sh`가 `uploads/`로 복사할 실제 seed 이미지 원본 |

## `mysql-connect.sh`

Docker Compose로 실행 중인 MySQL 컨테이너에 `mysql` CLI로 접속합니다.

```bash
./scripts/mysql-connect.sh
```

기본 접속값은 프로젝트 기본 DB 설정과 맞춰져 있습니다.

| 환경 변수 | 기본값 | 설명 |
|-----------|--------|------|
| `MYSQL_CONTAINER_NAME` | `todaybread-mysql` | 접속할 Docker 컨테이너 이름 |
| `MYSQL_DATABASE` | `todaybread` | 접속할 데이터베이스 |
| `MYSQL_USER` | `todaybread` | MySQL 사용자 |
| `MYSQL_PASSWORD` | `todaybread` | MySQL 비밀번호 |
| `MYSQL_DEFAULT_CHARSET` | `utf8mb4` | CLI 문자셋 |
| `MYSQL_HOST` | `127.0.0.1` | 컨테이너 내부에서 접속할 host |

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
2. Flyway가 만든 기본 테이블이 있는지 확인합니다.
3. `uploads/` 디렉터리에 seed 이미지를 준비합니다.
4. 지정된 SQL 파일을 MySQL에 적용합니다.
5. SQL 적용 후 `store_image`, `bread_image`, `review_image`의 `stored_filename`에 맞는 이미지 파일을 생성합니다.

이미지 처리:

- `scripts/seed-images/seed_store_01.*` ~ `seed_store_10.*`이 있으면 매장 이미지 원본으로 사용합니다.
- `scripts/seed-images/seed_bread_01.*` ~ `seed_bread_10.*`이 있으면 빵 이미지 원본으로 사용합니다.
- `scripts/seed-images/seed_review_01.*` ~ `seed_review_05.*`이 있으면 리뷰 이미지 원본으로 사용합니다.
- 원본 이미지가 없으면 SVG placeholder를 생성합니다.
- `UPLOAD_DIR` 환경 변수로 업로드 디렉터리를 바꿀 수 있습니다. 기본값은 프로젝트 루트의 `uploads/`입니다.

토큰 주의:

- access token과 refresh token은 seed하지 않습니다.
- 로그인 API가 토큰을 발급하고 `refresh_token`에는 refresh token 해시를 저장합니다.

## `test-data.sql`

개발용 seed 데이터 본문입니다. 재실행 가능하도록 기존 demo seed 계정과 일부 구버전 `.local` seed 데이터를 먼저 정리한 뒤 다시 삽입합니다.

삽입하는 주요 데이터:

- 일반 유저 1명: `demo-user01@todaybread.com / todaybread123`
- 사장님 120명: `demo-boss1@todaybread.com` ~ `demo-boss120@todaybread.com / todaybread123`
- 서울 전역 매장 120개
- 한성대학교 1km 이내 3개, 3km 이내 누적 10개, 5km 이내 누적 20개
- 한성대 5km 안 판매중 9개, 영업중 품절 7개, 휴무 4개
- 매장별 일반 영업시간: 평일 09:00~22:00, 토요일 09:00~21:00, 일요일 휴무
- 매장별 메뉴 15개 이하, 한성대 1km 매장은 메뉴 3~5개
- 빵 이미지 레코드
- 즐겨찾기
- 장바구니 샘플
- 2026년 1월 1일부터 2026년 5월 7일까지의 주문/매출 내역
- 매장별 월 주문 최대 15건, 2026년 5월은 7일까지만 생성
- 주문 상태에 맞는 결제 데이터 (`PICKED_UP`/`CANCELLED`만 생성)
- 매장별 리뷰 10개, 그중 이미지 리뷰 5개와 텍스트 리뷰 5개
- 이미지 리뷰는 리뷰당 1장 또는 2장

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
- `sellingStatus` 값은 DB에 저장하지 않습니다. 백엔드가 영업시간과 재고를 기반으로 계산해야 합니다.
- 픽업 대기(`CONFIRMED`) 주문은 만들지 않습니다. 데모데이용 실시간 주문은 별도 스크립트에서 생성합니다.

## `test-order.sh`

주문과 결제 API 흐름을 빠르게 확인하는 스크립트입니다.

```bash
./scripts/test-order.sh
```

기본 흐름:

1. `/api/system/health`로 서버 상태 확인
2. 샘플 유저 로그인
3. `/api/payments/client-key` 조회
4. `/api/bread/nearby`에서 주문 가능한 빵 조회
5. `/api/orders/direct`로 바로 구매 주문 생성
6. stub 모드에서는 `/api/payments`로 가짜 결제 승인
7. 주문 상세 조회
8. `CONFIRMED` 주문이면 주문 취소 API 호출

토스 연동 안내 모드:

```bash
./scripts/test-order.sh --toss
```

`--toss` 모드는 실제 토스 결제를 자동 완료하지 않습니다. 주문 생성 후 `/api/payments/confirm`에 보낼 `curl` 예시를 출력합니다. 실제 confirm에는 토스 결제 인증 단계에서 발급된 `paymentKey`가 필요합니다.

현재 주의점:

- `test-order.sh`의 기본 로그인 계정은 `test-data.sql` seed 계정과 같은 `demo-user01@todaybread.com`입니다.
- 일반 토스 모드에서는 `paymentKey`가 있어야 합니다.

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
docker compose up -d
./gradlew bootRun
./scripts/test-data.sh
```

주문/결제 흐름을 stub으로 확인하려면 서버를 `stub` 프로필로 띄운 뒤 실행합니다.

```bash
SPRING_PROFILES_ACTIVE=stub ./gradlew bootRun
./scripts/test-order.sh
```
