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

## 현재 상태

폴더 구조와 README만 있습니다. 빌드 설정(Gradle/Maven), `StoryforgeApplication.java`, `application.yml`은 아직 만들지 않았습니다.
빌드 도구와 버전은 `docs/plan/CURRENT_STACK.md`에 먼저 확정합니다.

## 관련 문서

- [기획서 §7, §8.2, §11](../StoryForge_기획서_v1.2.md)
- [docs/Architecture/](../docs/Architecture)
- [docs/plan/CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md)
- 저장소 안내: [../README.md](../README.md), 작업 규칙: [../AGENTS.md](../AGENTS.md)
