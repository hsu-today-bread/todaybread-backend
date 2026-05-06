# Project Audit Guide

현재 브랜치는 최종 병합 전 감사/리팩토링 브랜치로 본다.

목표는 대규모 기능 추가가 아니라, 현재 구현이 논리적으로 일관적인지 검증하고, 위험한 부분을 문서화한 뒤, 안전한 범위의 리팩토링만 적용하는 것이다.

## 작업 원칙

- 기존 API 계약을 함부로 바꾸지 않는다.
- DTO 통합은 바로 하지 말고 후보만 표시한다.
- Entity, migration, 테스트 간 불일치를 우선 확인한다.
- Optional 정리는 가능하지만 비즈니스 분기는 유지한다.
- 삭제는 참조 검색과 테스트 확인 전에는 하지 않는다.
- 공용 유틸/추상화는 만들지 않는다.
- 필요한 경우 Service 내부 private helper까지만 허용한다.
- 리팩토링보다 검증과 문서화를 우선한다.

## 기준선 검증

감사 시작 전에 먼저 현재 상태를 확인한다.

```bash
git status
./gradlew compileJava testClasses
./gradlew test
```

실패하면 바로 고치지 말고 아래에 기준선 실패로 기록한다.

```text
기준선 컴파일 결과:
기준선 테스트 결과:
실패 테스트:
실패 원인 추정:
리팩토링 전부터 깨져 있었는지:
```

## 읽는 순서

도메인 순서:

```text
auth/user
store/bread
cart/order/payment
review/keyword/wishlist
global/config
```

각 도메인 안에서는 다음 순서로 읽는다.

```text
IntegrationTest / ServiceTest
Service
Entity
Repository
Controller
DTO
migration
```

처음부터 DTO, Repository query, Config 전체를 읽지 않는다. 테스트와 Service에서 사용자 흐름과 중간 로직을 먼저 파악한다.

## 검증 관점

각 도메인마다 아래를 파일/메서드 기준으로 확인한다.

1. 핵심 비즈니스 흐름이 명확한가
2. 상태를 바꾸는 로직이 어디에 있는가
3. 권한/소유자 검증이 누락된 곳은 없는가
4. 트랜잭션 경계가 적절한가
5. 동시성 문제가 생길 수 있는 곳은 없는가
6. Entity와 DB migration이 일치하는가
7. DTO가 API 계약을 명확히 표현하는가
8. 테스트가 실제 위험한 흐름을 막고 있는가
9. 테스트가 없는 위험한 흐름은 무엇인가
10. 삭제 가능한 코드처럼 보이는 것이 실제로 참조되는가

## Optional 정리 기준

Optional을 전부 없애는 것이 목표가 아니다.

```text
없음 = 에러
-> orElseThrow 또는 Service 내부 private helper 사용

있음/없음 둘 다 정상 흐름
-> Optional 분기 유지
```

정리 가능 예:

```java
UserEntity user = userRepository.findById(userId)
    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
```

반복되면 Service 내부 private helper로 뺀다.

```java
private UserEntity getUserOrThrow(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
}
```

유지해야 하는 예:

```text
장바구니 아이템 있으면 수량 증가, 없으면 생성
즐겨찾기 있으면 삭제, 없으면 생성
리프레시 토큰 있으면 갱신, 없으면 생성
결제 idempotency key 있으면 기존 결과 반환, 없으면 새 결제
키워드 있으면 재사용, 없으면 생성
```

helper 이름에는 조회 조건을 드러낸다.

```text
getUserOrThrow
getActiveStoreOrThrow
getActiveStoreByUserOrThrow
getOwnedActiveStoreOrThrow
getNotDeletedBreadOrThrow
getCartWithLockOrThrow
```

피해야 할 이름:

```text
getEntity
check
validate
process
handle
```

## DTO 정리 기준

DTO는 바로 통합하지 않는다. 통합 후보만 문서에 표시한다.

통합 가능 후보:

```text
필드 의미가 완전히 같음
validation 조건이 같음
같은 사용자 권한/같은 API 계약에서 사용됨
앞으로도 같이 바뀔 가능성이 높음
```

통합 금지 후보:

```text
Create / Update 필수값이 다름
고객용 / 사장님용 응답이 다름
목록 / 상세 응답이 다름
필드는 같지만 의미가 다름
null 필드가 많아지는 공통 DTO가 됨
```

DTO 이름 변경보다 중요한 것은 JSON 필드 계약이다. 다음 변경은 API breaking change로 본다.

