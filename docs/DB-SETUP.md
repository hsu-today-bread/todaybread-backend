# DB 환경 설정 가이드

TodayBread 로컬 개발 DB는 Docker Compose로 MySQL 컨테이너를 띄우고, Spring Boot는 `local` 프로필로 해당 DB에 접속합니다.

현재 설정은 의도적으로 **프로퍼티 기본값을 두지 않습니다.** 로컬 실행도 `.env` 또는 IntelliJ 환경변수 설정이 필요합니다.
DB, JWT, Toss, FCM, NTS 같은 환경변수는 서버 시작 전에 모두 존재해야 합니다. EC2 프로필에서는 S3 환경변수도 필요합니다.

## 전체 구조

```text
.env.example                커밋되는 로컬 환경변수 예시
.env                        로컬 실제 환경변수 파일, 커밋 금지
.env.ec2.example            커밋되는 EC2 환경변수 예시

docker-compose.yml          .env를 자동으로 읽어 MySQL 컨테이너 생성
application.properties      공통 Spring 설정
application-local.properties local 프로필 DB/JWT/로컬 업로드 설정
application-ec2.properties  ec2 프로필 RDS/JWT/S3 설정
```

Docker Compose는 프로젝트 루트의 `.env` 파일을 자동으로 읽습니다. Spring Boot는 `.env` 파일을 자동으로 읽지 않으므로, 터미널에서 export하거나 IntelliJ Run Configuration/EnvFile 플러그인으로 주입해야 합니다.

## 빠른 시작

```bash
cp .env.example .env
docker compose up -d

set -a
source .env
set +a

./gradlew bootRun
```

서버가 한 번 뜨면 Flyway가 `src/main/resources/db/migration`의 SQL을 실행해 스키마를 생성합니다. 개발용 seed 데이터가 필요하면 서버가 뜬 뒤 다른 터미널에서 실행합니다.

```bash
./scripts/local-test-data.sh
```

## 로컬 환경변수

`.env.example`을 복사해 `.env`를 만듭니다.

```bash
cp .env.example .env
```

로컬에서 필요한 주요 값은 다음과 같습니다. `.env.example`에 있는 값은 모두 채우는 것을 전제로 합니다.

| 변수 | 용도 |
|------|------|
| `SPRING_PROFILES_ACTIVE=local` | Spring Boot가 `application-local.properties`를 읽도록 지정 |
| `MYSQL_HOST=localhost` | 로컬 Docker MySQL 접속 호스트 |
| `MYSQL_PORT=3306` | 로컬 Docker MySQL 포트 |
| `MYSQL_DATABASE` | Docker가 생성하고 Spring Boot가 접속할 DB 이름 |
| `MYSQL_USER` | Docker가 생성하고 Spring Boot가 사용할 DB 사용자 |
| `MYSQL_PASSWORD` | DB 사용자 비밀번호 |
| `MYSQL_ROOT_PASSWORD` | Docker MySQL root 비밀번호, Spring Boot는 사용하지 않음 |
| `UPLOAD_DIR` | 로컬 이미지 업로드 저장 경로 |
| `JWT_SECRET` | 로컬 JWT 서명 키 |
| `TOSS_SECRET_KEY` | 토스 API 호출용 Secret Key |
| `TOSS_CLIENT_KEY` | 프론트엔드 토스 SDK 초기화용 Client Key |
| `FCM_ENABLED` | FCM 알림 기능 활성화 여부 |
| `GOOGLE_APPLICATION_CREDENTIALS` | FCM 서비스 계정 JSON 경로 |
| `NTS_BUSINESS_SERVICE_KEY` | 국세청 사업자 진위확인 API service key |
| `BUSINESS_APPROVAL_HASH_SECRET` | 사업자 승인 정보 해시 키 |

`.env`는 `.gitignore` 대상입니다. 실제 비밀번호와 키는 커밋하지 않습니다.
외부 연동을 사용하지 않는 경우에도 Spring placeholder 해결을 위해 값 자체는 존재해야 합니다.

## IntelliJ 실행

IntelliJ에서 실행할 때는 두 가지 중 하나를 사용합니다.

1. EnvFile 플러그인 사용
   - Run Configuration에서 `ServerApplication` 선택
   - EnvFile 활성화
   - 프로젝트 루트의 `.env` 추가
   - Active profiles는 비워도 `.env`의 `SPRING_PROFILES_ACTIVE=local`이 적용됩니다.

2. 직접 환경변수 등록
   - Run Configuration의 Environment variables에 `.env` 값을 직접 입력
   - Active profiles에 `local` 입력

`.env` 파일만 만들어두고 IntelliJ에 연결하지 않으면 Spring Boot는 해당 값을 읽지 않습니다.

