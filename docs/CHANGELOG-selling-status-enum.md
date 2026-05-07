# SellingStatus Enum 도입 — 변경 내역

> 브랜치: `fix/store-status`  
> 기준 브랜치: `main`  
> 날짜: 2026-05-08

## 요약

매장 판매 상태를 기존 `isSelling: boolean` 단일 필드에서 3단계 enum `SellingStatus`로 세분화했습니다.  
기존 `isSelling` 필드는 하위 호환을 위해 유지합니다.

| 상태 | 의미 |
|------|------|
| `SELLING` | 영업시간 내 + 주문마감 전 + 재고 있음 |
| `OPEN_SOLD_OUT` | 영업시간 내 + 주문마감 전 + 재고 없음 |
| `CLOSED` | 비활성 / 휴무 / 영업시간 밖 / 주문마감 이후 |

## 변경 파일 목록

### 신규 파일

| 파일 | 설명 |
|------|------|
| `src/main/java/.../store/util/SellingStatus.java` | 3단계 판매 상태 enum 정의 |

### 수정 파일 (프로덕션 코드)

| 파일 | 변경 내용 |
|------|-----------|
| `src/main/java/.../store/util/SellingStatusUtil.java` | `getSellingStatus()` 메서드 2개 추가 (전날 자정 넘김 고려), 기존 `isSelling()`은 `getSellingStatus()`에 위임 |
| `src/main/java/.../store/dto/StoreDetailResponse.java` | `sellingStatus` 필드 추가, `of()` 팩토리가 `SellingStatus`를 받아 `isSelling` 파생 |
| `src/main/java/.../store/dto/NearbyStoreResponse.java` | `sellingStatus` 필드 추가 (`isSelling` 뒤에 배치) |
| `src/main/java/.../store/service/StoreService.java` | `getStoreDetail()`, `getNearbyStores()`에서 List 기반 `getSellingStatus()` 호출로 변경 |

### 수정 파일 (테스트)

| 파일 | 변경 내용 |
|------|-----------|
| `src/test/java/.../store/util/SellingStatusUtilTest.java` | `getSellingStatus()` 테스트 (자정 넘김 전날 row 7개 포함, 총 23개) |
| `src/test/java/.../store/service/StoreServiceTest.java` | `sellingStatus` 필드 검증 assertion 추가 |

### 수정 파일 (문서)

| 파일 | 변경 내용 |
|------|-----------|
| `docs/API.md` | 응답 예시에 `sellingStatus` 필드 추가 + enum 값 설명, boss 계정 범위 수정 (~120) |
| `docs/DB-SETUP.md` | boss 계정 범위 수정 (~120) |

## 핵심 설계 결정

### 1. 판별 로직 단일화

`SellingStatusUtil.getSellingStatus()`가 유일한 판별 로직 소스입니다.  
기존 `isSelling()`은 내부적으로 `getSellingStatus() == SELLING`으로 위임하므로 로직 중복이 없습니다.

### 2. 자정 넘김 영업 — 전날 row 고려

List 기반 `getSellingStatus(List, Clock)` 메서드는 오늘 row뿐 아니라 전날 row도 확인합니다.

우선순위:
1. 오늘 row 영업시간 안이면 → 오늘 row 기준으로 판별
2. 오늘 row가 CLOSED이고, 전날 row가 자정 넘김 영업이며 현재 시간이 전날 cutoffTime 이전이면 → 전날 row 기준으로 판별
3. 그 외 → CLOSED

예시: 월요일 22:00~03:00 영업인 매장을 화요일 01:00에 조회하면, 월요일 row의 연장 구간으로 인식하여 SELLING 또는 OPEN_SOLD_OUT을 반환합니다.

### 3. 24시간 영업 정책

시스템은 `startTime == endTime`을 등록 시 금지하고, `lastOrderTime`을 필수로 요구합니다.  
따라서 24시간 영업(`startTime == cutoffTime`)은 현재 시스템에서 도달 불가능한 상태입니다.  
`SellingStatusUtil`에서 24시간 판정 분기를 제거하여 등록 검증과 판별 로직의 일관성을 확보했습니다.

