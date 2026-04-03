# DB 환경 설정 가이드

- 이 문서는 TodayBread 프로젝트의 데이터베이스 환경을 처음부터 세팅하는 방법을 설명합니다.
- 이 문서 하나만 읽으면 Docker, .env, Flyway, Spring Boot 연결까지 전부 이해할 수 있습니다.
- 다소 길더라도 읽어주세요.

---

## 전체 구조 요약

```
.env (환경변수 파일, 선택사항)
  ├── docker-compose.yml이 읽음 → MySQL 컨테이너 생성
  └── application.properties가 읽음 → Spring Boot가 MySQL에 접속

.env가 없어도 양쪽 다 기본값(todaybread)으로 동작합니다.

Docker 시작 시:
  1. Docker가 docker-compose.yml 파일을 읽음
  2. MySQL 다운로드 및 이미지 파일 배포
  3. MySQL 정상 실행

Spring Boot 시작 시:
  1. Flyway가 db/migration 폴더의 SQL 파일을 자동 실행 → 테이블 생성
  2. JPA(Hibernate)가 엔티티와 테이블 매핑을 검증
  3. 서버 정상 실행

개발용 테스트 데이터가 필요하면:
  1. Spring Boot를 한 번 실행해 Flyway로 테이블 생성
  2. `./scripts/test-data.sh` 실행
  3. 샘플 유저/사장님/가게/영업시간/빵 데이터 삽입
```

---

## 사전 준비

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 설치
- 깃 허브 원격 리포지터리 클론 완료
- intelliJ를 통해 프로젝트 열기

---

## Docker란?

- Docker는 "가상 컴퓨터를 만들어주는 프로그램"입니다. 
- MySQL을 내 PC에 직접 설치하는 대신, Docker가 만든 작은 가상 환경(컨테이너) 안에 MySQL을 설치해서 돌립니다.

장점:
- 내 PC를 더럽히지 않음 (MySQL 직접 설치 불필요)
- 필요 없으면 컨테이너만 지우면 깔끔하게 사라짐
- 팀원 모두 동일한 환경에서 개발 가능 (docker-compose.yml만 공유하면 docker가 맞는 환경을 자동 세팅함)

Docker Compose는 그 가상 컴퓨터를 "설정 파일 하나로 관리"하는 도구입니다.
`docker-compose.yml`에 설정을 적어두면 `docker compose up` 한 방에 다 세팅됩니다.

---

## 볼륨(Volume)이란?

- 컨테이너는 기본적으로 삭제하면 안에 있던 데이터도 다 날아갑니다. 그래서 MySQL 데이터를 컨테이너 밖(볼륨)에 저장해둡니다.
- 다만 현재 도커 내부에 있는 데이터와 MySQL의 데이터는 서로 다른 공간에 존재합니다.
- 리눅스의 심볼릭 링크, 윈도우의 바로가기 개념처럼 둘의 공간을 docker가 서로 연결해둡니다.

```
컨테이너 안:  /var/lib/mysql  ← MySQL이 여기에 데이터를 읽고 씀
                  ↕ (마운트 연결)
내 PC:        Docker 볼륨 (mysql_data)  ← 실제 데이터가 저장되는 곳
```

- `docker compose down` → 컨테이너 삭제, 볼륨(데이터)은 살아있음
- `docker compose down -v` → 컨테이너 + 볼륨 다 삭제 → DB 완전 초기화
- 컨테이너를 다시 만들어도 볼륨을 지우지 않으면 기존 스키마/데이터가 그대로 남아 있음
- 개발용 테스트 데이터는 Docker init SQL이 아니라 `./scripts/test-data.sh`로 반복 주입하는 방식을 사용함

볼륨은 프로젝트 폴더에 있는 게 아니라 Docker가 자체적으로 관리하는 별도 경로에 있습니다.
`docker volume ls`로 목록을 확인할 수 있습니다.

---

## 1단계: .env 파일 설정 (선택사항)

### 기본값으로 쓸 경우 (가장 간단)

`.env` 파일 없이도 동작합니다.
docker-compose.yml과 application.properties 양쪽 다 기본값이 설정되어 있어서
아무 설정 없이 바로 `docker compose up -d` → Spring Boot 실행이 가능합니다.

