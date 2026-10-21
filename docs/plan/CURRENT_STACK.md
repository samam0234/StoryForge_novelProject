# CURRENT_STACK — 확정 스택과 인프라 값

> 기준: 기획서 v1.2 §7, §12, §13
> 포트와 인프라 확정값은 코드에서 정하지 않고 **이 문서에서** 관리합니다. (정본 "스택과 범위")
> `미정` 항목은 결정되면 값을 채우고 아래 변경 이력에 남깁니다.

## 1. 기술 스택

| 레이어 | 기획서 기준 | 확정 버전 | 비고 |
|--------|-------------|-----------|------|
| Frontend | Angular 18+, Angular Material, RxJS, TipTap | 미정 | Standalone components 권장 |
| Main Backend | Spring Boot 3.x, Spring Security, Spring WebFlux (WebClient), Flyway | 미정 | 빌드 도구(Gradle/Maven) 미정 |
| Java | — | 미정 | |
| AI Service | FastAPI, Pydantic v2, httpx / OpenAI SDK | 미정 | LangChain 선택적 도입 |
| Python | — | 미정 | 패키지 관리 도구 미정 |
| Database | PostgreSQL 16+ | 미정 | pgvector 확장 검토 (Phase 3) |
| 인프라 | Docker Compose (개발), 향후 Kubernetes | 미정 | |
| 관측성 | Spring Actuator, Prometheus, 구조화 로깅 | 미정 | AI 비용 추적 포함 |

## 2. 포트

| 서비스 | 개발 포트 | 외부 노출 | 비고 |
|--------|-----------|-----------|------|
| storyforge-frontend | 미정 | 예 | 개발 서버 |
| storyforge-backend | 미정 | 예 | 외부에 노출하는 단일 API 표면 |
| storyforge-ai | 미정 | **아니오** | 내부망 전용 |
| PostgreSQL | 미정 | 아니오 | |

배포 환경별 포트 인벤토리는 [../web_management/](../web_management/)에서 관리합니다.

## 3. 환경 변수

| 변수 | 사용 서비스 | 설명 | 값 위치 |
|------|-------------|------|---------|
| (미정) | | | `.env` (커밋 금지), `.env.example`에 키만 기록 |

## 4. 서비스 간 통신

| 경로 | 프로토콜 | 기획서 근거 |
|------|----------|-------------|
| Angular → Spring Boot | HTTPS (REST + SSE 스트리밍) | §7.1 |
| Spring Boot → FastAPI | 내부 HTTP (WebClient) | §7.1, §7.3 |
| FastAPI → LLM Providers | 공급자 SDK / HTTP (스트리밍) | §7.1 |
| Spring Boot → PostgreSQL | JDBC | §7.1 |

- 외부 API 버전 prefix: `/api/v1/` (§11)
- 내부 API 예시: `POST /internal/generate/chapter` (§7.2)

## 변경 이력

| 일자 | 변경 | 커밋 기록 |
|------|------|-----------|
| 2026-10-22 | 초안 작성 (기획서 기준값만 기록, 확정값 미정) | — |
