# CURRENT_STACK — 확정 스택과 인프라 값

> 기준: 기획서 v1.3 §7, §12, §13
> 포트와 인프라 확정값은 코드에서 정하지 않고 **이 문서에서** 관리합니다. (정본 "스택과 범위")
> 코드는 아래 값을 환경 변수 기본값으로만 씁니다. 값이 바뀌면 이 문서를 먼저 고칩니다.

## 1. 기술 스택

| 레이어 | 기획서 기준 | 확정 버전 | 비고 |
|--------|-------------|-----------|------|
| Frontend | Angular 18+, Angular Material, RxJS, TipTap | Angular 21.2, Angular Material 21.2, RxJS 7.8, TypeScript 5.9 | Standalone + signals, zoneless. Angular 22는 Node 24.15+가 필요해 보류. TipTap 3.32 (본문은 Markdown으로 저장) |
| Node.js (개발) | — | 24.14 | |
| Main Backend | Spring Boot 4.x, Spring Security, WebClient, Flyway | Spring Boot 4.1.2 (Spring Framework 7, Jackson 3, Hibernate 7), springdoc-openapi 3.2.0 | 빌드 도구: Maven Wrapper (Maven 3.10.0) |
| Java | — | 소스/타깃 21, 개발 JDK 26 | 설치된 JDK가 26뿐이라 26으로 실행 |
| AI Service | FastAPI, Pydantic v2, httpx / OpenAI SDK | FastAPI 0.158.1, Pydantic 2, anthropic SDK 2.0.0, Jinja2 3 | 패키지 관리: venv + `requirements.txt` |
| Python | — | 3.10 | |
| LLM | OpenAI, Anthropic 등 | 기본 `mock`, 선택 `anthropic` (`claude-opus-5-5`) | 서버 측 refusal fallback(`fallbacks: "default"`) 사용 |
| Database | PostgreSQL 16+ | PostgreSQL 16 (Docker 이미지 `postgres:16`) | pgvector는 Phase 3 |
| 인프라 | Docker Compose (개발), 향후 Kubernetes | Docker Compose (DB만) | 서비스 3개는 로컬 프로세스로 실행 |
| 안정성 | Circuit Breaker, Rate Limiting | Resilience4j 2.4.0, 자체 필터 | [stability.md](stability.md) |
| 내보내기 | Markdown, DOCX, PDF | commonmark 0.32, Apache POI 6.0, OpenPDF 3.0, 나눔고딕 내장. TXT, HTML, EPUB, ODT, RTF는 라이브러리 없이 생성 | [should-features.md](should-features.md), [community.md](community.md) |
| 관측성 | Spring Actuator, Prometheus, 구조화 로깅 | Actuator `health`, `info`만 노출 | Prometheus 미도입 |

## 2. 포트 (로컬 개발)

| 서비스 | 개발 포트 | 외부 노출 | 비고 |
|--------|-----------|-----------|------|
| storyforge-frontend | 4200 | 예 | `ng serve`. `/api`는 `proxy.conf.json`으로 8080에 전달 |
| storyforge-backend | 8080 | 예 | 외부에 노출하는 단일 API 표면 |
| storyforge-ai | 8001 | **아니오** | `127.0.0.1`에만 바인딩. 내부망 전용. 8000은 같은 PC의 다른 프로젝트(Django)와 겹쳐 2026-11-02에 변경 |
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
| `AI_SERVICE_URL` | backend | `http://localhost:8001` | 백엔드만 사용. 프론트에 넣지 않음 |
| `FRONTEND_ORIGIN` | backend | `http://localhost:4200` | CORS 허용 출처 |
| `JWT_SECRET` | backend | (없음) | 32바이트 이상. 비우면 시작할 때 무작위 생성(재시작하면 로그인 풀림). **운영에서는 필수** |
| `JWT_TTL` | backend | `12h` | 액세스 토큰 유효 기간 |
| `RATE_LIMIT_AUTH_PER_MINUTE` | backend | `20` | IP당 로그인·가입 |
| `RATE_LIMIT_GENERATE_PER_MINUTE` | backend | `30` | 사용자당 AI 생성, 가이드챗 |
| `RATE_LIMIT_COMMUNITY_PER_MINUTE` | backend | `60` | 사용자당 글·댓글·공유·Clone, IP당 공유 방문 |
| `COMMUNITY_ZONE` | backend | `Asia/Seoul` | 작업 그래프와 활동 잔디의 날짜 기준 |
| `CODICOMPERCHAT_URL` | backend | (없음) | 가이드챗 콜라보 이동 주소. 비우면 이동 버튼을 표시하지 않음 |
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
| 2026-11-02 | JWT 인증 환경 변수 추가 | [auth.md](auth.md) |
| 2026-11-02 | Spring Boot 4.1.2 전환, AI 기본 포트 8001 | [roadmap-progress.md](roadmap-progress.md) |