기본값:
| 변수명 | 기본값 |
|--------|--------|
| `MYSQL_DATABASE` | `todaybread` |
| `MYSQL_USER` | `todaybread` |
| `MYSQL_PASSWORD` | `todaybread` |
| `MYSQL_ROOT_PASSWORD` | `rootpassword` |

**기본값으로 쓰는 경우**

> 기본값으로 넘어가면 어떻게 되나요?
>
> docker-compose.yml에 `${MYSQL_DATABASE:-todaybread}` 처럼 기본값이 설정되어 있어서
> `.env` 파일이 없어도 Docker가 `todaybread`라는 DB와 유저를 자동으로 만듭니다.
> application.properties도 마찬가지로 `${MYSQL_USER:todaybread}` 기본값이 있어서
> Spring Boot가 `todaybread` 계정으로 접속합니다.
>
> 즉, 양쪽 다 같은 기본값을 쓰기 때문에 아무 설정 없이도 정상 동작합니다.
> 2단계(환경변수 등록)도 건너뛰고 바로 5단계(Docker 실행)로 가면 됩니다.

### 값을 커스텀하고 싶을 경우

프로젝트 루트에 `.env.example` 파일이 있습니다.
이 파일을 복사해서 `.env`를 만들고 값을 수정하세요.

```bash
cp .env.example .env
```

`.env` 파일 내용:
```env
MYSQL_DATABASE=todaybread
MYSQL_USER=todaybread
MYSQL_PASSWORD=todaybread
MYSQL_ROOT_PASSWORD=rootpassword
```

> `.env` 파일은 `.gitignore`에 포함되어 있어서 Git에 올라가지 않습니다.
> 따라서 팀원마다 다른 값을 써도 서로 영향 없습니다.

### 각 변수가 하는 일

| 변수명 | 설명 | 누가 사용하나 |
|--------|------|--------------|
| `MYSQL_DATABASE` | Docker가 컨테이너 처음 만들 때 자동으로 생성할 데이터베이스 이름 | Docker + Spring Boot |
| `MYSQL_USER` | Docker가 자동으로 만들어주는 MySQL 사용자 계정 이름. Spring Boot가 이 계정으로 DB에 접속함 | Docker + Spring Boot |
| `MYSQL_PASSWORD` | 위 사용자 계정의 비밀번호. Spring Boot가 DB 접속 시 이 비밀번호를 사용함 | Docker + Spring Boot |
| `MYSQL_ROOT_PASSWORD` | MySQL root(최고 관리자) 계정의 비밀번호. Docker가 컨테이너 초기화할 때만 사용하고, Spring Boot에서는 사용하지 않음 | Docker만 |

> root 계정 vs 일반 계정:
> - root → MySQL 관리자 (DB 생성/삭제, 유저 관리 등). 앱에서는 사용하지 않음
> - MYSQL_USER → 앱이 실제로 쓰는 계정 (데이터 읽기/쓰기만)
> - 앱이 root로 접속하면 보안상 위험하므로 일반 계정을 사용합니다

---

## 2단계: .env 값을 커스텀했을 때 Spring Boot에 반영하는 방법

** 1단계에서 기본값으로 쓰면 이 단계는 건너뛰세요.**

`.env`에서 값을 바꿨다면 Spring Boot도 그 값을 알아야 합니다.
Spring Boot는 `.env` 파일을 자동으로 읽지 않기 때문에 아래 방법 중 하나를 선택하세요.

### 방법 A: IntelliJ에서 환경변수 등록

1. 상단 메뉴 → Run → Edit Configurations
2. ServerApplication 선택
3. Environment variables 항목 옆의 아이콘 클릭
4. `.env` 파일 경로를 지정하거나, 변수를 직접 입력
5. Apply → OK

> IntelliJ의 EnvFile 플러그인을 설치하면 `.env` 파일을 바로 읽을 수 있어서 더 편합니다.

### 방법 B: 터미널에서 실행

```bash
source .env
./gradlew bootRun
```

`source .env`는 현재 터미널 세션에만 유효합니다. 새 터미널을 열면 다시 해야 합니다.

