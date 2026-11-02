# storyforge-backend/src/test/java/com/storyforge/support/

통합 테스트 공통 기반입니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `IntegrationTestBase.java` | Testcontainers PostgreSQL, 실제 HTTP 호출 도우미, 상태 준비 도우미. Docker가 없으면 테스트를 건너뜁니다 |
| `StubAiServer.java` | FastAPI 내부 API를 흉내 내는 테스트 서버 (실제 HTTP/SSE). 실패 모드 전환 가능 |

## 관련 문서

- 상위 폴더: [../README.md](../README.md)
