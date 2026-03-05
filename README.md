# 개발 환경
### 언어: Java 21.0.9 LTS
### 프레임 워크: Spring boot
    - Project: Gradle - Groovy
    - Language: Java
    - Spring Boot: 3.5.11
    - Packaging: Jar
    - Config: Properties
    - Java: 21
# 컨벤션

---
## A. 코드 컨벤션
### 1. 네이밍 컨벤션
- 클래스/인터페이스: `PascalCase` + 명사 (`UserService`, `UserRepository`, `ErrorResponse`)
- 메서드/변수: `camelCase` + 메서드는 동사로 시작 (`registerUser`, `findById`)
- 상수: `UPPER_SNAKE_CASE` (`MAX_RETRY_COUNT`)
- 패키지: 소문자만 사용 (`com.todaybread.server.domain.user.controller`)
- Enum: 타입은 `PascalCase`, 상수는 `UPPER_SNAKE_CASE`
- DTO 접미사: `Request`, `Response`

### 2. 코드 포맷팅/주석
- 들여쓰기 4칸 스페이스, 탭 사용 지양
- 중괄호는 K&R 스타일
- 클래스/메서드 간 빈 줄 1줄 유지
- 주석은 "왜(why)" 중심으로 최소화, 코드가 설명 가능한 내용은 주석 생략
- Public API 성격의 클래스/메서드는 JavaDoc 권장

### 3. 레이어/패키지 규칙
- 기본 구조: `domain/{도메인}/controller`, `service`, `repository`, `dto`, `entity`
- `controller`: 요청/응답 처리, 검증 트리거(`@Valid`)만 담당
- `service`: 비즈니스 로직, 트랜잭션 경계 관리
- `repository`: DB 접근 전담, 비즈니스 로직 금지
- `entity`: 영속성 모델, API 스펙(`Request/Response`)으로 직접 사용 금지

### 4. Spring Web 규칙
- Controller는 얇게 유지하고 비즈니스 로직은 Service로 이동
- API 요청 DTO에는 Bean Validation을 사용 (`@NotNull`, `@NotBlank`, `@Size` 등)
- API 응답은 Entity 직접 반환 금지, Response DTO로 변환 후 반환
- 공통 예외는 `CustomException`, `ErrorCode`, `GlobalExceptionHandler` 체계 사용

### 5. JPA 엔티티 규칙
- 모든 엔티티는 `@Entity` 사용, 필요한 경우 `@Table(name = "...")` 명시
- PK는 `Long id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)` 기본 사용
- 기본 생성자는 `protected`로 제한
- Setter 남발 금지, 의미 있는 도메인 메서드로 상태 변경
- 엔티티에는 비즈니스 불변식(유효한 상태) 유지 로직 포함 가능
- 생성/수정 시간은 `BaseEntity`(예: `createdAt`, `updatedAt`)로 공통 관리

### 6. 연관관계 및 조회 규칙
- 연관관계는 기본 `FetchType.LAZY` 사용
- 양방향 연관관계는 꼭 필요할 때만 사용, 연관관계 편의 메서드 제공
- 목록 조회는 항상 페이징 고려 (`Pageable`)
- N+1 가능성이 있으면 Fetch Join/EntityGraph/배치 전략으로 해결
- 복잡 쿼리는 JPQL 또는 QueryDSL 사용, Native Query는 최소화

### 7. 트랜잭션 규칙
- `@Transactional`은 Service 계층에 선언
- 조회 전용 메서드는 `@Transactional(readOnly = true)` 사용
- Controller/Repository에 트랜잭션 선언하지 않음
- 트랜잭션 내부에서 외부 API 호출은 지양 (락 점유 시간 최소화)

### 8. Repository 규칙
- 기본은 `JpaRepository` 상속
- 단순 조회는 메서드 네이밍 쿼리 사용
- 조건이 복잡해지면 커스텀 Repository로 분리
- `Optional<T>`를 적극 사용하고 `null` 반환 금지

### 9. DB 마이그레이션(Flyway) 규칙
- 모든 스키마 변경은 SQL 파일로 관리하고 수동 DB 변경 금지
- 마이그레이션 파일명 규칙: `V{버전}__{설명}.sql` (예: `V1__init_user_table.sql`)
- 운영 반영 전 로컬/스테이징에서 동일 마이그레이션 검증
- 롤백이 필요한 변경은 사전 대응 SQL 또는 대체 전략을 함께 준비

### 10. 테스트 규칙
- Service: 단위 테스트로 비즈니스 로직 검증
- Repository: `@DataJpaTest`로 쿼리/매핑 검증
- Controller: `@WebMvcTest` 또는 통합 테스트로 API 계약 검증
- 테스트 메서드명은 시나리오가 드러나도록 작성 (`registerUser_success`)

## B. 협업 컨벤션
### 1. 브랜치 전략 (GitHub Flow)
- `main`: 배포 가능한 메인 브랜치
- `feature/*`: 새 기능 개발
- `fix/*`: 버그 수정
- `chore/*`: 빌드/설정/의존성 작업
- `refactor/*`: 리팩토링
- `docs/*`: 문서 작업

### 2. 브랜치 이름 규칙
- 형식: `{브랜치 타입}/{설명}`
```markdown
feature/login
feature/register
docs/readme-update
```

### 3. 커밋 메시지 규칙
- 형식: `[{브랜치 타입}] 작업 내용`
- 예시
  - `[feature] 로그인 기능 구현`
  - `[fix] 회원가입 중복 체크 버그 수정`
  - `[docs] README 코딩 컨벤션 업데이트`
