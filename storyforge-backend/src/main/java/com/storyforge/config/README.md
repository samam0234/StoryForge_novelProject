# storyforge-backend/src/main/java/com/storyforge/config/

**Security, Web, OpenAPI, WebClient** 설정 클래스를 둡니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `SecurityConfig.java` | JWT 리소스 서버, CORS, 무상태 세션, 경로별 허용, 401 ProblemDetail |
| `JwtConfig.java`, `JwtProperties.java` | HS256 키, 인코더·디코더, BCrypt |
| `RateLimitFilter.java`, `RateLimitProperties.java` | 로그인·가입, AI 호출, 커뮤니티 쓰기 요청 제한 (429) |
| `RateLimitCounter.java`, `JdbcRateLimitCounter.java`, `MemoryRateLimitCounter.java` | 1분 고정 창. 운영은 PostgreSQL 창을 여러 서버가 공유하고, 메모리 창은 단위 테스트용 |
| `ClientAddresses.java` | 요청 제한에 쓰는 클라이언트 주소. 바로 앞 연결이 신뢰 프록시일 때만 `X-Forwarded-For`를 봄 |
| `MetricsTokenFilter.java` | `METRICS_TOKEN`이 있을 때만 헤더로 Prometheus 스크랩 허용 |
| `LiveSocketConfig.java`, `LiveSessionHandler.java` | `/api/v1/ws`를 로그인 세션으로 열고 가이드·장면 개척만 AI 웹소켓으로 넘김. 챕터 SSE 경로는 바꾸지 않음 |
| `WebSocketTokenFilter.java` | 웹소켓 업그레이드의 쿼리 `access_token`을 Bearer 헤더로 옮겨 기존 JWT 필터가 검증하게 함 |
| `ProductionChecks.java`, `ProductionGuard.java` | `prod`에서 개발 자격 증명·빈 JWT 키를 거절하고 웹·콘솔 각각의 정확한 HTTPS 출처를 검증 |
| `OwnershipInterceptor.java`, `WebMvcConfig.java`, `ProjectAccess.java` | 경로 변수 `{projectId}`, `{chapterId}` 검사. 주인과 수락한 공동 창작자만 통과 |
| `AccountGuardInterceptor.java` | 쓰기 요청마다 정지·삭제된 계정 차단 ([moderation.md](../../../../../../../docs/plan/moderation.md)) |
| `WebClientConfig.java` | FastAPI용 WebClient (연결 5초, 응답 타임아웃), Circuit Breaker |
| `OpenApiConfig.java` | OpenAPI 문서 정보 |

## 들어갈 것

- Spring Security (JWT 필터, 인가 규칙)
- CORS 설정 (§13)
- OpenAPI 설정 (§11)
- FastAPI용 WebClient 빈 (타임아웃 포함)
- Rate Limiting (§13)

## 들어가지 않을 것

- 포트·주소 하드코딩 — `application.yml`과 환경 변수로 주입

## 관련 문서

- [기획서 §8.2, §13](../../../../../../../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