## 터미널 실행

`.env`는 `KEY=value` 형식이므로, Spring Boot가 읽을 수 있게 환경변수로 export해야 합니다.

```bash
set -a
source .env
set +a
./gradlew bootRun
```

`source .env`만 실행하면 shell 변수로만 남을 수 있습니다. `set -a`를 함께 쓰면 source로 읽은 값을 자동 export합니다.

## Docker Compose

현재 `docker-compose.yml`은 `.env` 값을 그대로 사용합니다.

```yaml
environment:
  MYSQL_DATABASE: ${MYSQL_DATABASE}
  MYSQL_USER: ${MYSQL_USER}
  MYSQL_PASSWORD: ${MYSQL_PASSWORD}
  MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
ports:
  - "${MYSQL_PORT}:3306"
```

`.env`가 없거나 필수 변수가 빠지면 Docker Compose 실행도 실패하거나 의도하지 않은 빈 값으로 동작할 수 있습니다. 로컬 개발을 시작하기 전에 반드시 `.env`를 준비하세요.

## 프로필별 설정

| 파일 | 역할 |
|------|------|
| `application.properties` | 공통 설정: 애플리케이션 이름, Swagger, Flyway, JWT 만료 시간, multipart 제한, 외부 API base-url |
| `application-local.properties` | 로컬 DB 접속, SQL 로그 활성화, JWT secret, `UPLOAD_DIR` |
| `application-ec2.properties` | RDS 접속, SQL 로그 비활성화, JWT secret, S3 bucket/region |
| `application-test.properties` | 테스트용 H2, 테스트 JWT, 테스트 업로드 경로 |

`application-local.properties`와 `application-ec2.properties`는 모두 `${ENV_VAR}`만 사용합니다. `${ENV_VAR:default}` 형태의 기본값은 두지 않습니다.

## EC2 / RDS 실행 개요

EC2에서는 `.env.ec2.example`을 참고해 EC2 서버 안에 실제 `/home/ubuntu/todaybread/secrets/.env.ec2`를 작성합니다.

필수 값:

```env
SPRING_PROFILES_ACTIVE=ec2
MYSQL_HOST=your-rds-endpoint.ap-northeast-2.rds.amazonaws.com
MYSQL_PORT=3306
MYSQL_DATABASE=todaybread
MYSQL_USER=your-rds-user
MYSQL_PASSWORD=your-rds-password
JWT_SECRET=replace-with-ec2-64-hex-secret
TOSS_SECRET_KEY=test_sk_your_secret_key_here
TOSS_CLIENT_KEY=test_ck_your_client_key_here
FCM_ENABLED=false
GOOGLE_APPLICATION_CREDENTIALS=/home/ubuntu/todaybread/firebase-adminsdk.json
NTS_BUSINESS_SERVICE_KEY=your_own_service_key
BUSINESS_APPROVAL_HASH_SECRET=replace-with-ec2-64-hex-secret
S3_BUCKET=todaybread-demo-images
AWS_REGION=ap-northeast-2
```

systemd를 쓰는 경우 서비스 파일에서 `EnvironmentFile`로 `.env.ec2`를 읽게 할 수 있습니다.

```ini
[Service]
EnvironmentFile=/home/ubuntu/todaybread/secrets/.env.ec2
ExecStart=/usr/bin/java -Xms256m -Xmx768m -jar /home/ubuntu/todaybread/server.jar
```

## S3 이미지 저장

EC2 프로필은 `S3FileStorage`를 사용합니다. 업로드와 삭제는 서버가 S3 권한으로 수행하고, API 응답의 `imageUrl`은 S3 public URL입니다.

EC2 실행에는 아래 값이 필요합니다.

```env
S3_BUCKET=todaybread-demo-images
AWS_REGION=ap-northeast-2
```

S3 버킷은 public read만 허용하고 public write는 허용하지 않습니다. EC2에는 `s3:PutObject`, `s3:DeleteObject`, 필요 시 `s3:GetObject` 권한을 가진 IAM Role을 붙입니다.

## 자주 쓰는 명령

```bash
cp .env.example .env
docker compose up -d
docker compose ps

set -a
source .env
set +a
./gradlew bootRun
```

다른 터미널에서:

```bash
./scripts/local-test-data.sh
./scripts/local-mysql-connect.sh
```

## DB 초기화

로컬 DB를 새 baseline으로 맞춰야 하면 Docker 볼륨까지 제거합니다.

```bash
docker compose down -v
docker compose up -d

set -a
source .env
set +a
./gradlew bootRun
```

필요하면 seed 데이터도 다시 넣습니다.

```bash
./scripts/local-test-data.sh
```
