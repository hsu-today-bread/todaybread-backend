# TodayBread

한성대학교 캡스톤 프로젝트 TodayBread 백엔드 서버입니다.

## 개발 환경

| 항목 | 내용 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.11 |
| Build | Gradle |
| DB | MySQL 8.0.45, H2 테스트 |
| Migration | Flyway |
| Auth | Spring Security + JWT |
| API Docs | SpringDoc OpenAPI, Swagger UI |
| Password | Argon2 |
| Payment | Toss Payments, local stub profile |

## 빠른 시작

`.env` 없이도 Docker Compose와 Spring Boot가 기본값으로 동작합니다.

```bash
docker compose up -d
./gradlew bootRun
```

서버 실행 후 Swagger UI에서 API를 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui/index.html
```

개발용 seed 데이터가 필요하면, 서버를 한 번 실행해서 Flyway가 테이블을 만든 뒤 실행합니다.

```bash
./scripts/test-data.sh
```

MySQL CLI 접속은 아래 스크립트를 사용합니다.

```bash
./scripts/mysql-connect.sh
```

## 테스트 데이터

`./scripts/test-data.sh`는 `scripts/test-data.sql`을 적용하고 `uploads/`에 seed 이미지를 준비합니다. `scripts/seed-images/`에 실제 이미지가 있으면 해당 파일을 사용하고, 없으면 SVG placeholder를 생성합니다.

샘플 계정:

| 역할 | 계정 | 비밀번호 |
|------|------|----------|
| 일반 유저 | `demo-user@todaybread.com` | `todaybread123` |
| 사장님 | `demo-boss1@todaybread.com` ~ `demo-boss20@todaybread.com` | `todaybread123` |

근처 매장/빵 조회 추천 좌표:

```text
Gangnam: lat=37.4980950, lng=127.0276100, radius=5
Hansung Univ: lat=37.5826000, lng=127.0106000, radius=2
```

토큰은 seed하지 않습니다. 로그인 API가 access token과 refresh token을 발급하고, refresh token은 DB에 해시로 저장합니다.

## 주문/결제 테스트

stub 결제 모드에서는 토스 키 없이 주문 생성부터 결제, 주문 취소까지 확인할 수 있습니다.

```bash
SPRING_PROFILES_ACTIVE=stub ./gradlew bootRun
./scripts/test-order.sh
```

토스 연동 모드는 주문 생성 후 confirm API 호출용 `curl` 예시를 출력합니다. 실제 승인에는 프론트엔드 토스 SDK에서 받은 `paymentKey`가 필요합니다.

```bash
./scripts/test-order.sh --toss
```

토스 테스트 키는 환경 변수로 설정합니다.

```bash
TOSS_SECRET_KEY=test_sk_...
TOSS_CLIENT_KEY=test_ck_...
./gradlew bootRun
```

## 주요 문서

| 문서 | 설명 |
|------|------|
| [API.md](docs/API.md) | 전체 API 목록, 요청/응답 예시, 에러 코드 |
| [DB-SETUP.md](docs/DB-SETUP.md) | Docker MySQL, `.env`, Flyway, DBeaver/TablePlus 연결 |
| [SCRIPTS.md](docs/SCRIPTS.md) | `scripts/` 폴더의 실행 스크립트와 seed 이미지 규칙 |
| [TOSS.md](docs/TOSS.md) | 토스 페이먼츠 결제 흐름, 키 관리, confirm/cancel 구조 |
| [JWT-GUIDE.md](docs/JWT-GUIDE.md) | JWT 인증 엔드포인트 작성 패턴 |
| [CONVENTION.md](docs/CONVENTION.md) | 코드, API, 예외, 협업 컨벤션 |

## 프로젝트 구조

```text
.
├── docker-compose.yml
├── build.gradle
├── docs/
├── scripts/
│   ├── mysql-connect.sh
│   ├── test-data.sh
│   ├── test-data.sql
│   ├── test-order.sh
│   └── seed-images/
├── uploads/
├── src/main/java/com/todaybread/server/
│   ├── ServerApplication.java
│   ├── config/
│   ├── domain/
│   │   ├── auth/
│   │   ├── bread/
│   │   ├── cart/
│   │   ├── keyword/
│   │   ├── order/
│   │   ├── payment/
│   │   ├── review/
│   │   ├── store/
│   │   ├── user/
│   │   └── wishlist/
│   ├── global/
│   └── system/
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/
│       └── V1__init_schema.sql
└── src/test/
```

각 도메인은 대체로 아래 레이어를 따릅니다.

```text
domain/{name}/
├── controller/
├── dto/
├── entity/
├── repository/
├── service/
└── util/
```

## DB 마이그레이션

Flyway 마이그레이션은 단일 baseline 파일 하나로 전체 스키마를 생성합니다.

| 파일 | 설명 |
|------|------|
| `V1__init_schema.sql` | 전체 스키마 (테이블, 인덱스, FK, CHECK 제약 포함) |

주요 테이블:

| 테이블 | 설명 |
|--------|------|
| `users` | 사용자 정보 |
| `refresh_token` | JWT refresh token 해시 |
| `password_reset_token` | 비밀번호 재설정 일회용 토큰 (10분 유효) |
| `keyword`, `user_keyword` | 키워드 마스터와 사용자 키워드 |
| `store`, `store_image`, `store_business_hours`, `favourite_store` | 매장, 이미지, 영업시간, 단골 매장 |
| `bread`, `bread_image` | 빵 메뉴와 이미지 (soft delete 지원) |
| `cart`, `cart_item` | 장바구니 (단일 매장 제약) |
| `orders`, `order_item` | 주문과 주문 항목 (멱등성 키, 상태 머신) |
| `payment` | 결제 승인/취소 정보 (토스 페이먼츠 연동) |
| `review`, `review_image` | 리뷰와 리뷰 이미지 |

기존 로컬 DB에 오래된 Flyway 이력(V1~V11 분리 시절)이 남아 있으면 baseline과 맞지 않을 수 있습니다. 개발 DB를 새 스키마로 맞추려면 볼륨을 초기화합니다.

```bash
docker compose down -v
docker compose up -d
./gradlew bootRun
./scripts/test-data.sh
```

## 자주 쓰는 명령

```bash
./gradlew test
docker compose ps
./scripts/mysql-connect.sh -e "SHOW TABLES;"
./scripts/test-data.sh
SPRING_PROFILES_ACTIVE=stub ./gradlew bootRun
./scripts/test-order.sh
```
