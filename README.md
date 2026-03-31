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

# 3. MySQL 접속이 필요하면 프로젝트 스크립트 사용
#    utf8mb4로 접속해서 터미널에서도 한글이 깨지지 않음
./scripts/mysql-connect.sh

# 4. Spring Boot 실행 (IntelliJ 또는 터미널)
./gradlew bootRun
```

> Swagger UI: http://localhost:8080/swagger-ui/index.html

---

## 문서

| 문서 | 설명 |
|------|------|
| [API 문서](docs/API.md) | 전체 API 목록, 인증 구조, 에러 코드 |
| [DB 환경 설정 가이드](docs/DB-SETUP.md) | Docker, .env, Flyway, Spring Boot 연결 설정 |
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
│   └── mysql-connect.sh                   # utf8mb4로 MySQL CLI 접속
├── src/main/java/com/todaybread/server/
│   ├── ServerApplication.java             # Spring Boot 시작점
│   ├── config/
│   │   ├── OpenApiConfig.java             # Swagger 설정
│   │   ├── SecurityConfig.java            # Spring Security 설정
│   │   ├── WebConfig.java                 # CORS 등 웹 설정
│   │   └── jwt/
│   │       ├── JwtRoleHelper.java         # JWT에서 userId/role 추출
│   │       └── JwtTokenService.java       # JWT 발급/검증
│   ├── domain/
│   │   ├── auth/                          # 인증/토큰 도메인
│   │   ├── bread/                         # 빵(메뉴) 도메인
│   │   ├── keyword/                       # 키워드 도메인
│   │   ├── store/                         # 매장/이미지 도메인
│   │   └── user/                          # 사용자 도메인
│   ├── global/
│   │   ├── entity/BaseEntity.java         # 공통 엔티티 (createdAt, updatedAt)
│   │   ├── exception/
│   │   │   ├── CustomException.java       # 비즈니스 예외
│   │   │   ├── ErrorCode.java             # 에러 코드 enum
│   │   │   ├── ErrorResponse.java         # 에러 응답 DTO
│   │   │   └── GlobalExceptionHandler.java # 전역 예외 핸들러
│   │   └── storage/
│   │       ├── FileStorage.java           # 파일 저장소 인터페이스
│   │       └── LocalFileStorage.java      # 로컬 파일 저장소 구현
│   └── system/
│       └── HealthController.java          # 헬스 체크 API
└── src/main/resources/
    ├── application.properties             # 애플리케이션 설정
    └── db/migration/                      # Flyway 마이그레이션 SQL
        ├── V1__init.sql
        ├── V2__create_refresh_tokens.sql
        ├── V3__create_key_word.sql
        ├── V4__create_store.sql
        ├── V5__create_store_image.sql
        └── V6__create_stock.sql
```

### 도메인별 구조

각 도메인은 동일한 레이어드 패턴을 따릅니다:

```
domain/{name}/
├── controller/    # REST API 엔드포인트
├── dto/           # 요청/응답 DTO (record 타입)
├── entity/        # JPA 엔티티
├── repository/    # Spring Data JPA 리포지터리
└── service/       # 비즈니스 로직
```

---

## DB 마이그레이션

| 파일 | 설명 |
|------|------|
| `V1__init.sql` | users 테이블 |
| `V2__create_refresh_tokens.sql` | refresh_tokens 테이블 |
| `V3__create_key_word.sql` | keyword, user_keyword 테이블 |
| `V4__create_store.sql` | store, favourite_store 테이블 + 위도/경도 인덱스 |
| `V5__create_store_image.sql` | store_image 테이블 |
| `V6__create_stock.sql` | bread, bread_image 테이블 |
