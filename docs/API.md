# API 문서

> Swagger UI: http://localhost:8080/swagger-ui/index.html
>
> 이 문서는 전체 API 목록과 에러 코드를 정리한 참고용 문서입니다.
> 실시간 API 스펙은 Swagger UI에서 확인하세요.

개발용 테스트 데이터가 필요하면 먼저 아래 스크립트를 실행하세요:

```bash
./gradlew bootRun
./scripts/test-data.sh
```

샘플 계정:

- `demo-user@todaybread.com` / `todaybread123`
- `demo-boss1@todaybread.com` ~ `demo-boss10@todaybread.com` / `todaybread123`

근처 매장/빵 조회 추천 좌표: `lat=37.4980950`, `lng=127.0276100`, `radius=5`

---

## API 목록

### 1. 인증 (Auth)

#### `POST /api/auth/reissue` — 토큰 재발급

| 항목 | 값 |
|------|-----|
| 인증 | X |

**요청 바디:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**응답 형식:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**에러 응답:** `AUTH_003`

---

#### `POST /api/auth/logout` — 로그아웃

| 항목 | 값 |
|------|-----|
| 인증 | O |

**응답 형식:**

```json
{
  "success": true
}
```

**에러 응답:** `AUTH_001`, `AUTH_002`

---

### 2. 사용자 (User)

#### `POST /api/user/register` — 회원가입

| 항목 | 값 |
|------|-----|
| 인증 | X |

**요청 바디:**

```json
{
  "email": "user@todaybread.com",
  "nickname": "빵순이",
  "name": "김빵순",
  "password": "todaybread123",
  "phoneNumber": "010-1234-5678"
}
```

**응답 형식:**

```json
{
  "success": true,
  "message": "회원가입 완료"
}
```

**에러 응답:** `USER_001`, `USER_002`, `USER_003`, `COMMON_001`

---

#### `POST /api/user/login` — 로그인

| 항목 | 값 |
|------|-----|
| 인증 | X |

**요청 바디:**

```json
{
  "email": "demo-user@todaybread.com",
  "password": "todaybread123"
}
```

**응답 형식:**

```json
{
  "success": true,
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "nickname": "demo-user",
  "name": "데모 유저",
  "phoneNumber": "010-7000-1001"
}
```

**에러 응답:** `USER_004`, `COMMON_001`

---

#### `GET /api/user/exist/email?value=` — 이메일 중복 확인

| 항목 | 값 |
|------|-----|
| 인증 | X |

**쿼리 파라미터:** `value` (이메일)

**응답 형식:**

```json
true
```

> `true`: 이미 존재, `false`: 사용 가능

---

#### `GET /api/user/exist/nickname?value=` — 닉네임 중복 확인

| 항목 | 값 |
|------|-----|
| 인증 | X |

**쿼리 파라미터:** `value` (닉네임)

**응답 형식:**

```json
false
```

---

#### `GET /api/user/exist/phone?value=` — 전화번호 중복 확인

| 항목 | 값 |
|------|-----|
| 인증 | X |

**쿼리 파라미터:** `value` (전화번호)

**응답 형식:**

```json
false
```

---

#### `PATCH /api/user/update-profile` — 프로필 수정

| 항목 | 값 |
|------|-----|
| 인증 | O |

**요청 바디:**

```json
{
  "nickname": "빵덕후",
  "name": "김빵순",
  "phoneNumber": "010-9876-5432"
}
```

**응답 형식:**

```json
{
  "nickname": "빵덕후",
  "name": "김빵순",
  "phoneNumber": "010-9876-5432"
}
```

**에러 응답:** `USER_003`, `USER_004`, `COMMON_001`

---

#### `POST /api/user/boss-approve` — 사장님 등록

| 항목 | 값 |
|------|-----|
| 인증 | O |

**요청 바디:**

```json
{
  "bossNumber": "123-45-67890"
}
```

**응답 형식:**

