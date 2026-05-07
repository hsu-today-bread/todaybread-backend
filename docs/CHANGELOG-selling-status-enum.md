# API 변경 사항 — sellingStatus 필드 추가

> 브랜치: `fix/store-status`  
> 날짜: 2026-05-08

## 무엇이 바뀌었나요?

기존에 매장 판매 상태가 `isSelling: boolean` 하나였는데, **매장 상세 API**에 `sellingStatus` enum 필드가 추가됩니다.
이제 매장 상세 정보에 진입하면, 매장이 판매중인지, 영업 중이지만 재고가 없는지, 아예 영업 종료 인지 판별할 수 있습니다.

| 값 | 의미 | 기존 isSelling |
|----|------|----------------|
| `"SELLING"` | 영업중 + 재고 있음 | `true` |
| `"OPEN_SOLD_OUT"` | 영업중 + 재고 없음 (품절) | `false` |
| `"CLOSED"` | 영업 종료 / 휴무 / 비활성 | `false` |

기존 `isSelling` 필드는 그대로 유지됩니다. 새 필드를 안 쓰더라도 기존 동작에 영향 없습니다.

---

## 어떤 API에 추가되었나요?

### GET /api/store/{storeId} — 매장 상세

`sellingStatus` 필드가 `isSelling` 바로 뒤에 추가됩니다.

```json
{
  "store": { ... },
  "images": [ ... ],
  "breads": [ ... ],
  "isSelling": true,
  "sellingStatus": "SELLING",
  "averageRating": 4.5,
  "reviewCount": 12
}
```

---

## 추가되지 않은 API

아래 응답에는 `sellingStatus`가 **없습니다**. 기존 `isSelling`만 유지됩니다.

- GET /api/store/nearby — 근처 매장 목록
- GET /api/store/favourite — 찜한 매장 목록
- GET /api/bread/{breadId} — 빵 상세
- GET /api/bread/nearby — 근처 빵 목록

---

## 프론트에서 활용 방법

```typescript
type SellingStatus = "SELLING" | "OPEN_SOLD_OUT" | "CLOSED";

// 매장 상세 화면에서 상태별 UI 분기
switch (store.sellingStatus) {
  case "SELLING":
    // 초록색 "판매중" 뱃지
    break;
  case "OPEN_SOLD_OUT":
    // 주황색 "품절" 뱃지 (영업은 하고 있지만 재고 없음)
    break;
  case "CLOSED":
    // 회색 "영업종료" 뱃지
    break;
}
```

기존에 `isSelling: false`로만 처리하던 부분을 `OPEN_SOLD_OUT`과 `CLOSED`로 구분해서 보여줄 수 있습니다.

---

## 참고: sellingStatus 판별 기준

| 조건 | 결과 |
|------|------|
| 영업시간 내 + 주문마감 전 + 재고 있음 | `SELLING` |
| 영업시간 내 + 주문마감 전 + 재고 없음 | `OPEN_SOLD_OUT` |
| 영업시간 밖 / 주문마감 이후 / 휴무 / 비활성 | `CLOSED` |

자정 넘김 영업(예: 22:00~03:00)도 정상 처리됩니다.
