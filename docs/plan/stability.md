# 안정성 스펙

> 기준: 기획서 v1.3 §13 (Rate Limiting), §15 (Circuit Breaker, 타임아웃, 재시도, 에러 전파, 사용자별 한도)

## 구현

| 항목 | 동작 | 위치 |
|------|------|------|
| Rate Limiting | 로그인·가입 IP당 분당 20회. AI 호출(생성, 가이드챗, 패키지 검사) 사용자당 분당 30회. 글·댓글·공유·Clone·신고 사용자당 분당 60회, 공유 방문 IP당 분당 60회. 넘으면 **429** + `Retry-After`. 창은 PostgreSQL | `config/RateLimitFilter.java` |
| 재검증 실패 | 수정본 승인 중 AI가 실패하면 502, 승인하지 않음. 다시 누르면 됨 | `service/orchestration/PipelineOrchestrator.java` |
| 재검증 중 편집 | 검증을 요청한 챕터 버전과 현재 버전이 다르면 409. 새 본문에 이전 요약·리포트를 적용하지 않음 | `ChapterService`, V13 `chapter.version` |
| 완료 이벤트 누락 | AI가 `done` 없이 연결을 정상 종료해도 SSE `error`와 생성 전 상태 복귀. 부분 본문 미저장 | `PipelineOrchestrator` |
| 타임아웃 | 연결 5초, 응답 180초 | `config/WebClientConfig.java` |
| 재시도 | 동기 호출(Bible, Outline)의 **연결 실패만** 1회 | `client/AiServiceClient.java` |
| Circuit Breaker | 최근 10회 중 50% 이상 실패(최소 5회)면 30초간 호출 차단, 그동안 바로 502 | `config/WebClientConfig.java` |
| 멈춘 상태 복구 | `CHAPTER_GENERATING`이 210초보다 오래되면 시작 시와 1분마다 되돌림 (챕터가 있으면 `CHAPTER_REVIEW`, 없으면 `OUTLINE_CONFIRMED`) | `service/orchestration/PipelineRecovery.java` |
| 동시 요청 | 프로젝트 낙관적 락(`version`). 같은 챕터 동시 생성은 한 건만 진행, 나머지 **409** | `domain/project/Project.java`, V3 |

## 결정

| 항목 | 결정 | 이유 |
|------|------|------|
| Rate Limiting 저장소 | PostgreSQL `rate_limit_window` (1분 고정 창) | 서버 여러 대가 같은 한도를 공유. 단위 테스트만 메모리 창 |
| IP 기준 | 바로 앞 주소가 `TRUSTED_PROXIES`에 있을 때만 `X-Forwarded-For`를 오른쪽부터 해석. 아니면 `getRemoteAddr()` | 위조된 프록시 헤더를 그대로 믿지 않기 위해. 운영 프록시 주소를 넣으면 실제 클라이언트로 제한 |
| 응답 오류 재시도 | 하지 않음 | 요청이 AI까지 갔다면 생성 비용이 이미 발생. 스트리밍은 재시도하면 본문이 중복됨 |
| Resilience4j | 코어 모듈만 사용 | Spring Boot 스타터 없이 코드에서 명시적으로 구성 |

## 제한

- 복구는 `updated_at`이 210초보다 오래된 `CHAPTER_GENERATING`만 되돌리고, 1분마다 다시 확인합니다. 다른 서버에서 진행 중인 생성은 유지합니다.
- 챕터 생성 중 AI가 보내는 `error` 이벤트는 정상 응답이라 Circuit Breaker 실패로 세지 않습니다. 연결·HTTP 오류만 셉니다.

## 환경 변수

| 변수 | 기본값 |
|------|--------|
| `RATE_LIMIT_AUTH_PER_MINUTE` | `20` |
| `RATE_LIMIT_GENERATE_PER_MINUTE` | `30` |
| `RATE_LIMIT_COMMUNITY_PER_MINUTE` | `60` |

## 운영 조치와 추론 과부하 보호 (2026-11-10)

운영 조치는 공통 DB 잠금과 트랜잭션으로 직렬화합니다. 이미 처리된 대상은 409입니다. Ollama는 프로세스별 동시 추론과 슬롯 대기 시간을 제한하며 취소·오류에서 슬롯과 HTTP 스트림을 반환합니다. 내부 503은 기존 외부 AI 장애 계약에 따라 처리합니다. [운영 구조](../Architecture/operations-console.md), [Ollama 가이드](../guidance/ollama-performance.md).

일괄 정지·해제에서는 같은 요청 안에서 이미 바꾼 계정만 재사용해 동일 작성자의 여러 글을 처리할 수 있습니다. 후속 요청의 상태 충돌은 계속 거부하고 대상별 감사·신고 처리와 전체 롤백을 유지합니다. Ollama의 공백뿐인 응답은 완료 신호가 있어도 실패입니다. [재현과 해결](../find_debug/2026-11-10.md).
