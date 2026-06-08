# 오늘의 빵 🥖
2026 한성대학교 모바일 소프트웨어 캡스톤 오늘의 빵(TodayBread) 백엔드 리포지터리입니다.

## 개발 환경

| 항목 | 내용 |
|------|------|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.5.11 |
| 빌드 | Gradle |
| DB | MySQL 8.0.45, H2 테스트 |
| DB 마이그레이션 | Flyway |
| 인증 | Spring Security + JWT |
| API 문서 | SpringDoc OpenAPI, Swagger UI |
| 비밀번호 | Argon2 |

## 주요 문서

| 문서 | 설명 |
|------|------|
| [API.md](docs/API.md) | 전체 API 목록, 요청/응답 예시, 에러 코드 |
| [DB-SETUP.md](docs/DB-SETUP.md) | Docker MySQL, `.env`, Flyway, DB 클라이언트 연결 |
| [SCRIPTS.md](docs/SCRIPTS.md) | seed 데이터, demo 데이터, 스크립트 실행 규칙 |
| [TOSS.md](docs/TOSS.md) | 토스 페이먼츠 결제 흐름, 키 관리, confirm/cancel 구조 |
| [JWT-GUIDE.md](docs/JWT-GUIDE.md) | JWT 인증 엔드포인트 작성 패턴 |
| [CONVENTION.md](docs/CONVENTION.md) | 코드, API, 예외, 협업 컨벤션 |

## 프로젝트 구조

- 각 도메인은 `controller`, `dto`, `entity`, `repository`, `service` 중심의 레이어 구조를 따릅니다.

```text
.
├── docker-compose.yml # 개발 과정에서 DB 연동을 위한 도커 설정 파일
├── .env.example # 로컬 개발 및 테스트 시 환경 변수 파일
├── .env.ec2.example # EC2 배포 시 환경 변수 파일
├── docs/ # 각종 프로젝트 문서
├── scripts/ # 스크립트 디렉터리
├── src/main/java/com/todaybread/server/ # 소스 코드
├── src/main/resources/ # 프로젝트 설정 디렉터리
│   ├── application.properties
│   ├── application-local.properties
│   ├── application-ec2.properties
│   └── db/migration/ # Flyway DB 마이그레이션 디렉터리
└── src/test/ # 테스트 코드
```

## 로컬 환경 빠른 시작

- 로컬 실행은 `.env.example`을 복사해 `.env`를 만들고, Docker MySQL과 Spring Boot에 같은 환경변수를 주입하는 방식입니다.
- 현재 properties에는 기본값을 두지 않으므로 `.env`의 모든 환경변수는 ***서버 실행 전에 반드시 설정***해야 합니다.

### 1. env 파일 생성

```bash
cp .env.example .env
```

### 2. env 파일 수정

`.env` 파일을 열고 로컬에서 사용할 값을 채웁니다.
외부 연동을 사용하지 않는 경우에도 Spring placeholder 해결을 위해 값 자체는 필요합니다.
모든 .env 파일의 값은 필수이므로, 토스, 국세청, FCM을 위한 값을 모두 가져오세요.

```text
# ===========================
# Toss Payments
# ===========================
TOSS_SECRET_KEY=토스_시크릿_키
TOSS_CLIENT_KEY=토스_클라이언트_키

# ===========================
# FCM Push Notification
# ===========================
FCM_ENABLED=true
GOOGLE_APPLICATION_CREDENTIALS=FCM_json_파일_절대_경로

# ===========================
# NTS Service
# ===========================
NTS_BUSINESS_SERVICE_KEY=국세청_API_시크릿_키
```

JWT와 사업자 승인 해시 키는 32바이트 랜덤값을 사용합니다.

```bash
openssl rand -hex 32
```

생성한 값을 `.env`에 넣습니다.

```text
# ===========================
# JWT
# ===========================
JWT_SECRET=위_명령어로_생성된_키값

# ===========================
# Business Approval
# ===========================
BUSINESS_APPROVAL_HASH_SECRET=위_명령어로_생성된_키값
```

### 3. Docker MySQL 실행

```bash
docker compose up -d
```

### 4. 환경변수 주입 후 서버 실행

```bash
set -a
source .env
set +a

./gradlew bootRun
```

서버 실행 후 Swagger UI에서 API를 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui/index.html
```

### 5. (선택) 테스트 데이터 삽입 및 DB 연결

서버를 실행 후, 테스트 데이터가 필요하면 해당 스크립트를 실행합니다.

```bash
./scripts/local-test-data.sh
```

MySQL CLI 접속은 아래 스크립트를 사용합니다.

```bash
./scripts/local-mysql-connect.sh
```

위 스크립트들은 `.env` 파일을 직접 읽지는 않지만, 로컬 기본 DB 값과 맞는 내부 기본값을 가지고 있습니다.
`.env`에서 DB 이름, 계정, 비밀번호, 로컬 업로드 경로를 바꿨다면 해당 터미널에서도 `source .env` 후 실행합니다.

## 설정 구조

| 파일 | 역할 |
|------|------|
| `.env.example` | 로컬 개발용 환경변수 예시 |
| `.env.ec2.example` | EC2 배포용 환경변수 예시 |
| `application.properties` | 공통 Spring 설정 |
| `application-local.properties` | 로컬 Docker MySQL, JWT, 로컬 업로드 경로 설정 |
| `application-ec2.properties` | EC2/RDS, JWT, S3 이미지 저장 설정 |
| `application-test.properties` | 테스트용 H2 설정 |

Spring Boot는 `.env` 파일을 자동으로 읽지 않습니다. 터미널에서는 `set -a && source .env && set +a`로 export합니다.
IntelliJ에서는 EnvFile 플러그인 또는 Run Configuration의 Environment variables에 등록합니다. 값이 하나라도 빠지면 서버가 시작 단계에서 실패할 수 있습니다.

## 자주 쓰는 명령

```bash
./gradlew test
docker compose ps
./scripts/local-mysql-connect.sh -e "SHOW TABLES;"
./scripts/local-test-data.sh
```

DB를 완전히 초기화해야 하면 Docker 볼륨까지 제거합니다.

```bash
docker compose down -v
docker compose up -d
```

## 이용 안내

본 저장소는 캡스톤 디자인 및 포트폴리오 목적으로 공개되었습니다.
상업적 이용은 하지 말아 주세요.