```text
field rename
field delete
required/nullability 변경
list/detail 구조 변경
date/enum 문자열 변경
```

## 리팩토링 허용 범위

허용:

```text
Optional isEmpty/get -> orElseThrow
반복 조회 + 같은 예외 -> Service 내부 private helper
명백히 죽은 import 제거
사용 안 하는 private method 제거
테스트 이름/문서 보강
```

주의:

```text
DTO 통합
Repository query 변경
Entity 관계 변경
migration 수정
권한 로직 변경
결제/주문/재고 상태 변경
```

금지:

```text
API 응답 필드 변경
DB schema 임의 변경
큰 공용 추상화 추가
도메인 간 구조 대이동
테스트를 맞추기 위한 동작 변경
```

## 도메인별 테스트

수정 후 관련 테스트를 먼저 돌린다.

```bash
./gradlew test --tests '*User*' --tests '*Auth*'
./gradlew test --tests '*Store*' --tests '*Bread*'
./gradlew test --tests '*Cart*' --tests '*Order*' --tests '*Payment*'
./gradlew test --tests '*Review*' --tests '*Keyword*' --tests '*Wishlist*'
```

마지막 전체 검증:

```bash
./gradlew clean test
```

## 도메인별 감사 템플릿

아래 형식을 복사해 도메인마다 채운다.

```md
## domain-name

### 핵심 흐름
- 

### 상태 변경 로직
- 

### 권한/소유자 검증
- 

### Optional / 예외 처리
- 

### DTO / API 계약
- 

### Entity / DB migration
- 

### 테스트 현황
- 

### 위험
- 

### 결정
- 유지:
- 수정 필요:
- 삭제 후보:
```

## 감사 기록

### 기준선

```text
브랜치: refactor/audit
커밋: 50aab03
git status: Untracked files: docs/PROJECT-AUDIT.md
compileJava testClasses: BUILD SUCCESSFUL
전체 test: BUILD SUCCESSFUL
```

### auth/user

#### 핵심 흐름
- 회원가입→로그인→토큰재발급→로그아웃
- 사장님 등록 (사업자번호 검증 → BOSS 권한 부여 → 토큰 재발급)
- 비밀번호 재설정 (전화번호로 이메일 찾기 → 본인 확인 → 비밀번호 변경)

#### 상태 변경 로직
- UserEntity.approveBoss(): isBoss = true 설정
- UserEntity.changePassword(): passwordHash 변경
- UserEntity.updateProfile(): name, nickname, phoneNumber 변경
- RefreshTokenEntity.renew(): token, expiresAt 갱신

#### 권한/소유자 검증
- JWT subject로 userId 추출 (JwtTokenService.parseAccessToken)
- 프로필 수정은 자기 자신만 가능 (Controller에서 @AuthenticationPrincipal userId 전달)
- 사장님 등록도 자기 자신만 가능 (동일 패턴)

#### Optional / 예외 처리
- login에서 이메일 미존재와 비밀번호 불일치 모두 USER_NOT_FOUND 반환 (의도적 보안 패턴 - 계정 존재 여부 노출 방지)
- updateProfile, approveBoss: getUserOrThrow(userId) private helper로 정리 완료
- findEmailByPhone, verifyIdentity, resetPassword: orElseThrow로 정리 완료
- reissue: refreshToken 조회와 user 조회 모두 orElseThrow로 정리 완료
- saveRefreshToken: 있으면 갱신, 없으면 생성 → Optional 분기 유지 (정상)

#### DTO / API 계약
- record 기반 DTO, @Valid 검증
- 비밀번호 최소 10자 (@Size(min=10))
- UserLoginResponse에 accessToken, refreshToken, userId, email, nickname, isBoss 포함

#### Entity / DB migration
- UserEntity(users): id, email, passwordHash, name, nickname, phoneNumber, isBoss - BaseEntity 상속 (created_at, updated_at)
- RefreshTokenEntity(refresh_token): id, userId, token(해시), expiresAt - BaseEntity 상속

#### 테스트 현황
- UserServiceTest: 회원가입, 로그인, 프로필 수정, 사장님 등록 단위 테스트
- AuthServiceTest: 토큰 재발급, 로그아웃 단위 테스트
- UserRecoveryServiceTest: 이메일 찾기, 본인 확인, 비밀번호 재설정 단위 테스트
- UserAuthApiIntegrationTest: 회원가입→로그인→토큰재발급→로그아웃 통합 테스트

#### 위험
- 사업자 번호 검증이 10자리 숫자 형식만 체크 (`\\d{10}` 정규식). 실제 사업자 등록 API 미연동
- TODO 주석으로 표시되어 있음

