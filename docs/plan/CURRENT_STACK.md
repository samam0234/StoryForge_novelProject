# CURRENT_STACK — 확정 스택과 인프라 값

> 기준: 기획서 v1.2 §7, §12, §13
> 포트와 인프라 확정값은 코드에서 정하지 않고 **이 문서에서** 관리합니다. (정본 "스택과 범위")
> 코드는 아래 값을 환경 변수 기본값으로만 씁니다. 값이 바뀌면 이 문서를 먼저 고칩니다.

## 1. 기술 스택

| 레이어 | 기획서 기준 | 확정 버전 | 비고 |
|--------|-------------|-----------|------|
| Frontend | Angular 18+, Angular Material, RxJS, TipTap | Angular 21.2, Angular Material 21.2, RxJS 7.8, TypeScript 5.9 | Standalone + signals, zoneless. Angular 22는 Node 24.15+가 필요해 보류. TipTap은 아직 미도입 |
| Node.js (개발) | — | 24.14 | |
| Main Backend | Spring Boot 3.x, Spring Security, WebFlux (WebClient), Flyway | Spring Boot 3.5.16, springdoc-openapi 2.8.17 | 빌드 도구: Maven Wrapper (Maven 3.10.0) |
| Java | — | 소스/타깃 21, 개발 JDK 26 | 설치된 JDK가 26뿐이라 26으로 실행. Spring Boot 3.5는 공식 지원 범위 밖이므로 JDK 21/25 설치 권장 |
| AI Service | FastAPI, Pydantic v2, httpx / OpenAI SDK | FastAPI 0.158.1, Pydantic 2, anthropic SDK 2.0.0, Jinja2 3 | 패키지 관리: venv + `requirements.txt` |
| Python | — | 3.10 | |
| LLM | OpenAI, Anthropic 등 | 기본 `mock`, 선택 `anthropic` (`claude-opus-5-5`) | 서버 측 refusal fallback(`fallbacks: "default"`) 사용 |
| Database | PostgreSQL 16+ | PostgreSQL 16 (Docker 이미지 `postgres:16`) | pgvector는 Phase 3 |
| 인프라 | Docker Compose (개발), 향후 Kubernetes | Docker Compose (DB만) | 서비스 3개는 로컬 프로세스로 실행 |
| 관측성 | Spring Actuator, Prometheus, 구조화 로깅 | Actuator `health`, `info`만 노출 | Prometheus 미도입 |

## 2. 포트 (로컬 개발)

| 서비스 | 개발 포트 | 외부 노출 | 비고 |
|--------|-----------|-----------|------|
| storyforge-frontend | 4200 | 예 | `ng serve`. `/api`는 `proxy.conf.json`으로 8080에 전달 |
| storyforge-backend | 8080 | 예 | 외부에 노출하는 단일 API 표면 |
| storyforge-ai | 8000 | **아니오** | `127.0.0.1`에만 바인딩. 내부망 전용 |
| PostgreSQL | 5433 → 컨테이너 5432 | 아니오 | 로컬에 이미 5432를 쓰는 PostgreSQL이 있어 5433 사용 |

배포 환경별 포트 인벤토리는 [../web_management/](../web_management/)에서 관리합니다.

## 3. 환경 변수

키 목록은 루트 [.env.example](../../.env.example)에 있습니다. 실제 값은 `.env`(커밋 금지)나 셸 환경 변수로 넣습니다.

| 변수 | 사용 서비스 | 기본값 | 설명 |
|------|-------------|--------|------|
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | compose, backend | `storyforge` | 개발용 기본값. 운영에서는 반드시 바꿈 |
| `DB_PORT` | compose | `5433` | 호스트 포트 |
| `DB_URL` | backend | `jdbc:postgresql://localhost:5433/storyforge` | |
| `BACKEND_PORT` | backend | `8080` | |
| `AI_SERVICE_URL` | backend | `http://localhost:8000` | 백엔드만 사용. 프론트에 넣지 않음 |
| `FRONTEND_ORIGIN` | backend | `http://localhost:4200` | CORS 허용 출처 |
| `LLM_PROVIDER` | ai | `mock` | `mock` / `anthropic` |
| `LLM_MODEL` | ai | `claude-opus-5-5` | |
| `LLM_EFFORT` | ai | `medium` | Claude Opus 5.5 기본값과 같게 명시 |
| `ANTHROPIC_API_KEY` | ai | (없음) | 비우면 SDK 표준 자격 증명 경로 사용 |

## 4. 서비스 간 통신

| 경로 | 프로토콜 | 기획서 근거 |
|------|----------|-------------|
| Angular → Spring Boot | HTTP REST + SSE (`fetch` 스트림) | §7.1 |
| Spring Boot → FastAPI | 내부 HTTP (WebClient), 챕터는 SSE | §7.1, §7.3 |
| FastAPI → LLM Providers | anthropic SDK 스트리밍 | §7.1 |
| Spring Boot → PostgreSQL | JDBC (JPA, Flyway) | §7.1 |

- 외부 API 버전 prefix: `/api/v1/` (§11). 상세는 [api-v1.md](api-v1.md)
- 내부 API: `POST /internal/generate/{bible|outline|chapter}` (§7.2)
- 타임아웃: 백엔드 → AI 180초, 연결 5초. MVC 비동기 요청 180초 (§4 P95 < 90초 기준 여유)

## 변경 이력

| 일자 | 변경 | 커밋 기록 |
|------|------|-----------|
| 2026-10-22 | 초안 작성 (기획서 기준값만 기록, 확정값 미정) | — |
| 2026-10-22 | 첫 구현에 맞춰 버전·포트·환경 변수 확정 | `feature/web-foundation` 커밋 기록 참고 |
