# storyforge-backend/src/main/java/com/storyforge/service/auth/

가입, 로그인, JWT 발급, 현재 사용자 조회입니다. 기획서 §8.2에는 없고 인증 구현을 위해 추가했습니다 ([auth.md](../../../../../../../../../docs/plan/auth.md)).

## 현재 파일

| 파일 | 내용 |
|------|------|
| `AuthService.java` | 가입(이메일 정규화, BCrypt), 로그인, 내 정보 |
| `TokenService.java` | HS256 액세스 토큰 발급 |
| `CurrentUser.java` | 요청 스레드의 인증 사용자 ID. 비동기 스트림 안에서는 쓰지 않습니다 |

## 들어가지 않을 것

- 소유자 검사 → `config/OwnershipInterceptor.java`

## 관련 문서

- 상위 폴더: [../README.md](../README.md)
