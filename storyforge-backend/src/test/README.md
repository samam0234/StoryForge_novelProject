# storyforge-backend/src/test/

백엔드 테스트 76건입니다. `./mvnw test`로 실행합니다. 패키지 구조는 `src/main/java`와 같습니다.

**통합 테스트는 Docker로 실제 PostgreSQL을 띄웁니다.** Docker가 꺼져 있으면 통합 테스트는 실패가 아니라 **건너뜀**으로 처리되므로, 결과의 "Skipped"를 확인하세요.

## 파일

| 위치 (`java/com/storyforge/`) | 내용 |
|-------------------------------|------|
| `support/IntegrationTestBase` | Testcontainers PostgreSQL 한 개를 공유, 실제 HTTP 호출·상태 준비 도우미. 요청 제한은 테스트에서 사실상 해제 |
| `support/StubAiServer` | FastAPI 내부 API를 흉내 내는 서버(실제 HTTP/SSE). 실패·지연·중단 모드 전환 |
| `controller/AuthIntegrationTest` | 가입·로그인, 이메일 대소문자, 401, 다른 사용자의 리소스 404 |
| `service/orchestration/PipelineIntegrationTest` | 전체 파이프라인, 컨텍스트 주입, 단계·검토 건너뛰기 차단, 재생성, 실패 시 상태 복귀 |
| `service/orchestration/StabilityIntegrationTest` | 멈춘 상태 복구, Circuit Breaker, 동시 생성 차단, 연결 실패 502 |
| `service/orchestration/PipelineStateMachineTest` | 전이 표 단위 테스트 |
| `service/project/ShouldFeaturesIntegrationTest` | 프로젝트 내보내기(MD·DOCX·PDF), 생성·수정 이력, Bible 버전, 프로젝트 설정 |
| `service/orchestration/ChapterReviewIntegrationTest` | 고친 원고의 재요약·재검증, 다음 챕터 컨텍스트 반영, 심각한 이슈와 AI 실패 시 승인 중단 |
| `service/community/CommunityIntegrationTest` | 게시, 게시판, 비공개, 작업 그래프, Clone 범위·정책, 네트워크, 원본 비교, 공유, 내보내기 8종, 패키지, 추천·달성, 댓글, 프로필, 계정, 함께 창작 |
| `service/community/PackageContinuityIntegrationTest` | 패키지 전체 검사, 비공개 설정 제외, 원본 가져오기, 확인 표시 |
| `service/moderation/ModerationIntegrationTest` | 신고, 숨김·복원, 정지, 삭제된 토큰, 차단, 약관, 관리자 판별, 알림 |
| `config/RateLimitFilterTest` | 요청 제한과 시간 창 초기화, 가이드챗·커뮤니티 쓰기 제한 |

## 규칙

- 실제 Claude API를 호출하는 테스트는 넣지 않습니다. AI는 `StubAiServer`로 대신합니다.
- 새 엔드포인트를 추가하면 소유자 검사(다른 사용자 404) 케이스도 함께 추가합니다.

## 관련 문서

- [docs/plan/stability.md](../../../docs/plan/stability.md), [docs/plan/auth.md](../../../docs/plan/auth.md)
