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
## 브랜치 컨벤션
### Git-hub Flow
- main: 메인 
- feature/000: 새 기능 추가 
- fix/000: 버그 관련
- chore/000: 빌드 및 설정 관련  
- refactor/000: 리팩토링
- docs/000: 문서 관련
### 브랜치 이름 규칙
{브랜치 타입}/{설명}
```markdown
feature/login
feature/register
docs/readme-update
```
### 커밋 메시지 규칙
- 커밋 메세지는 "[{브랜치 타입}] 작업 내용"
- 예시
  - [feature] 로그인 기능 구현 완료
  - [fix] 회원가입시 중복 체크 부분 완료
  - [docs] README.md 업데이트 완료
