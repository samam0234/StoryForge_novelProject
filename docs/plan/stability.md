# 안정성 스펙

> 기준: 기획서 v1.3 §13 (Rate Limiting), §15 (Circuit Breaker, 타임아웃, 재시도, 에러 전파, 사용자별 한도)

## 구현

| 항목 | 동작 | 위치 |
|------|------|------|
| Rate Limiting | 로그인·가입 IP당 분당 20회, AI 생성 사용자당 분당 30회. 넘으면 **429** + `Retry-After` | `config/RateLimitFilter.java` |
| 타임아웃 | 연결 5초, 응답 180초 | `config/WebClientConfig.java` |
| 재시도 | 동기 호출(Bible, Outline)의 **연결 실패만** 1회 | `client/AiServiceClient.java` |
| Circuit Breaker | 최근 10회 중 50% 이상 실패(최소 5회)면 30초간 호출 차단, 그동안 바로 502 | `config/WebClientConfig.java` |
| 멈춘 상태 복구 | 서버 시작 시 `CHAPTER_GENERATING`인 프로젝트를 되돌림 (챕터가 있으면 `CHAPTER_REVIEW`, 없으면 `OUTLINE_CONFIRMED`) | `service/orchestration/PipelineRecovery.java` |
| 동시 요청 | 프로젝트 낙관적 락(`version`). 같은 챕터 동시 생성은 한 건만 진행, 나머지 **409** | `domain/project/Project.java`, V3 |

## 결정

| 항목 | 결정 | 이유 |
|------|------|------|
| Rate Limiting 저장소 | 서버 메모리 (1분 고정 창) | MVP는 서버 한 대. 여러 대로 늘리면 Redis 등 공유 저장소가 필요 |
| IP 기준 | `request.getRemoteAddr()` | 프록시 헤더는 위조할 수 있어 믿지 않음. 프록시 뒤에 배포하면 설정 필요 |
| 응답 오류 재시도 | 하지 않음 | 요청이 AI까지 갔다면 생성 비용이 이미 발생. 스트리밍은 재시도하면 본문이 중복됨 |
| Resilience4j | 코어 모듈만 사용 | Spring Boot 스타터 없이 코드에서 명시적으로 구성 |

## 제한

- 복구는 서버 한 대 기준입니다. 여러 대로 늘리면 다른 서버의 진행 중 생성까지 되돌리므로 재검토해야 합니다.
- 챕터 생성 중 AI가 보내는 `error` 이벤트는 정상 응답이라 Circuit Breaker 실패로 세지 않습니다. 연결·HTTP 오류만 셉니다.

## 환경 변수

| 변수 | 기본값 |
|------|--------|
| `RATE_LIMIT_AUTH_PER_MINUTE` | `20` |
| `RATE_LIMIT_GENERATE_PER_MINUTE` | `30` |
