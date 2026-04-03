# JWT 인증 엔드포인트 작성 가이드

이 문서는 JWT 인증이 적용된 상태에서 새로운 API 엔드포인트를 작성할 때 참고하는 예시 문서입니다.

개발용 테스트 계정이 필요하면 먼저 `./scripts/test-data.sh`를 실행하세요.

---

## 기본 원리

- SecurityConfig에서 `anyRequest().authenticated()`로 설정되어 있음
- permitAll 목록에 없는 모든 엔드포인트는 자동으로 JWT 검증됨
- 프론트는 매 요청마다 헤더에 `Authorization: Bearer {accessToken}`을 넣어서 보냄
- 별도 검증 코드 불필요 — Spring Security가 자동 처리

---

## permitAll 경로 (인증 불필요)

```
/api/user/register
/api/user/login
/api/user/exist/**
/api/auth/reissue
/api/user/find-email
/api/user/verify-identity
/api/user/reset-password
/api/system/health
/swagger-ui/**
/v3/api-docs/**
/images/**
```

위 경로 외의 모든 요청은 JWT 인증 필수입니다.

---

## 패턴 1: 인증 필요 + Body 있음

유저 정보가 필요하고, 요청 데이터도 있는 경우.

```java
@PostMapping("/register")
public StoreResponse register(@RequestBody @Valid StoreRequest request,
                               @AuthenticationPrincipal Jwt jwt) {
    Long userId = Long.parseLong(jwt.getSubject());
    return storeService.register(userId, request);
}
```

프론트 (Flutter/Dio):
```dart
await dio.post('/api/store/register',
    data: {'name': '오늘의빵', 'address': '서울시 강남구'},
    options: Options(headers: {'Authorization': 'Bearer $accessToken'}),
);
```

---

## 패턴 2: 인증 필요 + Body 없음

유저 정보만 필요하고, 별도 데이터는 없는 경우.

```java
@PostMapping("/logout")
public LogoutResponse logout(@AuthenticationPrincipal Jwt jwt) {
    Long userId = Long.parseLong(jwt.getSubject());
    authService.logout(userId);
    return LogoutResponse.ok();
}
```

프론트:
```dart
await dio.post('/api/auth/logout',
    options: Options(headers: {'Authorization': 'Bearer $accessToken'}),
);
```

---

## 패턴 3: 인증 필요 + PathVariable

특정 리소스를 조회/수정할 때.

```java
@GetMapping("/{storeId}")
public StoreResponse getStore(@PathVariable Long storeId,
                               @AuthenticationPrincipal Jwt jwt) {
    Long userId = Long.parseLong(jwt.getSubject());
    return storeService.getStore(userId, storeId);
}
```

프론트:
```dart
await dio.get('/api/store/1',
    options: Options(headers: {'Authorization': 'Bearer $accessToken'}),
);
```

---

## 패턴 4: 인증 불필요 (permitAll)

로그인, 회원가입 등 인증 없이 접근 가능한 API.
`@AuthenticationPrincipal` 사용하지 않음.

```java
@PostMapping("/login")
public UserLoginResponse login(@RequestBody @Valid UserLoginRequest request) {
    return userService.login(request);
}
```

프론트:
```dart
final response = await dio.post('/api/user/login',
    data: {'email': 'demo-user@todaybread.local', 'password': 'todaybread123'},
);
// 응답에서 accessToken, refreshToken 저장
```

> 새로운 permitAll 경로를 추가하려면 `SecurityConfig.java`의 `requestMatchers()`에 추가해야 합니다.

---

## JWT에서 꺼낼 수 있는 정보

`@AuthenticationPrincipal Jwt jwt`로 받은 뒤:

| 메서드 | 반환값 | 설명 |
|--------|--------|------|
| `jwt.getSubject()` | `"1"` (String) | userId (Long으로 파싱 필요) |
| `jwt.getClaim("email")` | `"user@test.com"` | 유저 이메일 |
| `jwt.getClaim("role")` | `"USER"` 또는 `"BOSS"` | 유저 역할 |

---

## 자동 에러 응답

JWT 검증 실패 시 Spring Security가 자동으로 에러 응답을 보냅니다:

| 상황 | 에러 코드 | HTTP | 메시지 |
|------|-----------|------|--------|
| 토큰 없음 | AUTH_002 | 401 | 유효하지 않은 Access 토큰입니다. |
| 토큰 만료 | AUTH_001 | 401 | Access 토큰이 만료되었습니다. |
| 토큰 위조/형식 오류 | AUTH_002 | 401 | 유효하지 않은 Access 토큰입니다. |

프론트에서 401 받으면:
1. AUTH_001 (만료) → refreshToken으로 `/api/auth/reissue` 호출
2. reissue 성공 → 새 토큰 저장 후 원래 요청 재시도
3. reissue 실패 (refreshToken도 만료) → 로그인 화면으로 이동

---

## 토큰 재발급 흐름

```
프론트 요청 → 401 (AUTH_001 만료)
    ↓
POST /api/auth/reissue (body: { refreshToken: "..." })
    ↓
성공 → 새 accessToken + refreshToken 받음 → 원래 요청 재시도
실패 → 로그인 화면으로 이동
```