```json
{
  "success": true,
  "message": "사업자 등록이 완료되었습니다.",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

> 사장님 등록 후 역할이 BOSS로 변경되므로 새 토큰이 발급됩니다.

**에러 응답:** `USER_004`, `USER_006`, `USER_007`

---

### 3. 계정 복구 (User Recovery)

> 아래 API는 인증 없이 접근 가능합니다. 현재 별도의 인증 토큰(OTP 등) 없이 동작하므로,
> 운영 배포 전 Rate Limiting 또는 일회용 토큰 검증 추가를 권장합니다.

#### `GET /api/user/find-email?phone=` — 이메일 찾기

| 항목 | 값 |
|------|-----|
| 인증 | X |

**쿼리 파라미터:** `phone` (전화번호)

**응답 형식:**

```json
{
  "maskedEmail": "de****@todaybread.com"
}
```

**에러 응답:** `USER_005`

---

#### `GET /api/user/verify-identity?phone=&email=` — 본인 확인

| 항목 | 값 |
|------|-----|
| 인증 | X |

**쿼리 파라미터:** `phone` (전화번호), `email` (이메일)

**응답 형식:**

```json
{
  "verified": true,
  "email": "demo-user@todaybread.com"
}
```

**에러 응답:** `USER_005`

---

#### `POST /api/user/reset-password` — 비밀번호 재설정

| 항목 | 값 |
|------|-----|
| 인증 | X |

**요청 바디:**

```json
{
  "email": "demo-user@todaybread.com",
  "newPassword": "newpassword123"
}
```

**응답 형식:**

```json
{
  "success": true,
  "message": "비밀번호가 재설정되었습니다."
}
```

> 비밀번호 재설정 시 기존 세션(Refresh Token)이 모두 무효화됩니다.

**에러 응답:** `USER_005`, `COMMON_001`

---

### 4. 빵 — 일반 유저 (Bread)

#### `GET /api/bread/nearby?lat=&lng=&radius=&sort=` — 근처 빵 목록

| 항목 | 값 |
|------|-----|
| 인증 | O |

**쿼리 파라미터:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `lat` | BigDecimal | O | — | 위도 (-90 ~ 90) |
| `lng` | BigDecimal | O | — | 경도 (-180 ~ 180) |
| `radius` | int | X | 1 | 검색 반경 km (1 ~ 10) |
| `sort` | String | X | none | 정렬: `none`(랜덤 셔플), `distance`, `price`, `discount` |

**응답 형식:**

```json
[
  {
    "id": 1,
    "name": "시그니처 소금빵",
    "originalPrice": 3500,
    "salePrice": 2500,
    "imageUrl": "/images/bread/1.jpg",
    "storeId": 1,
    "storeName": "투데이브레드 데모 강남점",
    "isSelling": true,
    "lastOrderTime": "22:30:00",
    "distance": 0.35,
    "averageRating": 4.5,
    "reviewCount": 12
  }
]
```

> `sort=none`(기본값)은 의도적으로 랜덤 셔플합니다. 품절된 빵은 목록에서 제외됩니다.

**에러 응답:** `COMMON_001`

---

#### `GET /api/bread/detail/{breadId}` — 빵 상세 조회

| 항목 | 값 |
|------|-----|
| 인증 | O |

**경로 변수:** `breadId` (빵 ID)

**응답 형식:**

```json
{
  "id": 1,
  "name": "시그니처 소금빵",
  "originalPrice": 3500,
  "salePrice": 2500,
  "remainingQuantity": 14,
  "description": "겉은 바삭하고 속은 촉촉한 대표 메뉴입니다.",
  "imageUrl": "/images/bread/1.jpg",
  "storeId": 1,
  "storeName": "투데이브레드 데모 강남점",
  "isSelling": true,
  "averageRating": 4.5,
  "reviewCount": 12
}
```

**에러 응답:** `BREAD_001`

---

#### `GET /api/bread/{storeId}` — 가게별 메뉴 목록

| 항목 | 값 |
|------|-----|
| 인증 | O |

**경로 변수:** `storeId` (가게 ID)

**응답 형식:**

```json
[
  {
    "id": 1,
    "storeId": 1,
    "name": "시그니처 소금빵",
    "originalPrice": 3500,
    "salePrice": 2500,
    "remainingQuantity": 14,
    "description": "겉은 바삭하고 속은 촉촉한 대표 메뉴입니다.",
    "imageUrl": "/images/bread/1.jpg"
  }
]
```

**에러 응답:** `STORE_004`

---

### 5. 빵 — 사장님 (Bread Boss)

#### `GET /api/boss/bread` — 내 가게 메뉴 목록

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |

**응답 형식:**

```json
[
  {
    "id": 1,
    "storeId": 1,
    "name": "시그니처 소금빵",
    "originalPrice": 3500,
    "salePrice": 2500,
    "remainingQuantity": 14,
    "description": "겉은 바삭하고 속은 촉촉한 대표 메뉴입니다.",
    "imageUrl": "/images/bread/1.jpg"
  }
]
```

**에러 응답:** `STORE_001`, `STORE_004`

---

#### `POST /api/boss/bread` — 메뉴 등록 (multipart)

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |
| Content-Type | `multipart/form-data` |

**요청 파트:**

- `request` (JSON):
```json
{
  "name": "크림치즈 베이글",
  "originalPrice": 4000,
  "salePrice": 2800,
  "remainingQuantity": 10,
  "description": "부드러운 크림치즈가 듬뿍 들어간 베이글입니다."
}
```
- `image` (파일, 선택): 빵 이미지

**응답 형식:**

```json
{
  "id": 4,
  "storeId": 1,
  "name": "크림치즈 베이글",
  "originalPrice": 4000,
  "salePrice": 2800,
  "remainingQuantity": 10,
  "description": "부드러운 크림치즈가 듬뿍 들어간 베이글입니다.",
  "imageUrl": "/images/bread/4.jpg"
}
```

**에러 응답:** `STORE_001`, `STORE_004`, `BREAD_004`, `COMMON_001`, `COMMON_005`, `COMMON_006`, `COMMON_007`

---

#### `PUT /api/boss/bread/{breadId}` — 메뉴 수정 (multipart)

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |
| Content-Type | `multipart/form-data` |

**경로 변수:** `breadId` (빵 ID)

**요청 파트:**

- `request` (JSON):
```json
{
  "name": "크림치즈 베이글",
  "originalPrice": 4500,
  "salePrice": 3000,
  "remainingQuantity": 15,
  "description": "리뉴얼! 크림치즈가 더 듬뿍."
}
```
- `image` (파일, 선택): 새 이미지

**응답 형식:**

```json
{
  "id": 4,
  "storeId": 1,
  "name": "크림치즈 베이글",
  "originalPrice": 4500,
  "salePrice": 3000,
  "remainingQuantity": 15,
  "description": "리뉴얼! 크림치즈가 더 듬뿍.",
  "imageUrl": "/images/bread/4.jpg"
}
```

**에러 응답:** `BREAD_001`, `BREAD_002`, `BREAD_004`, `COMMON_001`, `COMMON_005`, `COMMON_006`, `COMMON_007`

---

#### `PATCH /api/boss/bread/{breadId}/stock` — 재고 변경 / 품절 처리

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |

**경로 변수:** `breadId` (빵 ID)

**요청 바디:**

```json
{
  "remainingQuantity": 0
}
```

> `remainingQuantity`를 0으로 설정하면 품절 처리됩니다.

**응답 형식:**

```json
{
  "success": true
}
```

**에러 응답:** `BREAD_001`, `BREAD_002`, `COMMON_001`

---

#### `DELETE /api/boss/bread/{breadId}` — 메뉴 삭제

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |

**경로 변수:** `breadId` (빵 ID)

**응답 형식:**

```json
{
  "success": true
}
```

**에러 응답:** `BREAD_001`, `BREAD_002`

---

### 6. 매장 — 일반 유저 (Store)

#### `GET /api/store/nearby?lat=&lng=&radius=` — 근처 가게 목록

| 항목 | 값 |
|------|-----|
| 인증 | O |

**쿼리 파라미터:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `lat` | BigDecimal | O | — | 위도 (-90 ~ 90) |
| `lng` | BigDecimal | O | — | 경도 (-180 ~ 180) |
| `radius` | int | X | 1 | 검색 반경 km (1 ~ 10) |

**응답 형식:**

```json
[
  {
    "storeId": 1,
    "name": "투데이브레드 데모 강남점",
    "storeAddressLine1": "서울특별시 강남구 테헤란로 123",
    "storeAddressLine2": "1층",
    "latitude": 37.4980950,
    "longitude": 127.0276100,
    "primaryImageUrl": "/images/store/1_0.jpg",
    "isSelling": true,
    "distance": 0.35,
    "lastOrderTime": "22:30:00"
  }
]
```

**에러 응답:** `COMMON_001`

---

#### `GET /api/store/{storeId}` — 가게 상세 조회

| 항목 | 값 |
|------|-----|
| 인증 | O |

**경로 변수:** `storeId` (가게 ID)

**응답 형식:**

```json
{
  "store": {
    "id": 1,
    "name": "투데이브레드 데모 강남점",
    "phone": "02-7000-3001",
    "description": "강남역 근처에서 소금빵과 식사용 빵을 판매하는 프론트 연동용 데모 매장입니다.",
    "addressLine1": "서울특별시 강남구 테헤란로 123",
    "addressLine2": "1층",
    "latitude": 37.4980950,
    "longitude": 127.0276100,
    "businessHours": [
      {
        "dayOfWeek": 1,
        "isClosed": false,
        "startTime": "07:00:00",
        "endTime": "23:00:00",
        "lastOrderTime": "22:30:00"
      },
      {
        "dayOfWeek": 7,
        "isClosed": true,
        "startTime": null,
        "endTime": null,
        "lastOrderTime": null
      }
    ]
  },
  "images": [
    {
      "id": 1,
      "imageUrl": "/images/store/1_0.jpg",
      "displayOrder": 0
    }
  ],
  "breads": [
    {
      "id": 1,
      "storeId": 1,
      "name": "시그니처 소금빵",
      "originalPrice": 3500,
      "salePrice": 2500,
      "remainingQuantity": 14,
      "description": "겉은 바삭하고 속은 촉촉한 대표 메뉴입니다.",
      "imageUrl": "/images/bread/1.jpg"
    }
  ],
  "isSelling": true
}
```

**에러 응답:** `STORE_004`

---

### 7. 매장 — 사장님 (Store Boss)

#### `GET /api/boss/store/status` — 가게 등록 상태 조회

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |

**응답 형식:**

```json
{
  "hasStore": true
}
```

---

#### `GET /api/boss/store` — 내 가게 정보 + 이미지 조회

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |

**응답 형식:**

```json
{
  "store": {
    "id": 1,
    "name": "투데이브레드 데모 강남점",
    "phone": "02-7000-3001",
    "description": "강남역 근처에서 소금빵과 식사용 빵을 판매하는 프론트 연동용 데모 매장입니다.",
    "addressLine1": "서울특별시 강남구 테헤란로 123",
    "addressLine2": "1층",
    "latitude": 37.4980950,
    "longitude": 127.0276100,
    "businessHours": [
      {
        "dayOfWeek": 1,
        "isClosed": false,
        "startTime": "07:00:00",
        "endTime": "23:00:00",
        "lastOrderTime": "22:30:00"
      }
    ]
  },
  "images": [
    {
      "id": 1,
      "imageUrl": "/images/store/1_0.jpg",
      "displayOrder": 0
    },
    {
      "id": 2,
      "imageUrl": "/images/store/1_1.jpg",
      "displayOrder": 1
    }
  ]
}
```

**에러 응답:** `STORE_001`, `STORE_004`

---

#### `POST /api/boss/store` — 가게 등록 (multipart)

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |
| Content-Type | `multipart/form-data` |

**요청 파트:**

- `request` (JSON):
```json
{
  "name": "투데이브레드 강남점",
  "phone": "02-7000-3001",
  "description": "강남역 근처 빵집입니다.",
  "addressLine1": "서울특별시 강남구 테헤란로 123",
  "addressLine2": "1층",
  "latitude": 37.4980950,
  "longitude": 127.0276100,
  "businessHours": [
    {
      "dayOfWeek": 1,
      "isClosed": false,
      "startTime": "07:00:00",
      "endTime": "23:00:00",
      "lastOrderTime": "22:30:00"
    },
    {
      "dayOfWeek": 2,
      "isClosed": false,
      "startTime": "07:00:00",
      "endTime": "23:00:00",
      "lastOrderTime": "22:30:00"
    },
    {
      "dayOfWeek": 3,
      "isClosed": false,
      "startTime": "07:00:00",
      "endTime": "23:00:00",
      "lastOrderTime": "22:30:00"
    },
    {
      "dayOfWeek": 4,
      "isClosed": false,
      "startTime": "07:00:00",
      "endTime": "23:00:00",
      "lastOrderTime": "22:30:00"
    },
    {
      "dayOfWeek": 5,
      "isClosed": false,
      "startTime": "07:00:00",
      "endTime": "23:00:00",
      "lastOrderTime": "22:30:00"
    },
    {
      "dayOfWeek": 6,
      "isClosed": false,
      "startTime": "07:00:00",
      "endTime": "23:00:00",
      "lastOrderTime": "22:30:00"
    },
    {
      "dayOfWeek": 7,
      "isClosed": true,
      "startTime": null,
      "endTime": null,
      "lastOrderTime": null
    }
  ]
}
```
- `images` (파일 목록, 1~5장): 가게 이미지

**응답 형식:**

```json
{
  "store": {
    "id": 1,
    "name": "투데이브레드 강남점",
    "phone": "02-7000-3001",
    "description": "강남역 근처 빵집입니다.",
    "addressLine1": "서울특별시 강남구 테헤란로 123",
    "addressLine2": "1층",
    "latitude": 37.4980950,
    "longitude": 127.0276100,
    "businessHours": [
      {
        "dayOfWeek": 1,
        "isClosed": false,
        "startTime": "07:00:00",
        "endTime": "23:00:00",
        "lastOrderTime": "22:30:00"
      }
    ]
  },
  "images": [
    {
      "id": 1,
      "imageUrl": "/images/store/1_0.jpg",
      "displayOrder": 0
    }
  ]
}
```

**에러 응답:** `STORE_001`, `STORE_002`, `STORE_003`, `STORE_005`, `STORE_006`, `COMMON_001`, `COMMON_005`, `COMMON_006`, `COMMON_007`, `STORE_IMAGE_002`

---

#### `PUT /api/boss/store` — 가게 정보 + 영업시간 수정

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |

**요청 바디:**

```json
{
  "name": "투데이브레드 강남점",
  "phone": "02-7000-3001",
  "description": "리뉴얼 오픈! 강남역 근처 빵집입니다.",
  "addressLine1": "서울특별시 강남구 테헤란로 123",
  "addressLine2": "1층",
  "latitude": 37.4980950,
  "longitude": 127.0276100,
  "businessHours": [
    {
      "dayOfWeek": 1,
      "isClosed": false,
      "startTime": "08:00:00",
      "endTime": "22:00:00",
      "lastOrderTime": "21:30:00"
    }
  ]
}
```

**응답 형식:**

```json
{
  "id": 1,
  "name": "투데이브레드 강남점",
  "phone": "02-7000-3001",
  "description": "리뉴얼 오픈! 강남역 근처 빵집입니다.",
  "addressLine1": "서울특별시 강남구 테헤란로 123",
  "addressLine2": "1층",
  "latitude": 37.4980950,
  "longitude": 127.0276100,
  "businessHours": [
    {
      "dayOfWeek": 1,
      "isClosed": false,
      "startTime": "08:00:00",
      "endTime": "22:00:00",
      "lastOrderTime": "21:30:00"
    }
  ]
}
```

**에러 응답:** `STORE_001`, `STORE_003`, `STORE_004`, `STORE_005`, `STORE_006`, `COMMON_001`

---

#### `PUT /api/boss/store/images` — 가게 이미지 일괄 교체 (multipart)

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |
| Content-Type | `multipart/form-data` |

**요청 파트:** `images` (파일 목록, 1~5장)

**응답 형식:**

```json
[
  {
    "id": 10,
    "imageUrl": "/images/store/1_0.jpg",
    "displayOrder": 0
  },
  {
    "id": 11,
    "imageUrl": "/images/store/1_1.jpg",
    "displayOrder": 1
  }
]
```

**에러 응답:** `STORE_001`, `STORE_004`, `STORE_IMAGE_002`, `COMMON_005`, `COMMON_006`, `COMMON_007`

---

### 8. 주문 — 사장님 (Boss Orders)

#### `GET /api/boss/orders?page=&size=` — 픽업 대기 주문 목록

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |

**쿼리 파라미터:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | int | X | 0 | 페이지 번호 (0부터) |
| `size` | int | X | 20 | 페이지 크기 (최대 100) |

**응답 형식 (Spring Page):**

```json
{
  "content": [
    {
      "orderId": 42,
      "orderNumber": "W6X7",
      "totalAmount": 7500,
      "createdAt": "2026-04-15T18:30:00",
      "items": [
        {
          "breadName": "시그니처 소금빵",
          "breadPrice": 2500,
          "quantity": 3
        }
      ]
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 2,
  "totalPages": 1,
  "last": true,
  "first": true,
  "empty": false
}
```

**에러 응답:** `STORE_001`, `STORE_004`

---

#### `POST /api/boss/orders/{orderId}/pickup` — 픽업 완료 처리

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |

**경로 변수:** `orderId` (주문 ID)

**응답 형식:** 응답 바디 없음 (HTTP 200)

**에러 응답:** `ORDER_001`, `ORDER_002`, `ORDER_003`

---

### 9. 매출 — 사장님 (Boss Sales)

#### `GET /api/boss/sales/daily?date=` — 일별 매출 조회

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |

**쿼리 파라미터:** `date` (ISO DATE, 예: `2026-04-15`)

**응답 형식:**

```json
{
  "totalSales": 18900,
  "totalQuantity": 6,
  "items": [
    {
      "breadId": 1,
      "breadName": "시그니처 소금빵",
      "breadPrice": 2500,
      "totalQuantity": 4,
      "totalSales": 10000
    },
    {
      "breadId": 2,
      "breadName": "바질 치아바타",
      "breadPrice": 3900,
      "totalQuantity": 2,
      "totalSales": 7800
    }
  ]
}
```

> 삭제된 메뉴의 `breadId`는 `null`로 표시됩니다.

**에러 응답:** `STORE_001`, `STORE_004`

---

#### `GET /api/boss/sales/monthly?year=&month=` — 월별 매출 조회

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 권한 | BOSS |

**쿼리 파라미터:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `year` | int | O | 연도 (2000 이상) |
| `month` | int | O | 월 (1 ~ 12) |

**응답 형식:**

```json
{
  "totalSales": 76000,
  "totalQuantity": 28,
  "dailySales": [
    {
      "date": "2026-03-02",
      "totalSales": 7800
    },
    {
      "date": "2026-03-04",
      "totalSales": 9600
    }
  ],
  "items": [
    {
      "breadId": 1,
      "breadName": "시그니처 소금빵",
      "breadPrice": 2500,
      "totalQuantity": 10,
      "totalSales": 25000
    },
    {
      "breadId": 2,
      "breadName": "바질 치아바타",
      "breadPrice": 3900,
      "totalQuantity": 5,
      "totalSales": 19500
    }
  ]
}
```

**에러 응답:** `STORE_001`, `STORE_004`, `COMMON_001`

---

### 10. 키워드 (Keyword)

#### `POST /api/keywords` — 키워드 등록

| 항목 | 값 |
|------|-----|
| 인증 | O |

**요청 바디:**

```json
{
  "keyword": "크루아상"
}
```

**응답 형식:**

```json
{
  "success": true
}
```

**에러 응답:** `KEYWORD_001`, `KEYWORD_002`, `KEYWORD_003`, `COMMON_001`

---

#### `GET /api/keywords` — 내 키워드 목록 조회

| 항목 | 값 |
|------|-----|
| 인증 | O |

**응답 형식:**

```json
[
  {
    "userKeywordId": 1,
    "displayText": "크루아상"
  },
  {
    "userKeywordId": 2,
    "displayText": "소금빵"
  }
]
```

---

#### `DELETE /api/keywords/{userKeywordId}` — 키워드 삭제

| 항목 | 값 |
|------|-----|
| 인증 | O |

**경로 변수:** `userKeywordId` (사용자-키워드 관계 ID)

**응답 형식:**

```json
{
  "success": true,
  "message": "키워드가 삭제되었습니다."
}
```

**에러 응답:** `KEYWORD_004`, `KEYWORD_005`

---

### 11. 단골 가게 (Favourite Store)

#### `POST /api/favourite-stores` — 단골 가게 토글 (추가/해제)

| 항목 | 값 |
|------|-----|
| 인증 | O |

**요청 바디:**

```json
{
  "storeId": 1
}
```

**응답 형식:**

```json
{
  "added": true
}
```

> `added: true` → 단골 추가됨, `added: false` → 단골 해제됨

**에러 응답:** `STORE_004`, `FAVOURITE_STORE_001`

---

#### `GET /api/favourite-stores` — 단골 가게 목록 조회

| 항목 | 값 |
|------|-----|
| 인증 | O |

**응답 형식:**

```json
[
  {
    "storeId": 1,
    "name": "투데이브레드 데모 강남점",
    "address": "서울특별시 강남구 테헤란로 123 1층",
    "imageUrl": "/images/store/1_0.jpg",
    "isSelling": true
  }
]
```

---

### 12. 찜목록 (Wishlist)

#### `GET /api/wishlist` — 찜목록 통합 조회

| 항목 | 값 |
|------|-----|
| 인증 | O |

**응답 형식:**

```json
{
  "keywords": [
    {
      "userKeywordId": 1,
      "displayText": "크루아상"
    },
    {
      "userKeywordId": 2,
      "displayText": "소금빵"
    }
  ],
  "favouriteStores": [
    {
      "storeId": 1,
      "name": "투데이브레드 데모 강남점",
      "address": "서울특별시 강남구 테헤란로 123 1층",
      "imageUrl": "/images/store/1_0.jpg",
      "isSelling": true
    }
  ]
}
```

---

### 13. 장바구니 (Cart)

#### `POST /api/cart` — 장바구니에 빵 추가

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 응답 코드 | 201 Created |

**요청 바디:**

```json
{
  "breadId": 1,
  "quantity": 2
}
```

**응답 형식:** 응답 바디 없음 (HTTP 201)

**에러 응답:** `BREAD_001`, `CART_001`, `COMMON_001`

---

#### `GET /api/cart` — 장바구니 조회

| 항목 | 값 |
|------|-----|
| 인증 | O |

**응답 형식:**

```json
{
  "storeName": "투데이브레드 데모 강남점",
  "lastOrderTime": "22:30:00",
  "items": [
    {
      "cartItemId": 1,
      "breadId": 1,
      "breadName": "시그니처 소금빵",
      "description": "겉은 바삭하고 속은 촉촉한 대표 메뉴입니다.",
      "quantity": 2,
      "imageUrl": "/images/bread/1.jpg",
      "salePrice": 2500
    }
  ]
}
```

---

#### `PATCH /api/cart/items/{cartItemId}` — 장바구니 수량 변경

| 항목 | 값 |
|------|-----|
| 인증 | O |

**경로 변수:** `cartItemId` (장바구니 항목 ID)

**요청 바디:**

```json
{
  "quantity": 3
}
```

**응답 형식:** 응답 바디 없음 (HTTP 200)

**에러 응답:** `CART_002`, `COMMON_001`

---

#### `DELETE /api/cart/items/{cartItemId}` — 장바구니 항목 삭제

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 응답 코드 | 204 No Content |

**경로 변수:** `cartItemId` (장바구니 항목 ID)

**응답 형식:** 응답 바디 없음 (HTTP 204)

**에러 응답:** `CART_002`

---

#### `DELETE /api/cart` — 장바구니 비우기

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 응답 코드 | 204 No Content |

**응답 형식:** 응답 바디 없음 (HTTP 204)

---

### 14. 주문 (Order)

> 주문 생성 API(`POST /api/orders/cart`, `POST /api/orders/direct`)는 `Idempotency-Key` 헤더가 필수입니다.
> 네트워크 오류 등으로 응답을 받지 못했을 때 같은 key로 재요청하면 동일한 주문 결과를 반환합니다.
> 새로운 주문을 생성하려면 반드시 새로운 key를 사용하세요. (UUID v4 권장)

#### `POST /api/orders/cart` — 장바구니 기반 주문 생성

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 필수 헤더 | `Idempotency-Key` |

**응답 형식:**

```json
{
  "orderId": 42,
  "storeName": "투데이브레드 데모 강남점",
  "status": "PENDING",
  "totalAmount": 7500,
  "orderNumber": "W6X7",
  "createdAt": "2026-04-15T18:30:00",
  "items": [
    {
      "breadName": "시그니처 소금빵",
      "breadPrice": 2500,
      "quantity": 3
    }
  ]
}
```

**에러 응답:** `CART_003`, `BREAD_003`, `COMMON_008`, `ORDER_004`

---

#### `POST /api/orders/direct` — 바로 구매

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 필수 헤더 | `Idempotency-Key` |

**요청 바디:**

```json
{
  "breadId": 1,
  "quantity": 2
}
```

**응답 형식:**

```json
{
  "orderId": 43,
  "storeName": "투데이브레드 데모 강남점",
  "status": "PENDING",
  "totalAmount": 5000,
  "orderNumber": "A1B2",
  "createdAt": "2026-04-15T19:00:00",
  "items": [
    {
      "breadName": "시그니처 소금빵",
      "breadPrice": 2500,
      "quantity": 2
    }
  ]
}
```

**에러 응답:** `BREAD_001`, `BREAD_003`, `COMMON_001`, `COMMON_008`, `ORDER_004`

---

#### `POST /api/orders/{orderId}/cancel` — 주문 취소

| 항목 | 값 |
|------|-----|
| 인증 | O |

**경로 변수:** `orderId` (주문 ID)

**응답 형식:** 응답 바디 없음 (HTTP 200)

**에러 응답:** `ORDER_001`, `ORDER_002`, `ORDER_003`

---

#### `GET /api/orders?page=&size=` — 주문 내역 목록

| 항목 | 값 |
|------|-----|
| 인증 | O |

**쿼리 파라미터:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | int | X | 0 | 페이지 번호 (0부터) |
| `size` | int | X | 20 | 페이지 크기 (최대 100) |

**응답 형식 (Spring Page):**

```json
{
  "content": [
    {
      "orderId": 42,
      "storeName": "투데이브레드 데모 강남점",
      "status": "CONFIRMED",
      "totalAmount": 7500,
      "orderNumber": "W6X7",
      "createdAt": "2026-04-15T18:30:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 15,
  "totalPages": 1,
  "last": true,
  "first": true,
  "empty": false
}
```

> 주문 상태: `PENDING`, `CONFIRMED`, `CANCELLED`, `PICKED_UP`

---

#### `GET /api/orders/{orderId}` — 주문 상세 조회

| 항목 | 값 |
|------|-----|
| 인증 | O |

**경로 변수:** `orderId` (주문 ID)

**응답 형식:**

```json
{
  "orderId": 42,
  "storeName": "투데이브레드 데모 강남점",
  "status": "CONFIRMED",
  "totalAmount": 7500,
  "orderNumber": "W6X7",
  "createdAt": "2026-04-15T18:30:00",
  "items": [
    {
      "breadName": "시그니처 소금빵",
      "breadPrice": 2500,
      "quantity": 3
    }
  ]
}
```

**에러 응답:** `ORDER_001`, `ORDER_002`

---

### 15. 결제 (Payment)

> 결제 승인 확정 API(`POST /api/payments/confirm`)는 `Idempotency-Key` 헤더가 필수입니다.
> 같은 key로 재요청하면 토스 Confirm API를 중복 호출하지 않고 기존 결제 결과를 반환합니다.
> 결제 실패 후 재시도할 때는 새로운 key를 사용하세요. (UUID v4 권장)

#### `POST /api/payments/confirm` — 결제 승인 확정

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 필수 헤더 | `Idempotency-Key` |

**요청 바디:**

```json
{
  "paymentKey": "tgen_20250101010101ABCDE",
  "orderId": 42,
  "amount": 7500
}
```

**응답 형식:**

```json
{
  "paymentId": 1,
  "orderId": 42,
  "amount": 7500,
  "status": "APPROVED",
  "paidAt": "2026-04-15T18:31:00",
  "method": "카드"
}
```

> 결제 상태: `PENDING`, `APPROVED`, `FAILED`, `CANCELLED`

**에러 응답:** `PAYMENT_001`, `PAYMENT_003`, `PAYMENT_004`, `PAYMENT_005`, `PAYMENT_008`, `ORDER_001`

---

#### `GET /api/payments/client-key` — 토스 Client Key 조회

| 항목 | 값 |
|------|-----|
| 인증 | X |

**응답 형식:**

```json
{
  "clientKey": "test_ck_..."
}
```

> 프론트엔드에서 토스 결제 위젯을 초기화할 때 사용합니다. Client Key는 공개 키이므로 인증 없이 접근 가능합니다.

---

#### ~~`POST /api/payments` — 결제 요청~~ (Deprecated)

> **⚠️ Deprecated:** 이 엔드포인트는 `POST /api/payments/confirm`으로 대체되었습니다. 토스 페이먼츠 연동에 따라 새 엔드포인트를 사용하세요.

| 항목 | 값 |
|------|-----|
| 인증 | O |
| 필수 헤더 | `Idempotency-Key` |

**요청 바디:**

```json
{
  "orderId": 42,
  "amount": 7500
}
```

**응답 형식:**

```json
{
  "paymentId": 1,
  "orderId": 42,
  "amount": 7500,
  "status": "APPROVED",
  "paidAt": "2026-04-15T18:31:00"
}
```

**에러 응답:** `PAYMENT_001`, `PAYMENT_002`, `PAYMENT_003`, `ORDER_001`, `COMMON_008`

---

### 16. 시스템 (System)

#### `GET /api/system/health` — 서버 상태 확인

| 항목 | 값 |
|------|-----|
| 인증 | X |

**응답 형식:**

```
"UP"
```

---

## 인증 구조

- JWT (HMAC-SHA256) 기반 stateless 인증
- 비밀번호 암호화: Argon2
- 역할 계층: `BOSS > USER`
- 인증 불필요 경로 (permitAll):
  - `/api/user/register` — 회원가입
  - `/api/user/login` — 로그인
  - `/api/user/exist/**` — 이메일/닉네임/전화번호 중복확인
  - `/api/auth/reissue` — 토큰 재발급
  - `/api/user/find-email` — 이메일 찾기
  - `/api/user/verify-identity` — 본인 확인
  - `/api/user/reset-password` — 비밀번호 재설정
  - `/api/payments/client-key` — 토스 Client Key 조회
  - `/api/system/health` — 헬스체크
  - `/swagger-ui/**` — Swagger UI
  - `/v3/api-docs/**` — OpenAPI 스펙
  - `/images/**` — 이미지 정적 파일
- 그 외 모든 경로는 `Authorization: Bearer {accessToken}` 필요
- 사장님 전용 API (`/api/boss/**`)는 `@PreAuthorize("hasRole('BOSS')")` 적용

---

## 공통 에러 응답 형식

모든 에러는 아래 형식으로 반환됩니다:

```json
{
  "code": "ERROR_CODE",
  "message": "에러 메시지"
}
```

---

## 에러 코드

### 공통 (COMMON)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `COMMON_001` | 400 | 요청값 검증에 실패했습니다. |
| `COMMON_002` | 405 | 허용되지 않은 HTTP 메서드입니다. |
| `COMMON_003` | 500 | 서버 내부 오류입니다. |
| `COMMON_004` | 403 | 접근 권한이 없습니다. |
| `COMMON_005` | 400 | 파일 크기는 5MB를 초과할 수 없습니다. |
| `COMMON_006` | 400 | 허용되지 않는 파일 형식입니다. (jpeg, png, gif, webp만 가능) |
| `COMMON_007` | 500 | 파일 저장에 실패했습니다. |
| `COMMON_008` | 409 | 중복된 데이터가 존재합니다. |

### 유저 (USER)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `USER_001` | 409 | 이미 가입한 이메일입니다. |
| `USER_002` | 409 | 이미 가입한 전화번호입니다. |
| `USER_003` | 409 | 이미 사용중인 닉네임입니다. |
| `USER_004` | 404 | 사용자를 찾을 수 없습니다. |
| `USER_005` | 404 | 가입 정보를 찾을 수 없습니다. |
| `USER_006` | 409 | 이미 사장님 등록이 완료된 상태입니다. |
| `USER_007` | 400 | 사업자 번호 형식이 맞지 않습니다. |

### 키워드 (KEYWORD)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `KEYWORD_001` | 409 | 이미 등록된 키워드입니다. |
| `KEYWORD_002` | 400 | 키워드는 최대 5개까지 등록할 수 있습니다. |
| `KEYWORD_003` | 400 | 키워드는 최대 10자까지 입력할 수 있습니다. |
| `KEYWORD_004` | 404 | 키워드를 찾을 수 없습니다. |
| `KEYWORD_005` | 403 | 해당 키워드에 대한 권한이 없습니다. |

### 매장 (STORE)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `STORE_001` | 403 | 사장님 등록 후 이용 가능한 기능입니다. |
| `STORE_002` | 409 | 이미 등록된 가게가 있습니다. |
| `STORE_003` | 409 | 가게 전화번호가 중복이 됩니다. |
| `STORE_004` | 404 | 가게를 찾을 수 없습니다. |
| `STORE_005` | 400 | 영업시간 데이터가 올바르지 않습니다. |
| `STORE_006` | 400 | 요일 데이터가 중복됩니다. |

### 매장 이미지 (STORE_IMAGE)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `STORE_IMAGE_001` | 404 | 이미지를 찾을 수 없습니다. |
| `STORE_IMAGE_002` | 400 | 이미지는 최대 5장까지 등록할 수 있습니다. |

### 단골 가게 (FAVOURITE_STORE)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `FAVOURITE_STORE_001` | 400 | 단골 가게는 최대 5개까지 등록할 수 있습니다. |

### 빵 (BREAD)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `BREAD_001` | 404 | 상품을 찾을 수 없습니다. |
| `BREAD_002` | 403 | 상품에 접근할 권한이 없습니다. |
| `BREAD_003` | 409 | 해당 상품의 재고가 부족합니다. |
| `BREAD_004` | 400 | 가격은 0원 이상이여야 합니다. |

> 빵 이미지 관련 에러(파일 형식, 크기, 저장 실패)는 공통 에러 코드 `COMMON_005`, `COMMON_006`, `COMMON_007`을 사용합니다.

### 인증 (AUTH)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `AUTH_001` | 401 | Access 토큰이 만료되었습니다. |
| `AUTH_002` | 401 | 유효하지 않은 Access 토큰입니다. |
| `AUTH_003` | 401 | 유효하지 않은 Refresh 토큰입니다. |

### 장바구니 (CART)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `CART_001` | 409 | 장바구니에는 하나의 매장 빵만 담을 수 있습니다. |
| `CART_002` | 404 | 장바구니 항목을 찾을 수 없습니다. |
| `CART_003` | 400 | 장바구니가 비어 있습니다. |

### 주문 (ORDER)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `ORDER_001` | 404 | 주문을 찾을 수 없습니다. |
| `ORDER_002` | 403 | 주문에 접근할 권한이 없습니다. |
| `ORDER_003` | 409 | 변경할 수 없는 주문 상태입니다. |
| `ORDER_004` | 500 | 주문 번호 생성에 실패했습니다. |

### 결제 (PAYMENT)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `PAYMENT_001` | 400 | 결제 금액이 주문 금액과 일치하지 않습니다. |
| `PAYMENT_002` | 400 | 결제 금액은 0보다 커야 합니다. |
| `PAYMENT_003` | 409 | 결제할 수 없는 주문 상태입니다. |
| `PAYMENT_004` | 502 | 결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요. |
| `PAYMENT_005` | 400 | 결제 승인에 실패했습니다. |
| `PAYMENT_006` | 409 | 이미 처리된 결제입니다. |
| `PAYMENT_007` | 502 | 결제 취소 처리 중 오류가 발생했습니다. |
| `PAYMENT_008` | 400 | Idempotency-Key 헤더가 필요합니다. |
