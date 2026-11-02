# storyforge-frontend/src/app/features/auth/

로그인과 회원가입 화면입니다. 기획서 §8.1의 `features` 목록에는 없지만, `core/auth`는 싱글톤 서비스 전용이라 화면을 여기에 두었습니다 ([auth.md](../../../../../docs/plan/auth.md)).

## 현재 파일

| 파일 | 내용 |
|------|------|
| `login.page.ts` | 로그인·가입 전환 폼. 성공하면 `returnUrl`로 이동 |

## 들어가지 않을 것

- 토큰 보관, 인터셉터, 가드 → `core/auth/`

## 관련 문서

- 상위 폴더: [../README.md](../README.md)
