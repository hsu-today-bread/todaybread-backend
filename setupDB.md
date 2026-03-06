# 🚀 TodayBread 백엔드 로컬 환경 세팅 가이드

이 가이드는 프로젝트를 처음 전달받은 개발자가 로컬 개발 환경을 구축하고 첫 서버를 구동하기 위한 모든 과정을 담고 있습니다.

---

## 0. 사전 준비 (Prerequisites)
프로젝트 구동을 위해 다음 도구들이 설치되어 있어야 합니다.
- **Java 21 JDK**: 프로젝트 빌드 및 실행에 필수입니다.
- **Docker Desktop**: 데이터베이스(MySQL) 구성을 위해 권장됩니다.
- **IDE**: IntelliJ IDEA (권장) 또는 VS Code

---

## 🟢 방법 1: Docker Compose로 시작하기 (가장 빠름 ⭐)

Spring Boot의 자동화 기능을 사용하여 데이터베이스 설치 및 설정 과정을 생략할 수 있습니다.

### Step 1. Docker 실행
1. **Docker Desktop 설치**: [공식 홈페이지](https://www.docker.com/products/docker-desktop/)에서 설치 후 실행 상태를 확인하세요.
2. Docker Desktop을 실행합니다. (고래 아이콘이 'Running' 상태여야 합니다.)

### Step 2. (중요) 기존 포트 점유 확인
로컬에 이미 MySQL이 설치되어 있다면 3306 포트가 충돌할 수 있습니다.
- **Mac/Linux**: `brew services stop mysql@8.0` 명령어로 로컬 MySQL을 중지하세요.

### Step 3. 서버 구동
터미널에서 프로젝트 루트 디렉토리로 이동한 후 다음 명령어를 실행합니다.
```bash
# 실행 권한 부여 (필요 시)
chmod +x gradlew

# 서버 실행
./gradlew bootRun
```

### Step 4. 자동화 로직 작동 확인
서버가 실행되면 Spring Boot가 루트의 `compose.yaml`을 읽어 다음 작업을 자동으로 수행합니다.
- MySQL 8.0 컨테이너 생성 및 실행
- `todaybread` 데이터베이스 생성 및 계정 설정
- Hibernate를 통한 테이블 스키마 생성 (DDL-Auto)

--------------

## 🔵 방법 2: 로컬 MySQL(Native) 사용하기 (Docker 미사용 시)

로컬에 설치된 MySQL을 사용하려면 추가적인 설정이 필요합니다.

### Step 1. MySQL 8.0 설치 및 구동 (Mac 기준)
```bash
brew install mysql@8.0
brew services start mysql@8.0
```

### Step 2. 데이터베이스 및 계정 생성
`root` 계정으로 접속하여 프로젝트용 DB와 사용자를 생성합니다.
```bash
mysql -u root -p
```

```sql
-- 1. 데이터베이스 생성
CREATE DATABASE todaybread DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 사용자 계정 생성 (MySQL 8.0 인증 방식 대응)
CREATE USER 'todaybread'@'localhost' IDENTIFIED BY 'todaybread';

-- 3. 권한 부여
GRANT ALL PRIVILEGES ON todaybread.* TO 'todaybread'@'localhost';
FLUSH PRIVILEGES;
```

### Step 2. 프로젝트 설정 변경
`src/main/resources/application.properties`에서 아래 설정을 추가하여 Docker 자동 실행 기능을 끕니다.
```properties
spring.docker.compose.enabled=false
```

---

## ✅ 정상 작동 확인 (Verification)

서버가 성공적으로 실행(`Started ServerApplication in ... seconds`)되었다면 아래 주소로 접속하여 확인하세요.

1. **API 문서 (Swagger)**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - UI가 정상적으로 보인다면 컨트롤러와 연결이 완료된 것입니다.
2. **헬스 체크**: [http://localhost:8080/api/system/health](http://localhost:8080/api/system/health)
   - `status: "UP"` 메시지가 보이면 DB 연결까지 정상입니다.

---

## 🛠️ 문제 해결 (Troubleshooting)

**Q. `Permission Denied` 오류가 발생합니다.**
- `chmod +x gradlew` 명령어를 실행하여 실행 파일에 권한을 부여하세요.

**Q. `Connection Refused` 혹은 `Table not found` 오류가 발생합니다.**
- Docker가 켜져 있는지 확인하세요.
- 첫 실행 시 MySQL 엔진이 완전히 켜지기까지 약 10초 정도 소요됩니다. 서버를 껐다가 다시 켜보세요.

**Q. 포트 3306이 이미 사용 중이라고 나옵니다.**
- `lsof -i :3306` 명령어로 포트를 점유 중인 프로세스를 확인하고 종료하거나, `compose.yaml`의 포트 매핑을 `3307:3306` 등으로 수정하세요.
