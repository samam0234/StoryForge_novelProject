# storyforge-frontend/src/app/core/auth/

로그인 상태와 JWT 처리를 맡습니다. Spring Boot가 발급한 토큰을 저장하고 요청에 붙입니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `auth.service.ts` | 세션 보관(localStorage), 로그인·가입·로그아웃, 401 처리 |
| `auth.interceptor.ts` | `/api/v1` 요청에 Bearer 토큰 첨부, 401이면 로그인으로 |
| `auth.guard.ts` | `authGuard`(로그인 필요), `guestGuard`(로그인 화면용) |

챕터 SSE는 `fetch`를 쓰므로 인터셉터 대신 `core/api/chapter-stream.service.ts`에서 토큰을 직접 붙입니다.

## 들어갈 것

- 인증 서비스 (로그인, 로그아웃, 토큰 보관)
- 토큰을 붙이는 HTTP 인터셉터
- 인증이 필요한 라우트 가드

## 들어가지 않을 것

- 토큰 발급·검증 로직 (Spring Security 담당)
- API Key 등 시크릿

## 관련 문서

- [기획서 §7.1 (인증/인가 JWT), §13 (보안)](../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