#### 결정
- 유지: 전체 구조, 보안 패턴 (login 동일 에러코드), saveRefreshToken Optional 분기
- 수정 필요: Optional 패턴 정리 → 완료 (orElseThrow + getUserOrThrow helper)
- 삭제 후보: 없음

### store/bread

#### 핵심 흐름
- 가게 등록/수정/이미지 교체 (사장님 전용)
- 빵 CRUD (soft delete 방식)
- 근처 가게/빵 검색 (좌표 기반 Haversine)
- 단골 가게 토글 (FavouriteStoreService)
- 가게 상세 조회 (빵 목록 + 판매 상태 + 평점 포함)

#### 상태 변경 로직
- StoreEntity.updateInfo(): name, phone, description, address, 좌표 변경
- BreadEntity.softDelete(): isDeleted=true, deletedAt 설정
- BreadEntity.decreaseQuantity() / increaseQuantity() / changeQuantity(): 재고 변경
- BreadEntity.updateInfo(): 빵 정보 수정

#### 권한/소유자 검증
- @PreAuthorize("hasRole('BOSS')"): 빵/가게 관리 API
- getOwnedBread(userId, breadId): 사장님 가게 소유 빵인지 검증 (storeId 비교)
- getStoreByUserId(userId): 사장님 본인 가게 조회

#### Optional / 예외 처리
- getStoreByUserId(), getOwnedBread(): 기존 private helper → orElseThrow로 정리 완료
- getStoreInfo(), getStoreDetail(), updateStore(): orElseThrow로 정리 완료
- getBreadsFromStore(), getBreadDetail(): orElseThrow로 정리 완료
- FavouriteStoreService.toggleFavouriteStore(): 있으면 삭제, 없으면 추가 → Optional 분기 유지 (정상)

#### DTO / API 계약
- Multipart 기반 이미지 업로드 (가게 1~5장, 빵 1장)
- BreadSortType enum (DISTANCE, PRICE, DISCOUNT, RANDOM)
- 좌표 기반 검색 (lat, lng, radiusKm 파라미터)
- StoreDetailResponse: 가게 정보 + 이미지 + 빵 목록 + 판매 상태 + 평점/리뷰수

#### Entity / DB migration
- StoreEntity(store): id, userId, name, phoneNumber, description, addressLine1/2, latitude, longitude, isActive, averageRating, reviewCount - BaseEntity 상속
- BreadEntity(bread): id, storeId, name, description, originalPrice, salePrice, remainingQuantity, isDeleted, deletedAt - BaseEntity 상속
- StoreBusinessHoursEntity(store_business_hours): storeId, dayOfWeek, isClosed, startTime, endTime, lastOrderTime
- StoreImageEntity(store_image): storeId, storedFilename, displayOrder

#### 테스트 현황
- BreadServiceTest: 빵 CRUD, 재고 변경, 소유권 검증 단위 테스트
- StoreServiceTest: 가게 등록/수정/조회, 근처 가게 검색 단위 테스트
- BreadImageServiceTest, StoreImageServiceTest: 이미지 업로드/삭제 단위 테스트
- FavouriteStoreServiceTest: 단골 가게 토글 단위 테스트
- SellingStatusUtilTest: 판매 상태 판별 로직 단위 테스트
- BossCatalogApiIntegrationTest: 사장님 가게/빵 관리 통합 테스트
- Property-based tests (jqwik): BreadEntity 재고 변경 속성 테스트

#### 위험
- Haversine 쿼리 성능: 인덱스 있음 (Bounding Box 사전 필터링으로 완화)
- 이미지 orphan 가능성: afterCommit 패턴으로 완화 (트랜잭션 롤백 시 파일 삭제 방지)

#### 결정
- 유지: soft delete 구조, 이미지 트랜잭션 패턴, Bounding Box + Haversine 조합
- 수정 필요: Optional 패턴 정리 → 완료
- 삭제 후보: 없음

### cart/order/payment

#### 핵심 흐름
- 장바구니 추가 → 주문 생성 (cart 기반 / direct 기반) → 결제 승인 (토스) → 주문 확정 → 픽업 완료
- 주문 취소: 2단계 패턴 (CONFIRMED → CANCEL_REQUESTED → 토스 취소 → CANCELLED)
- 만료 주문 자동 취소 (스케줄러)
- 멱등성 키 기반 중복 주문 방지