> 왜 이런 과정이 필요한가요?
>
> Docker Compose는 같은 디렉토리의 `.env` 파일을 자동으로 읽습니다.
> 하지만 Spring Boot는 자동으로 읽지 않습니다.
>
> application.properties에 기본값이 설정되어 있어서:
> ```
> ${MYSQL_USER:todaybread}
>          ↑ 환경변수    ↑ 기본값
> ```
> - 환경변수가 있으면 → 그 값 사용
> - 환경변수가 없으면 → 기본값 `todaybread` 사용
>
> 기본값을 그대로 쓰면 환경변수 등록이 필요 없습니다.
> `.env`에서 값을 바꿨는데 Spring Boot에 등록 안 하면,
> Docker는 바꾼 값으로 DB를 만들고 Spring Boot는 기본값으로 접속하려 해서 연결이 실패합니다.

---

## 3단계: .env와 각 파일의 연결 구조

`.env`의 변수들은 `docker-compose.yml`과 `application.properties` 양쪽에서 참조합니다.
하나의 `.env` 파일로 Docker와 Spring Boot 설정을 동시에 관리하는 구조입니다.
양쪽 다 기본값이 있어서 `.env` 없이도 동작합니다.

### docker-compose.yml에서 읽는 방식

```yaml
environment:
  MYSQL_DATABASE: ${MYSQL_DATABASE:-todaybread}
  MYSQL_USER: ${MYSQL_USER:-todaybread}
  MYSQL_PASSWORD: ${MYSQL_PASSWORD:-todaybread}
  MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-rootpassword}
```

`${변수명:-기본값}` 형식입니다.
- `.env`에 값이 있으면 → 그 값 사용
- `.env`가 없거나 변수가 없으면 → `:-` 뒤의 기본값 사용

### application.properties에서 읽는 방식

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/${MYSQL_DATABASE:todaybread}?...
spring.datasource.username=${MYSQL_USER:todaybread}
spring.datasource.password=${MYSQL_PASSWORD:todaybread}
```

Spring Boot는 `${변수명:기본값}` 형식을 사용합니다. (하이픈 없음)
- 환경변수가 설정되어 있으면 → 그 값 사용
- 설정되어 있지 않으면 → `:` 뒤의 기본값 사용

### 전체 흐름 정리

```
.env 파일 (선택사항)
  MYSQL_DATABASE=todaybread
  MYSQL_USER=todaybread
  MYSQL_PASSWORD=todaybread
  MYSQL_ROOT_PASSWORD=rootpassword
       │
       ├──→ docker-compose.yml
       │      ${MYSQL_DATABASE:-todaybread} → "todaybread" DB 생성
       │      ${MYSQL_USER:-todaybread} → "todaybread" 유저 생성
       │      ${MYSQL_PASSWORD:-todaybread} → 유저 비밀번호 설정
       │      ${MYSQL_ROOT_PASSWORD:-rootpassword} → root 비밀번호 설정
       │
       └──→ application.properties (환경변수로 등록한 경우)
              ${MYSQL_DATABASE:todaybread} → "todaybread" DB에 접속
              ${MYSQL_USER:todaybread} → "todaybread" 유저로 접속
              ${MYSQL_PASSWORD:todaybread} → 비밀번호로 접속

.env가 없으면 양쪽 다 기본값(todaybread)으로 동작
```

---

## 4단계: docker-compose.yml 상세 설명

```yaml
services:
  mysql:
    image: mysql:8.0.45
```
Docker Hub에서 MySQL 8.0.45 이미지를 가져옵니다.

```yaml
    container_name: todaybread-mysql
```
컨테이너 이름을 지정합니다. `docker ps`나 `docker exec`에서 이 이름으로 컨테이너를 찾습니다.

```yaml
    restart: unless-stopped
```
컨테이너가 예기치 않게 죽으면 자동으로 재시작합니다. 수동으로 `docker compose stop`한 경우에는 재시작하지 않습니다.

```yaml
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE:-todaybread}
      MYSQL_USER: ${MYSQL_USER:-todaybread}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-todaybread}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-rootpassword}
      TZ: Asia/Seoul
```
MySQL 컨테이너에 전달할 환경변수입니다. `.env`에서 값을 읽고, 없으면 기본값을 사용합니다.
Docker가 컨테이너를 처음 만들 때 이 값들로 DB와 유저를 자동 생성합니다.
`TZ`는 컨테이너의 타임존을 서울로 설정합니다.

```yaml
    ports:
      - "3306:3306"
