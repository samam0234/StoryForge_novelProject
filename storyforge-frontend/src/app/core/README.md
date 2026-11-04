# storyforge-frontend/src/app/core/


앱 전체에서 한 번만 생성되는 **싱글톤 서비스, 인터셉터, 가드, 공통 타입**입니다. 특정 기능에서만 쓰는 서비스는 해당 `features/*`에 둡니다.

## 파일

| 위치 | 내용 |
|------|------|
| `api/storyforge-api.service.ts` | Spring Boot `/api/v1` REST 래퍼 + `errorMessage()`(ProblemDetail → 사용자 메시지). 화면은 `HttpClient`를 직접 쓰지 않습니다 |
| `api/community-api.service.ts` | 게시판, Clone, 공유, 패키지, 라운지, 프로필, 신고, 알림, 가이드챗 API. `saveBlob`, `safeFilename` |
| `api/chapter-stream.service.ts` | 챕터 생성 SSE를 `fetch` 스트림으로 읽어 `token`/`done`/`error` 이벤트로 변환. 토큰을 직접 붙입니다 |
| `auth/auth.service.ts` | 세션 보관(localStorage), 로그인·가입·로그아웃, 만료·401 처리 |
| `auth/auth.interceptor.ts` | `/api/v1` 요청에 Bearer 토큰 첨부, 401이면 로그인으로 |
| `auth/auth.guard.ts` | `authGuard`(로그인 필요), `guestGuard`(로그인 화면용), `adminGuard`(서버 최신 정보로 관리자 확인. 실제 권한 검사는 백엔드) |
| `models/models.ts` | 엔티티 타입, `ProjectStatus`·`PIPELINE_STAGES`(기획서 §7.2), 모델·내보내기 형식 목록 |
| `models/community.ts` | 커뮤니티 타입과 화면 레이블 (유형, 역할, 채널, 신고 사유 등) |

## 규칙

- **FastAPI 주소나 `/internal/*`를 호출하지 않습니다.** Angular는 Spring Boot만 호출합니다.
- LLM 호출, 상태 전이 판단을 넣지 않습니다. 상태 이름과 순서는 기획서 그대로입니다.
- 챕터 SSE는 `fetch`를 쓰므로 인터셉터를 거치지 않습니다. 인증 처리를 바꾸면 `chapter-stream.service.ts`도 확인합니다.
- `models.ts`의 `MODEL_OPTIONS`는 백엔드 `UpdateProjectRequest`가 허용하는 모델과 같게 유지합니다.

## 관련 문서

- [기획서 §7.1, §8.1, §11](../../../../StoryForge_기획서_v1.2.md)
- [docs/plan/api-v1.md](../../../../docs/plan/api-v1.md), [docs/plan/auth.md](../../../../docs/plan/auth.md)
