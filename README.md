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
| Config | Properties |
| DB | MySQL 8.0.45 (Docker) |
| Migration | Flyway |

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
>
> 터미널에서 MySQL에 직접 들어갈 때는 `./scripts/mysql-connect.sh`를 사용하세요.
> 내부적으로 `mysql --default-character-set=utf8mb4`로 접속해서 조회 결과도 한글이 정상 출력됩니다.

---

## 문서

| 문서 | 설명 |
|------|------|
| [DB 환경 설정 가이드](DB-SETUP.md) | Docker, .env, Flyway, Spring Boot 연결 설정 |
| [컨벤션](CONVENTION.md) | 코드 컨벤션, 협업 컨벤션, 에러 코드 규격 |

---

## 프로젝트 구조

```
.
├── docker-compose.yml                     # MySQL Docker 실행 설정
├── build.gradle                           # Gradle 빌드 설정
├── scripts/
│   └── mysql-connect.sh                   # utf8mb4로 MySQL CLI 접속
├── src/main/java/com/todaybread/server/
│   ├── config/                            # 설정 (Swagger, Security, JWT)
│   ├── domain/
│   │   ├── auth/                          # 인증/토큰 도메인
│   │   ├── keyword/                       # 키워드 도메인
│   │   ├── store/                         # 매장/이미지 도메인
│   │   └── user/                          # 사용자 도메인
│   ├── global/
│   │   ├── entity/                        # 공통 엔티티 베이스
│   │   ├── exception/                     # 공통 예외 처리
│   │   └── storage/                       # 파일 저장소 추상화/구현
│   ├── system/                            # 시스템 API (Health Check)
│   └── ServerApplication.java             # Spring Boot 시작점
└── src/main/resources/
    ├── application.properties             # 애플리케이션 설정
    └── db/migration/                      # Flyway 마이그레이션 SQL
```