```
호스트(내 PC)의 3306 포트를 컨테이너의 3306 포트에 연결합니다.
이래야 `localhost:3306`으로 MySQL에 접속할 수 있습니다.

```yaml
    volumes:
      - mysql_data:/var/lib/mysql
```
컨테이너 안의 MySQL 데이터 경로(`/var/lib/mysql`)를 Docker 볼륨(`mysql_data`)에 연결합니다.
컨테이너를 삭제해도 볼륨에 데이터가 남아있어서 다시 `up` 하면 데이터가 그대로입니다.

```yaml
    command: >-
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
```
MySQL 시작 시 문자 인코딩을 utf8mb4로 설정합니다.
한글, 이모지 등 모든 유니코드 문자를 지원합니다.

```yaml
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 ..."]
      interval: 5s
      timeout: 3s
      retries: 20
```
5초마다 MySQL이 정상 응답하는지 체크합니다.
3초 안에 응답이 없으면 실패로 간주하고, 20번 연속 실패하면 unhealthy 상태가 됩니다.

```yaml
volumes:
  mysql_data:
```
named volume을 정의합니다. Docker가 관리하는 영구 저장소입니다.

---

## 5단계: Docker 컨테이너 실행

```bash
docker compose up -d
```

`-d`는 detached 모드로, 백그라운드에서 실행됩니다.
`-d` 없이 실행하면 터미널에 로그가 계속 출력되고, Ctrl+C 하면 컨테이너도 같이 꺼집니다.

정상 실행 확인:
```bash
docker compose ps
```

`todaybread-mysql`이 `running (healthy)` 상태면 성공입니다.

MySQL CLI에 바로 접속하려면 프로젝트 스크립트를 사용할 수 있습니다:
```bash
./scripts/mysql-connect.sh
```

이 스크립트는 컨테이너 안의 `mysql` 클라이언트를 `utf8mb4`로 실행합니다.
그래서 터미널에서도 한글이 `????`가 아니라 정상 출력됩니다.

프론트 연동용 테스트 데이터를 넣고 싶다면 아래 스크립트를 실행하세요:
```bash
./gradlew bootRun
./scripts/test-data.sh
```

이 스크립트는 샘플 유저, 사장님 계정, 가게, 영업시간, 빵, 찜 데이터를 삽입합니다.
테이블이 아직 없으면 먼저 Spring Boot를 한 번 실행해서 Flyway가 스키마를 생성해야 합니다.
테이블 생성만 끝나면 서버를 계속 켜둘 필요는 없습니다.
동일한 샘플 데이터만 먼저 정리한 뒤 다시 넣기 때문에 여러 번 실행해도 됩니다.

---

## 6단계: Spring Boot 실행

IntelliJ에서 `ServerApplication`을 실행합니다.

또는 터미널에서:
```bash
./gradlew bootRun
```

`.env` 값을 커스텀했다면:
```bash
source .env
./gradlew bootRun
```

실행 시 일어나는 일:
1. Spring Boot가 `application.properties`를 읽고 MySQL에 접속
2. Flyway가 `src/main/resources/db/migration/` 폴더의 SQL 파일을 확인
3. 아직 적용 안 된 SQL 파일이 있으면 자동으로 실행 (테이블 생성 등)
4. JPA(Hibernate)가 엔티티 클래스와 DB 테이블이 일치하는지 검증 (`validate` 모드)
5. 모든 검증 통과 시 앱 정상 기동

> 별도로 SQL을 실행하거나 테이블을 수동으로 만들 필요가 없습니다.
> Flyway가 전부 자동으로 처리합니다.

---

## Flyway란?

Flyway는 데이터베이스 스키마 버전 관리 도구입니다.
Git이 코드 변경 이력을 관리하듯, Flyway는 DB 스키마 변경 이력을 관리합니다.

### 동작 방식

- `src/main/resources/db/migration/` 폴더에 SQL 파일을 넣어둡니다
- 파일명 규칙: `V{버전}__{설명}.sql` (예: `V1__init.sql`, `V2__add_column.sql`)
  - V 뒤에 버전 번호, 언더스코어 두 개(`__`), 설명 순서
- Spring Boot 시작 시 Flyway가 자동으로 미적용 SQL을 순서대로 실행합니다
- 이미 적용된 SQL은 다시 실행하지 않습니다 (`flyway_schema_history` 테이블에서 관리)

### 예시 마이그레이션 파일

`V1__init.sql` — users 테이블 생성:
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(30) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(30) NOT NULL UNIQUE,
    phone_number VARCHAR(30) UNIQUE,
    is_boss BOOLEAN NOT NULL DEFAULT FALSE
);
```

