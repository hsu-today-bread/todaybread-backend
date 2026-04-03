# API 문서

> Swagger UI: http://localhost:8080/swagger-ui/index.html
>
> 이 문서는 전체 API 목록과 에러 코드를 정리한 참고용 문서입니다.
> 실시간 API 스펙은 Swagger UI에서 확인하세요.

개발용 테스트 데이터가 필요하면 먼저 아래 스크립트를 실행하세요:

```bash
./scripts/test-data.sh
```

샘플 계정:

- `demo-user@todaybread.local` / `todaybread123`
- `demo-boss-gangnam@todaybread.local` / `todaybread123`
- `demo-boss-seolleung@todaybread.local` / `todaybread123`

---

## API 목록

### 인증 (Auth)

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `POST` | `/api/auth/reissue` | 토큰 재발급 | X |
| `POST` | `/api/auth/logout` | 로그아웃 | O |

### 사용자 (User)

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `POST` | `/api/user/register` | 회원가입 | X |
| `POST` | `/api/user/login` | 로그인 | X |
| `GET` | `/api/user/exist/email?value=` | 이메일 중복 확인 | X |
| `GET` | `/api/user/exist/nickname?value=` | 닉네임 중복 확인 | X |
| `GET` | `/api/user/exist/phone?value=` | 전화번호 중복 확인 | X |
| `PATCH` | `/api/user/update-profile` | 프로필 수정 | O |
| `POST` | `/api/user/boss-approve` | 사장님 등록 | O |

### 계정 복구 (User Recovery)

> 아래 API는 인증 없이 접근 가능합니다. 현재 별도의 인증 토큰(OTP 등) 없이 동작하므로,
> 운영 배포 전 Rate Limiting 또는 일회용 토큰 검증 추가를 권장합니다.

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `GET` | `/api/user/find-email?phone=` | 이메일 찾기 (마스킹된 이메일 반환) | X |
| `GET` | `/api/user/verify-identity?phone=&email=` | 본인 확인 | X |
| `POST` | `/api/user/reset-password` | 비밀번호 재설정 (기존 세션 무효화 포함) | X |

### 빵 — 일반 유저 (Bread)

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `GET` | `/api/bread/nearby?lat=&lng=&radius=&sort=` | 근처 빵 목록 (위치 기반) | O |
| `GET` | `/api/bread/detail/{breadId}` | 빵 상세 조회 | O |
| `GET` | `/api/bread/{storeId}` | 가게별 메뉴 목록 | O |

### 빵 — 사장님 (Bread Boss)

| 메서드 | 경로 | 설명 | 인증 | 권한 |
|--------|------|------|------|------|
| `GET` | `/api/boss/bread` | 내 가게 메뉴 목록 | O | BOSS |
| `POST` | `/api/boss/bread` | 메뉴 등록 (multipart) | O | BOSS |
| `PUT` | `/api/boss/bread/{breadId}` | 메뉴 수정 (multipart) | O | BOSS |
| `PATCH` | `/api/boss/bread/{breadId}/stock` | 재고 변경 / 품절 처리 | O | BOSS |
| `DELETE` | `/api/boss/bread/{breadId}` | 메뉴 삭제 | O | BOSS |

### 매장 — 일반 유저 (Store)

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `GET` | `/api/store/nearby?lat=&lng=&radius=` | 근처 가게 목록 (위치 기반) | O |
| `GET` | `/api/store/{storeId}` | 가게 상세 조회 (정보 + 이미지 + 메뉴 + 판매 상태) | O |

### 매장 — 사장님 (Store Boss)

| 메서드 | 경로 | 설명 | 인증 | 권한 |
|--------|------|------|------|------|
| `GET` | `/api/boss/store/status` | 가게 등록 상태 조회 | O | BOSS |
| `GET` | `/api/boss/store` | 내 가게 정보 + 이미지 + 영업시간 조회 | O | BOSS |
| `POST` | `/api/boss/store` | 가게 등록 (정보 + 영업시간 7개 + 이미지, multipart) | O | BOSS |
| `PUT` | `/api/boss/store` | 가게 정보 + 영업시간 수정 | O | BOSS |
| `PUT` | `/api/boss/store/images` | 가게 이미지 일괄 교체 (multipart) | O | BOSS |

### 키워드 (Keyword)

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `POST` | `/api/keywords` | 키워드 등록 | O |
| `GET` | `/api/keywords` | 내 키워드 목록 조회 | O |
| `DELETE` | `/api/keywords/{userKeywordId}` | 키워드 삭제 | O |

### 단골 가게 (Favourite Store)

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `POST` | `/api/favourite-stores` | 단골 가게 토글 (추가/해제) | O |
| `GET` | `/api/favourite-stores` | 단골 가게 목록 조회 | O |

### 찜목록 (Wishlist)

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `GET` | `/api/wishlist` | 찜목록 통합 조회 (키워드 + 단골 가게) | O |

### 시스템 (System)

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `GET` | `/api/system/health` | 서버 상태 확인 | X |

---

## 인증 구조

- JWT (HMAC-SHA256) 기반 stateless 인증
- 역할 계층: `BOSS > USER`
- 인증 불필요 경로: 회원가입, 로그인, 이메일/닉네임/전화번호 중복확인, 토큰 재발급, 계정 복구, 헬스체크, Swagger, 이미지 정적 파일
- 그 외 모든 경로는 `Authorization: Bearer {accessToken}` 필요
- 사장님 전용 API (`/api/boss/**`)는 `@PreAuthorize("hasRole('BOSS')")` 적용

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

> 빵 이미지 관련 에러(파일 형식, 크기, 저장 실패)는 공통 에러 코드 `COMMON_006`, `COMMON_005`, `COMMON_007`을 사용합니다.

### 인증 (AUTH)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `AUTH_001` | 401 | Access 토큰이 만료되었습니다. |
| `AUTH_002` | 401 | 유효하지 않은 Access 토큰입니다. |
| `AUTH_003` | 401 | 유효하지 않은 Refresh 토큰입니다. |
