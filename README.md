# TodayBread

한성대학교 캡스톤 프로젝트 — TodayBread 백엔드 서버

---

## 개발 환경

| 항목 | 버전 |
|------|------|
| Language | Java 21 LTS |
| Framework | Spring Boot 3.5.11 |
| Build | Gradle (Groovy) |
| Packaging | Jar |
| DB | MySQL 8.0.45 (Docker) |
| Migration | Flyway |
| Auth | Spring Security + JWT (HMAC) |
| API 문서 | SpringDoc OpenAPI (Swagger UI) |
| 비밀번호 | Argon2 (BouncyCastle) |

---

## 빠른 시작

```bash
# 1. 리포지터리 클론
git clone {repository-url}

# 2. Docker 컨테이너 시작 (.env 없이도 기본값으로 동작)
docker compose up -d

# 3. 프론트 연동용 테스트 데이터 삽입 (선택)
#    샘플 유저/사장님/가게/영업시간/빵 데이터를 넣습니다.
./scripts/test-data.sh

# 4. MySQL 접속이 필요하면 프로젝트 스크립트 사용
#    utf8mb4로 접속해서 터미널에서도 한글이 깨지지 않음
./scripts/mysql-connect.sh

# 5. Spring Boot 실행 (IntelliJ 또는 터미널)
./gradlew bootRun
```

> Swagger UI: http://localhost:8080/swagger-ui/index.html

---

## 개발용 테스트 데이터

프론트 연동이나 Swagger/Postman 수동 확인용 샘플 데이터가 필요하면 아래 스크립트를 실행하세요.

```bash
./scripts/test-data.sh
```

샘플 계정:

- `demo-user@todaybread.local` / `todaybread123`
- `demo-boss-gangnam@todaybread.local` / `todaybread123`
- `demo-boss-seolleung@todaybread.local` / `todaybread123`

추천 근처 조회 좌표:

- `lat=37.4980950`
- `lng=127.0276100`
- `radius=3`

> Docker 볼륨(`mysql_data`)은 재사용되므로, 테스트 데이터가 꼬였을 때는 `./scripts/test-data.sh`를 다시 실행하면 됩니다.
> 스키마 자체가 꼬였거나 레거시 컬럼이 남아 있으면 `docker compose down -v`로 볼륨까지 초기화한 뒤 다시 올리세요.

---

## 문서

| 문서 | 설명 |
|------|------|
| [API 문서](docs/API.md) | 전체 API 목록, 인증 구조, 프론트 연동용 샘플 계정 |
| [DB 환경 설정 가이드](docs/DB-SETUP.md) | Docker, .env, Flyway, 테스트 데이터 스크립트 사용법 |
| [컨벤션](docs/CONVENTION.md) | 코드 컨벤션, 협업 컨벤션, 에러 코드 규격 |
| [JWT 가이드 문서](docs/JWT-GUIDE.md) | JWT 토큰 발급/검증/재발급 흐름 |

---

## 프로젝트 구조

```
.
├── docker-compose.yml                     # MySQL Docker 실행 설정
├── build.gradle                           # Gradle 빌드 설정
├── docs/                                  # 프로젝트 문서
├── scripts/
│   ├── mysql-connect.sh                   # utf8mb4로 MySQL CLI 접속
│   ├── test-data.sh                       # 개발용 테스트 데이터 주입 스크립트
│   └── test-data.sql                      # 프론트 연동용 샘플 데이터 SQL
├── src/main/java/com/todaybread/server/
│   ├── ServerApplication.java             # Spring Boot 시작점 + Clock 빈 등록
│   ├── config/
│   │   ├── OpenApiConfig.java             # Swagger 설정
│   │   ├── SecurityConfig.java            # Spring Security 설정
│   │   ├── WebConfig.java                 # 정적 리소스 매핑 (/images/**)
│   │   └── jwt/
│   │       └── JwtTokenService.java       # JWT 발급/검증 (Access + Refresh)
│   ├── domain/
│   │   ├── auth/                          # 인증/토큰 도메인
│   │   ├── bread/                         # 빵(메뉴) 도메인
│   │   ├── keyword/                       # 키워드 도메인
│   │   ├── store/                         # 매장/이미지/영업시간/단골 도메인
│   │   ├── user/                          # 사용자 도메인
│   │   └── wishlist/                      # 찜목록 통합 조회
│   ├── global/
│   │   ├── entity/BaseEntity.java         # 공통 엔티티 (createdAt, updatedAt)
│   │   ├── exception/
│   │   │   ├── CustomException.java       # 비즈니스 예외
│   │   │   ├── ErrorCode.java             # 에러 코드 enum
│   │   │   ├── ErrorResponse.java         # 에러 응답 DTO
│   │   │   └── GlobalExceptionHandler.java # 전역 예외 핸들러
│   │   ├── storage/
│   │   │   ├── FileStorage.java           # 파일 저장소 인터페이스
│   │   │   ├── ImageValidationHelper.java # 이미지 검증 헬퍼
│   │   │   └── LocalFileStorage.java      # 로컬 파일 저장소 구현
│   │   └── util/
│   │       └── JwtRoleHelper.java         # JWT에서 userId/role 추출
│   └── system/
│       └── HealthController.java          # 헬스 체크 API
└── src/main/resources/
    ├── application.properties             # 애플리케이션 설정
    └── db/migration/
        └── V1__init_schema.sql            # 전체 스키마 (통합 마이그레이션)
```

### 도메인별 구조

각 도메인은 동일한 레이어드 패턴을 따릅니다:

```
domain/{name}/
├── controller/    # REST API 엔드포인트
├── dto/           # 요청/응답 DTO (record 타입)
├── entity/        # JPA 엔티티
├── repository/    # Spring Data JPA 리포지터리
├── service/       # 비즈니스 로직
└── util/          # 유틸리티 (해당 도메인에만 존재)
```

---

## DB 마이그레이션

기존 V1~V12 마이그레이션을 단일 파일로 통합했습니다.

| 파일 | 설명 |
|------|------|
| `V1__init_schema.sql` | 전체 스키마 (users, refresh_token, keyword, user_keyword, store, favourite_store, store_image, store_business_hours, bread, bread_image) |

### 테이블 목록

| 테이블 | 설명 |
|--------|------|
| `users` | 사용자 정보 |
| `refresh_token` | JWT Refresh Token (해시 저장) |
| `keyword` | 정규화된 키워드 마스터 |
| `user_keyword` | 사용자-키워드 M:N 관계 |
| `store` | 가게 정보 (위치, 활성 상태) |
| `favourite_store` | 단골 가게 (유저-가게 관계) |
| `store_image` | 가게 이미지 (최대 5장) |
| `store_business_hours` | 요일별 영업시간 (가게당 7개) |
| `bread` | 빵 메뉴 (가격, 재고) |
| `bread_image` | 빵 이미지 (메뉴당 1장) |