### 새 테이블이나 컬럼을 추가하고 싶을 때

`db/migration/` 폴더에 새 SQL 파일을 추가하면 됩니다:
```
V2__add_store_table.sql
V3__add_address_to_users.sql
```

> 이미 적용된 SQL 파일(V1__init.sql 등)은 절대 수정하면 안 됩니다.
> Flyway가 체크섬을 비교해서 변경을 감지하면 앱 시작이 실패합니다.
> 변경이 필요하면 항상 새 버전의 SQL 파일을 추가하세요.
>
> 여러 `V` 파일에 흩어진 `ALTER`/`INDEX`를 신규 환경 기준으로 정리하고 싶다면,
> 기존 `V` 파일을 수정하지 말고 `B12__baseline_schema.sql` 같은 baseline migration을 추가하세요.

---

## application.properties 전체 구조

```properties
spring.application.name=server

# ============================================================
# Swagger (API 문서)
# localhost:8080/swagger-ui.html 에서 API 문서를 확인할 수 있음
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.packages-to-scan=com.todaybread.server
springdoc.paths-to-match=/api/**

# ============================================================
# DataSource (MySQL 연결)
# .env의 MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD를 참조
# 환경변수 없으면 기본값(todaybread) 사용
spring.datasource.url=jdbc:mysql://localhost:3306/${MYSQL_DATABASE:todaybread}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=${MYSQL_USER:todaybread}
spring.datasource.password=${MYSQL_PASSWORD:todaybread}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ============================================================
# JPA / Hibernate
# ddl-auto=validate: Flyway가 스키마를 관리하므로 Hibernate는 검증만 수행
# show-sql=true: 실행되는 SQL을 콘솔에 출력 (개발 편의)
# format_sql=true: 출력되는 SQL을 보기 좋게 포맷팅
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# ============================================================
# Flyway (DB 마이그레이션)
# enabled=true: 앱 시작 시 Flyway 자동 실행
# locations: 마이그레이션 SQL 파일 경로
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

### JDBC URL 파라미터 설명

`jdbc:mysql://localhost:3306/${MYSQL_DATABASE:todaybread}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8`

| 파라미터 | 설명 |
|----------|------|
| `useSSL=false` | 로컬 개발이므로 SSL 암호화 비활성화 |
| `allowPublicKeyRetrieval=true` | MySQL 8.0의 인증 방식 호환을 위해 필요 |
| `serverTimezone=Asia/Seoul` | 서버 타임존을 서울로 설정 |
| `characterEncoding=UTF-8` | 문자 인코딩을 UTF-8로 설정 |

### ddl-auto 모드 설명

| 모드 | 설명 | 사용 시점 |
|------|------|----------|
| `validate` | 엔티티와 테이블이 일치하는지 검증만 함 (현재 설정) | Flyway 사용 시 |
| `update` | 엔티티 기준으로 테이블을 자동 수정 | Flyway 없이 빠르게 개발할 때 |
| `create` | 매번 테이블을 삭제하고 새로 생성 | 테스트용 |
| `none` | 아무것도 안 함 | 운영 환경 |

현재는 Flyway가 스키마를 관리하므로 `validate`를 사용합니다.
Hibernate가 테이블을 직접 건드리지 않고, 엔티티와 테이블이 맞는지만 확인합니다.

---

## 자주 쓰는 Docker 명령어

### 컨테이너 시작/종료

| 명령어 | 설명 |
|--------|------|
| `docker compose up -d` | 컨테이너 백그라운드 시작 |
| `docker compose up` | 컨테이너 시작 (로그 출력, Ctrl+C로 종료) |
| `docker compose down` | 컨테이너 종료 + 삭제 (데이터 유지) |
| `docker compose down -v` | 컨테이너 종료 + 삭제 + 볼륨 삭제 (데이터 완전 초기화) |
| `docker compose stop` | 컨테이너 멈춤 (삭제 안 함, 데이터 유지) |
| `docker compose start` | 멈춘 컨테이너 다시 시작 |
| `docker compose restart` | 컨테이너 재시작 |

