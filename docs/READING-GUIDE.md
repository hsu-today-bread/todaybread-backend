# 프로젝트 읽기 가이드

이 문서는 TodayBread 서버 프로젝트를 처음 읽을 때 어디서부터 봐야 하는지 정리한 순서표입니다.

현재 워크트리 기준으로는 `user / auth / store / bread / keyword / cart / order / payment / wishlist` 도메인이 실제 소스에 보입니다.
전체 API 표면은 `docs/API.md`와 Swagger UI를 함께 확인하는 것이 좋습니다.

## 가장 빠른 시작 순서

| 순서 | 파일 | 왜 먼저 보는지 |
|------|------|----------------|
| 1 | `src/main/java/com/todaybread/server/ServerApplication.java` | 애플리케이션 진입점과 공통 빈(`Clock`)을 확인할 수 있습니다. |
| 2 | `src/main/java/com/todaybread/server/config/SecurityConfig.java` | 인증이 어디서 강제되고 어떤 경로가 예외인지 한 번에 파악할 수 있습니다. |
| 3 | `src/main/java/com/todaybread/server/global/exception/GlobalExceptionHandler.java` | 이 프로젝트의 에러 응답 방식이 어떻게 통일되는지 이해할 수 있습니다. |
| 4 | `src/main/java/com/todaybread/server/global/exception/ErrorCode.java` | 도메인별 실패 케이스를 빠르게 훑을 수 있습니다. |
| 5 | `docs/API.md` | 전체 API 표면을 먼저 보고, 이후 도메인 코드를 읽을 때 길을 잃지 않게 합니다. |

## 30분 읽기 루트

### 1. 공통 구조

1. `src/main/java/com/todaybread/server/ServerApplication.java`
2. `src/main/java/com/todaybread/server/config/SecurityConfig.java`
3. `src/main/java/com/todaybread/server/global/exception/GlobalExceptionHandler.java`
4. `src/main/java/com/todaybread/server/global/exception/ErrorCode.java`
5. `docs/API.md`

여기까지 보면:

- 인증이 기본적으로 필요한 구조인지
- 공통 예외가 어떤 형태로 내려가는지
- 어떤 도메인이 있는지
- 전체 API 표면이 어떻게 생겼는지

를 먼저 잡을 수 있습니다.

### 2. 인증과 유저

1. `src/main/java/com/todaybread/server/domain/auth/controller/AuthController.java`
2. `src/main/java/com/todaybread/server/domain/auth/service/AuthService.java`
3. `src/main/java/com/todaybread/server/domain/user/controller/UserController.java`
4. `src/main/java/com/todaybread/server/domain/user/service/UserService.java`
5. `src/main/java/com/todaybread/server/domain/user/controller/UserRecoveryController.java`
6. `src/main/java/com/todaybread/server/domain/user/service/UserRecoveryService.java`
7. `docs/JWT-GUIDE.md`

이 구간은 "누가 어떻게 로그인하고 인증되는가"를 이해하는 루트입니다.

### 3. 핵심 비즈니스

1. `src/main/java/com/todaybread/server/domain/store/controller/StoreController.java`
2. `src/main/java/com/todaybread/server/domain/store/controller/StoreBossController.java`
3. `src/main/java/com/todaybread/server/domain/store/service/StoreService.java`
4. `src/main/java/com/todaybread/server/domain/bread/controller/BreadController.java`
5. `src/main/java/com/todaybread/server/domain/bread/controller/BreadBossController.java`
6. `src/main/java/com/todaybread/server/domain/bread/service/BreadService.java`

이 구간은 이 프로젝트의 실제 상품 흐름을 이해하는 핵심입니다.
가게와 빵 조회, 사장님 관리 기능이 여기 몰려 있습니다.

### 4. 부가 기능

1. `src/main/java/com/todaybread/server/domain/keyword/controller/KeywordController.java`
2. `src/main/java/com/todaybread/server/domain/keyword/service/KeywordService.java`
3. `src/main/java/com/todaybread/server/domain/store/controller/FavouriteStoreController.java`
4. `src/main/java/com/todaybread/server/domain/store/service/FavouriteStoreService.java`
5. `src/main/java/com/todaybread/server/domain/wishlist/controller/WishlistController.java`

이 구간은 개인화 기능과 조회 보조 기능을 보는 순서입니다.

### 5. 커머스/결제

1. `src/main/java/com/todaybread/server/domain/cart/controller/CartController.java`
2. `src/main/java/com/todaybread/server/domain/cart/service/CartService.java`
3. `src/main/java/com/todaybread/server/domain/order/controller/OrderController.java`
4. `src/main/java/com/todaybread/server/domain/order/service/OrderService.java`
5. `src/main/java/com/todaybread/server/domain/payment/controller/PaymentController.java`
6. `src/main/java/com/todaybread/server/domain/payment/service/PaymentService.java`
7. `docs/TOSS.md`

이 구간은 장바구니, 주문, 결제, 취소 흐름을 보는 순서입니다.

## 도메인 하나를 읽는 고정 순서

도메인은 아래 순서로 읽으면 가장 덜 헤맵니다.

1. `controller`
2. `dto`
3. `service`
4. `entity`
5. `repository`

이 순서가 좋은 이유:

- `controller`에서 API 진입점을 먼저 봅니다.
- `dto`에서 입력/출력 모양을 확인합니다.
- `service`에서 실제 비즈니스 로직을 봅니다.
- `entity`에서 도메인 상태를 확인합니다.
- `repository`에서 DB 접근 방식을 마무리로 봅니다.

## 시간이 더 있으면 볼 문서

- `docs/CONVENTION.md`: 레이어 규칙, 예외 처리 규칙, 협업 규칙
- `docs/DB-SETUP.md`: MySQL, Flyway, 로컬 환경 구조
- `docs/JWT-GUIDE.md`: JWT 인증이 실제 컨트롤러에 어떻게 녹는지
- `docs/CLASS-DIAGRAM.md`: 커머스 코어 엔티티 관계를 Mermaid로 빠르게 파악할 때

## 추천 방식

- 처음에는 모든 파일을 다 읽으려고 하지 말고, `공통 구조 -> 인증/유저 -> store -> bread` 순서로 먼저 끝내는 것이 좋습니다.
- 소스와 문서가 어긋나 보이면 먼저 `src/main/java`를 기준으로 이해하고, 그 다음 `docs/API.md`를 참고해 차이를 보는 편이 안전합니다.
- 현재 프로젝트를 빠르게 이해하는 목적이라면 테스트보다 서비스 코드를 먼저 읽고, 구현 의도를 확인할 때만 테스트를 뒤따라보는 것이 효율적입니다.