#### 상태 변경 로직
- OrderEntity.updateStatus(): ALLOWED_TRANSITIONS map 기반 상태 전이 검증
- PaymentEntity.approve() / cancel(): 결제 상태 변경
- BreadEntity.decreaseQuantity() / increaseQuantity(): 주문 시 재고 차감, 취소 시 복원
- CartEntity / CartItemEntity: 장바구니 항목 관리

#### 권한/소유자 검증
- 주문 소유자 검증: order.getUserId().equals(userId) → ORDER_ACCESS_DENIED
- 사장님 가게 소유 검증: OrderBossService에서 가게 소유 주문만 조회/상태 변경
- 멱등성 키: Idempotency-Key 헤더 필수, UK(user_id, idempotency_key)로 DB 레벨 보장

#### Optional / 예외 처리
- 비관적 락 사용: findByIdWithLock, findAllByIdWithLock (재고 경합 방지)
- cancelOrder, confirmOrder: orElseThrow로 정리 완료
- getOrderDetail: order, store 모두 orElseThrow로 정리 완료
- createOrderFromCart/createDirectOrder: store 조회 orElseThrow로 정리 완료
- CartService.addToCart(): 기존 아이템 있으면 수량 증가, 없으면 생성 → Optional 분기 유지 (정상)
- findExistingOrderDetail(): 멱등성 체크 → Optional 분기 유지 (정상)

#### DTO / API 계약
- Idempotency-Key 헤더 필수 (주문 생성 API)
- TossOrderIdHelper: 내부 orderId ↔ 토스 orderId 변환
- 페이지네이션 (Pageable)
- OrderDetailResponse: 주문 정보 + 가게명 + 주문 항목 (스냅샷) + 빵 이미지

#### Entity / DB migration
- OrderEntity(orders): id, userId, storeId, status, totalAmount, idempotencyKey, orderNumber, orderDate - UK(user_id, idempotency_key)
- OrderItemEntity(order_item): id, orderId, breadId, breadName, breadPrice, quantity (스냅샷)
- PaymentEntity(payment): id, orderId, tossPaymentKey, amount, method, status - UK(order_id)
- CartEntity(cart): id, userId, storeId - UK(user_id)
- CartItemEntity(cart_item): id, cartId, breadId, quantity

#### 테스트 현황
- CartServiceTest: 장바구니 추가/삭제/수량 변경 단위 테스트
- OrderServiceTest: 주문 생성/취소/확정/조회, 멱등성 단위 테스트
- PaymentServiceTest: 결제 승인/취소 단위 테스트
- OrderExpiryServiceTest: 만료 주문 자동 취소 단위 테스트
- BossOrderSalesApiIntegrationTest: 사장님 주문 관리 통합 테스트
- CommerceWishlistApiIntegrationTest: 주문→리뷰→찜 통합 테스트

#### 위험
- 결제 취소 2단계 패턴: 토스 API 실패 시 CONFIRMED 복원 (데이터 정합성 보장, 재시도 필요)
- 만료 스케줄러 다중 인스턴스 경합: 문서화됨 (단일 인스턴스 운영 또는 분산 락 필요)

#### 결정
- 유지: 2단계 취소 패턴, 멱등성 처리, 비관적 락 기반 재고 관리, ALLOWED_TRANSITIONS 상태 전이
- 수정 필요: Optional 패턴 정리 → 완료
- 삭제 후보: 없음

### review/keyword/wishlist

#### 핵심 흐름
- 리뷰 작성 (PICKED_UP 주문만) → 가게 평점 갱신 (원자적)
- 키워드 등록/삭제 (사용자당 최대 5개, 키워드당 최대 10자)
- 단골 가게 토글 (FavouriteStoreService)
- 찜목록 통합 조회 (단골 가게 + 키워드)

#### 상태 변경 로직
- StoreEntity.addReviewRating(): 메모리 레벨 평점 계산 (사용하지 않음)
- StoreRepository.addReviewRating(): @Modifying 쿼리로 원자적 평점 갱신 (ReviewService에서 사용)

#### 권한/소유자 검증
- 리뷰: 주문 소유자만 작성 가능 (order.getUserId().equals(userId))
- 키워드 삭제: 소유자만 가능 (entity.getUserId().equals(userId) → KEYWORD_FORBIDDEN)
- 단골 가게: 자기 자신의 단골만 관리 (userId 기반)

#### Optional / 예외 처리
- 중복 리뷰: UK(user_id, order_item_id) 위반 → GlobalExceptionHandler에서 REVIEW_ALREADY_EXISTS 매핑
- KeywordService.deleteKeyword(): orElseThrow로 정리 완료
- KeywordService.createKeyword(): 키워드 있으면 재사용, 없으면 생성 → Optional 분기 유지 (정상)
- FavouriteStoreService.toggleFavouriteStore(): 있으면 삭제, 없으면 추가 → Optional 분기 유지 (정상)