### 상태 확인/로그

| 명령어 | 설명 |
|--------|------|
| `docker compose ps` | 컨테이너 상태 확인 |
| `docker compose logs -f mysql` | MySQL 로그 실시간 보기 (Ctrl+C로 종료) |
| `docker compose logs mysql` | MySQL 로그 한번만 보기 |

### MySQL 직접 접속

가장 간단한 방법은 프로젝트 스크립트를 사용하는 것입니다:

```bash
./scripts/mysql-connect.sh
```

기본값:
- 컨테이너: `todaybread-mysql`
- 데이터베이스: `todaybread`
- 사용자: `todaybread`
- 문자셋: `utf8mb4`

직접 명령어로 접속하고 싶다면 아래처럼 실행하세요:

```bash
docker exec -it todaybread-mysql mysql --default-character-set=utf8mb4 -u {MYSQL_USER 값} -p
```

비밀번호 입력 프롬프트가 나오면 `MYSQL_PASSWORD` 값을 입력하세요.
기본값을 쓰고 있다면 `todaybread`를 입력하면 됩니다.

> `--default-character-set=utf8mb4`를 붙여야 터미널 조회 결과도 한글이 정상 출력됩니다.
> 이 옵션이 없으면 DB에는 한글이 정상 저장되어 있어도 `????`로 보일 수 있습니다.

접속 후 유용한 SQL:
```sql
SHOW DATABASES;              -- 데이터베이스 목록
USE todaybread;              -- todaybread DB 선택
SHOW TABLES;                 -- 테이블 목록
DESCRIBE users;              -- users 테이블 구조 확인
SELECT * FROM users;         -- 유저 데이터 조회
SELECT * FROM flyway_schema_history;  -- Flyway 마이그레이션 이력 확인
```

### 개발용 테스트 데이터 삽입

프론트 연동용 더미 데이터가 필요하면 아래 스크립트를 실행하세요:

```bash
./gradlew bootRun
./scripts/test-data.sh
```

삽입되는 샘플 계정:

- `demo-user@todaybread.local` / `todaybread123`
- `demo-boss-gangnam@todaybread.local` / `todaybread123`
- `demo-boss-seolleung@todaybread.local` / `todaybread123`

추천 근처 조회 좌표:

- `lat=37.4980950`
- `lng=127.0276100`
- `radius=3`

> 이 스크립트는 Docker 볼륨 안의 현재 DB에 데이터를 추가합니다.
> 컨테이너를 다시 띄워도 볼륨을 지우지 않으면 데이터는 유지됩니다.
> 샘플 데이터만 다시 맞추고 싶을 때는 `./scripts/test-data.sh`를 재실행하세요.

### DBeaver / TablePlus로 테이블 보기

SQL을 직접 치지 않고 테이블 데이터를 스프레드시트처럼 보고 싶다면 DBeaver나 TablePlus 같은 GUI 툴을 사용할 수 있습니다.

권장 접속 정보:

- Host: `127.0.0.1`
- Port: `3306`
- Database: `todaybread`
- Username: `todaybread`
- Password: `todaybread`

MySQL 8.0에 GUI 툴로 붙을 때는 아래 옵션을 함께 쓰는 것이 안전합니다.

- `allowPublicKeyRetrieval=true`
- `useSSL=false`

JDBC URL 예시:

```text
jdbc:mysql://127.0.0.1:3306/todaybread?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Seoul
```

DBeaver 사용 시 권장 설정:

- `Show all databases` 끄기
- 연결 대상 DB를 `todaybread`로 지정
- 필요하면 `Connection view -> Simple` 사용

> `todaybread` 계정은 애플리케이션용 계정입니다.
> 그래서 `mysql.user` 같은 시스템 테이블은 조회 권한이 없고, `users`, `store`, `bread` 같은 프로젝트 테이블만 보는 것이 정상입니다.

### 볼륨/정리

