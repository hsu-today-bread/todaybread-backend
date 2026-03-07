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

# 3. Spring Boot 실행 (IntelliJ 또는 터미널)
./gradlew bootRun
```

> Swagger UI: http://localhost:8080/swagger-ui.html

---

## 문서

| 문서 | 설명 |
|------|------|
| [DB 환경 설정 가이드](db-setup.md) | Docker, .env, Flyway, Spring Boot 연결 설정 |
| [컨벤션](CONVENTION.md) | 코드 컨벤션, 협업 컨벤션, 에러 코드 규격 |

---

## 프로젝트 구조

```
src/main/java/com/todaybread/server/
├── config/                  # 설정 (Swagger, Security)
├── domain/
│   └── user/
│       ├── controller/      # API 엔드포인트
│       ├── dto/             # 요청/응답 DTO
│       ├── entity/          # JPA 엔티티
│       ├── repository/      # DB 접근
│       └── service/         # 비즈니스 로직
├── global/
│   └── exception/           # 에러 처리 (ErrorCode, Handler)
└── system/                  # 시스템 (Health Check)
```
