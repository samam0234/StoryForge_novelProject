# storyforge-backend/

StoryForge의 **Spring Boot 3.x Orchestrator**입니다. 외부에 노출되는 단일 API 표면이며, 파이프라인 상태 머신과 오케스트레이션을 맡습니다.
AI가 필요한 단계만 내부망의 FastAPI(`storyforge-ai`)에 위임합니다.

## 들어갈 것

- Spring Boot 3.x, Spring Security, Spring WebFlux (WebClient), Flyway, PostgreSQL (기획서 §12)
- 인증/인가 (JWT), 프로젝트/Bible/Outline/Chapter CRUD (§7.1)
- 파이프라인 상태 머신, 트랜잭션·영속화
- SSE로 챕터 생성 스트리밍 중계 (§7.2)
- OpenAPI 명세 생성 (§11)

## 들어가지 않을 것

- LLM 직접 호출, 프롬프트 조립 (FastAPI 담당)
- 포트 하드코딩 (값은 `docs/plan/CURRENT_STACK.md`)

## 하위 폴더

| 폴더 | 내용 |
|------|------|
| [src/](src/) | 소스 루트 |
| [.mvn/](.mvn/) | Maven Wrapper 설정 |

## 현재 상태

Spring Boot 3.5.16 + Maven Wrapper로 기초 API를 구현했습니다.

- 프로젝트·Bible·Outline·챕터 CRUD, 상태 머신(`service/orchestration/`), FastAPI 호출(`client/`), 챕터 SSE 중계
- Flyway `V1__init_schema.sql` (기획서 §10 엔티티)
- OpenAPI: http://localhost:8080/swagger-ui.html
- 실행 `./mvnw spring-boot:run`, 테스트 `./mvnw test`
- 아직 없음: JWT 인증(현재 `/api/v1/**` 개방, `SecurityConfig`의 TODO), Rate Limiting, Circuit Breaker

API 목록은 [docs/plan/api-v1.md](../docs/plan/api-v1.md), 실행 방법은 [docs/guidance/local-dev-setup.md](../docs/guidance/local-dev-setup.md)를 보세요.

## 관련 문서

- [기획서 §7, §8.2, §11](../StoryForge_기획서_v1.2.md)
- [docs/Architecture/](../docs/Architecture)
- [docs/plan/CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md)
- 저장소 안내: [../README.md](../README.md), 작업 규칙: [../AGENTS.md](../AGENTS.md)