| 명령어 | 설명 |
|--------|------|
| `docker volume ls` | 볼륨 목록 보기 |
| `docker volume inspect mysql_data` | 볼륨 상세 정보 (저장 경로 등) |
| `docker system prune` | 안 쓰는 컨테이너/이미지/네트워크 정리 |

---

## 빠른 시작 요약

```bash
# 1. (선택) .env 파일 생성 — 없어도 기본값으로 동작
cp .env.example .env

# 2. Docker 컨테이너 시작
docker compose up -d

# 3. 상태 확인
docker compose ps

# 4. Spring Boot 실행
./gradlew bootRun

# 5. 프론트 연동용 테스트 데이터 삽입 (선택)
./scripts/test-data.sh

# 6. MySQL CLI가 필요하면 utf8mb4 스크립트로 접속
./scripts/mysql-connect.sh
```

---

## 트러블슈팅

### Docker 컨테이너가 안 뜰 때

1. Docker Desktop이 실행 중인지 확인
2. 3306 포트가 이미 사용 중인지 확인:
   ```bash
   lsof -i :3306
   ```
   다른 MySQL이 돌고 있으면 먼저 종료하세요.

### Spring Boot 시작 시 DB 연결 실패

1. Docker 컨테이너가 `running (healthy)` 상태인지 확인:
   ```bash
   docker compose ps
   ```
2. `.env` 값을 커스텀했다면 IntelliJ에 환경변수가 등록되어 있는지 확인
3. 기본값을 쓰고 있다면 `.env`의 값이 `todaybread`인지 확인
4. DBeaver/TablePlus 등 GUI 툴을 테스트하던 중이라면, GUI 쪽에서 비밀번호를 다른 값으로 저장해둔 것은 아닌지 확인

### Flyway 마이그레이션 실패

1. 이미 적용된 SQL 파일을 수정하지 않았는지 확인
2. SQL 문법 오류가 없는지 확인
3. DB를 초기화하고 다시 시작:
   ```bash
   docker compose down -v
   docker compose up -d
   ```

### DB를 완전히 초기화하고 싶을 때

```bash
docker compose down -v    # 컨테이너 + 볼륨 삭제
docker compose up -d      # 다시 시작
./gradlew bootRun         # Flyway로 테이블 생성
```

Flyway가 처음부터 테이블을 다시 만듭니다.

필요하면 그 다음 테스트 데이터도 다시 넣으세요:

```bash
./gradlew bootRun
./scripts/test-data.sh
```

### 프론트 연동용 테스트 데이터가 필요할 때

```bash
./gradlew bootRun
./scripts/test-data.sh
```

이미 넣은 샘플 계정을 같은 상태로 다시 맞추고 싶을 때도 동일한 스크립트를 재실행하면 됩니다.

### Hibernate 검증 오류 (SchemaManagementException)

엔티티 클래스의 필드와 DB 테이블의 컬럼이 일치하지 않을 때 발생합니다.
엔티티를 수정했다면 새 Flyway 마이그레이션 SQL을 추가해야 합니다.

예:
- DB는 `TINYINT`인데 엔티티가 `INTEGER`로 검증되는 경우
- DB에는 삭제된 레거시 컬럼이 남아 있는데 엔티티에는 없는 경우

이 경우는 "테이블이 없다"가 아니라 "스키마와 엔티티 정의가 어긋난 상태"이므로,
로그에 나온 컬럼명을 기준으로 엔티티와 마이그레이션을 함께 확인해야 합니다.

### DBeaver에서 `Public Key Retrieval is not allowed`가 뜰 때

MySQL 연결 속성에 아래 값을 추가하세요:

- `allowPublicKeyRetrieval=true`
- `useSSL=false`

로컬 Docker MySQL에서는 이 설정으로 대부분 해결됩니다.

### DBeaver에서 `SELECT command denied ... for table 'user'`가 뜰 때

이 오류는 보통 프로젝트 테이블이 아니라 `mysql.user` 같은 시스템 테이블을 보려 할 때 발생합니다.

정상 동작:
- `todaybread.users`
- `todaybread.store`
- `todaybread.bread`

비정상처럼 보이지만 실제로는 정상:
- `mysql.user` 조회 실패

이 경우 DBeaver에서:

- `Show all databases` 끄기
- `todaybread` DB만 보기
- `Connection view -> Simple` 사용