### 4. 하위 호환

- `isSelling` boolean 필드를 모든 응답 DTO에서 유지합니다.
- `isSelling == (sellingStatus == SELLING)` 관계가 항상 성립합니다.
- 기존 클라이언트는 `sellingStatus` 필드를 무시하면 됩니다.

### 5. 변경하지 않은 DTO

`FavouriteStoreResponse`, `BreadDetailResponse`, `NearbyBreadResponse`는 변경하지 않았습니다.  
이 DTO들은 기존 `isSelling` boolean을 그대로 사용합니다.

## API 응답 변경

### GET /api/store/nearby

```json
{
  "storeId": 1,
  "name": "투데이브레드 강남점",
  "isSelling": true,
  "sellingStatus": "SELLING",
  "distance": 0.35,
  ...
}
```

### GET /api/store/{storeId}

```json
{
  "store": { ... },
  "isSelling": true,
  "sellingStatus": "SELLING",
  "averageRating": 4.5,
  "reviewCount": 12
}
```

## SellingStatusUtil 판별 로직 흐름

### 단일 row 기반 (`getSellingStatus(boolean, StoreBusinessHoursEntity, boolean, LocalTime)`)

```
isActive == false?          → CLOSED
todayHours == null?         → CLOSED
todayHours.isClosed?        → CLOSED
startTime/endTime == null?  → CLOSED
                ↓
cutoffTime = lastOrderTime ?? endTime
                ↓
startTime <= cutoffTime?    → 일반 영업: startTime <= now < cutoffTime
startTime > cutoffTime?     → 자정 넘김: now >= startTime OR now < cutoffTime
                ↓
영업시간 밖?                → CLOSED
영업시간 내?                → hasStock ? SELLING : OPEN_SOLD_OUT
```

### List 기반 (`getSellingStatus(boolean, List, boolean, Clock)`)

```
isActive == false?          → CLOSED
                ↓
오늘 row로 단일 판별 → SELLING 또는 OPEN_SOLD_OUT?  → 그대로 반환
                ↓ (CLOSED인 경우)
전날 row 확인:
  - 전날 row가 자정 넘김 영업?
  - 현재 시간 < 전날 cutoffTime?
  → 둘 다 YES: hasStock ? SELLING : OPEN_SOLD_OUT
  → 아니면: CLOSED
```

## 테스트 커버리지

### SellingStatusUtilTest (23개 테스트)

- 기존 `isSelling()` 4개 유지
- 단일 row `getSellingStatus()` 12개:
  - CLOSED: inactive, null hours, isClosed, null startTime, 영업시간 전, lastOrderTime 이후, 자정넘김 영업시간 밖
  - SELLING: 일반 영업시간 내+재고, 자정넘김 영업시간 내+재고
  - OPEN_SOLD_OUT: 일반 영업시간 내+재고없음, 자정넘김+재고없음
  - 위임 검증: `isSelling() == (getSellingStatus() == SELLING)`
- List 기반 자정 넘김 전날 row 7개:
  - 토→일 자정 넘김 연장 구간 (SELLING)
  - 토→일 자정 넘김 연장 구간 재고 없음 (OPEN_SOLD_OUT)
  - 토→일 cutoff 지남 (CLOSED)
  - 오늘 row 우선 (오늘 영업시간 안이면 오늘 기준)
  - 전날이 자정 넘김이 아닌 경우 (CLOSED)
  - 월→화 자정 넘김 (SELLING)
  - 일→월 자정 넘김 — 요일 wrap-around (SELLING)

### StoreServiceTest

- `getStoreDetail`: `sellingStatus == SELLING`, `isSelling == true` 검증
- `getNearbyStores`: store별 `sellingStatus`와 `isSelling` 일관성 검증
