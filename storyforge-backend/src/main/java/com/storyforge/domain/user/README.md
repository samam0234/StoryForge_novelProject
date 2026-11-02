# storyforge-backend/src/main/java/com/storyforge/domain/user/

사용자 엔티티와 인증 DTO입니다. 기획서 §8.2에는 없고 인증 구현을 위해 추가했습니다 ([auth.md](../../../../../../../../docs/plan/auth.md)).

## 현재 파일

| 파일 | 내용 |
|------|------|
| `AppUser.java` | 엔티티 (email, passwordHash, displayName) |
| `AppUserRepository.java` | 리포지토리 |
| `AuthDtos.java` | 가입·로그인 요청, 토큰 응답 record |

## 들어가지 않을 것

- 토큰 발급, 비밀번호 검증 → `service/auth/`

## 관련 문서

- 상위 폴더: [../README.md](../README.md)