#### DTO / API 계약
- 리뷰 이미지 최대 2장
- 키워드 최대 5개/10자
- 정렬/필터 옵션 (리뷰 목록)
- KeywordCreateResponse, KeywordDeleteResponse: 성공 여부만 반환

#### Entity / DB migration
- ReviewEntity(review): id, userId, orderItemId, storeId, rating, content - UK(user_id, order_item_id)
- ReviewImageEntity(review_image): id, reviewId, storedFilename, displayOrder
- KeywordEntity(keyword): id, normalisedText - UK(normalised_text)
- UserKeywordEntity(user_keyword): id, userId, keywordId, displayText - UK(user_id, keyword_id)

#### 테스트 현황
- ReviewServiceTest: 리뷰 작성, 중복 방지, 권한 검증 단위 테스트
- ReviewEntityPropertyTest: 리뷰 엔티티 속성 테스트 (jqwik)
- KeywordServiceTest: 키워드 등록/삭제, 정규화, 제한 단위 테스트
- CommerceWishlistApiIntegrationTest: 주문→리뷰→찜 통합 테스트

#### 위험
- 평점 갱신이 StoreEntity 메서드와 Repository @Modifying 쿼리 두 곳에 존재. ReviewService는 Repository 사용으로 원자적 보장됨. StoreEntity.addReviewRating()은 현재 미사용이나 삭제 전 참조 확인 필요

#### 결정
- 유지: 원자적 평점 갱신 (@Modifying 쿼리), UK 기반 중복 방지, 키워드 정규화
- 수정 필요: Optional 패턴 정리 → 완료
- 삭제 후보: 없음

### global/config

#### 핵심 흐름
- JWT 발급/검증 (JwtTokenService): accessToken (userId, email, role), refreshToken (userId)
- Spring Security 설정 (SecurityConfig): permitAll 경로, RoleHierarchy(BOSS > USER)
- 파일 저장소 (FileStorage 인터페이스 + LocalFileStorage 구현)
- 전역 예외 처리 (GlobalExceptionHandler): 모든 예외를 ErrorResponse로 변환

#### 상태 변경 로직
- 없음 (stateless)

#### 권한/소유자 검증
- SecurityConfig: permitAll 경로 정의 (회원가입, 로그인, 토큰 재발급, 공개 조회 API)
- @PreAuthorize + RoleHierarchy: BOSS > USER (사장님은 일반 사용자 권한도 보유)
- JwtAuthenticationFilter: 요청마다 JWT 검증 → SecurityContext에 인증 정보 설정

#### Optional / 예외 처리
- GlobalExceptionHandler: CustomException → ErrorResponse(code, message) 변환
- DataIntegrityViolationException → 적절한 ErrorCode 매핑 (REVIEW_ALREADY_EXISTS 등)
- 정리 대상 없음

#### DTO / API 계약
- ErrorResponse(code, message): 전역 표준 에러 형식
- OpenApiConfig: Swagger/OpenAPI 설정

#### Entity / DB migration
- BaseEntity: created_at, updated_at (JPA Auditing - @CreatedDate, @LastModifiedDate)

#### 테스트 현황
- JwtTokenServiceTest: 토큰 생성/파싱/만료 검증 단위 테스트
- ImageValidationHelperTest: 이미지 검증 유틸 단위 테스트

#### 위험
- JWT secret 기본값 사용 시 시작 실패 (의도적 안전장치 - @Value 바인딩 실패)
- LocalFileStorage는 개발용 (프로덕션에서는 S3 등 외부 저장소 전환 필요)

#### 결정
- 유지: FileStorage 인터페이스 추상화, GlobalExceptionHandler 구조, RoleHierarchy
- 수정 필요: 없음
- 삭제 후보: 없음

## 최종 산출물 체크리스트

- [x] 기준선 컴파일 결과 기록
- [x] 기준선 전체 테스트 결과 기록
- [x] 도메인별 핵심 흐름 기록
- [x] 도메인별 상태 변경 로직 기록
- [x] 권한/소유자 검증 누락 여부 기록
- [x] Entity / migration 불일치 여부 기록
- [x] Optional 정리 후보 기록
- [x] DTO 통합 후보 기록
- [x] 삭제 후보 기록
- [x] 도메인별 테스트 실행
- [x] `./gradlew clean test` 실행
