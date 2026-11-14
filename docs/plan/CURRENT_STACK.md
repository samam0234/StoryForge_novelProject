# CURRENT_STACK — 확정 스택과 인프라 값

> 기준: 기획서 v1.3 §7, §12, §13
> 포트와 인프라 확정값은 코드에서 정하지 않고 **이 문서에서** 관리합니다. (정본 "스택과 범위")
> 코드는 아래 값을 환경 변수 기본값으로만 씁니다. 값이 바뀌면 이 문서를 먼저 고칩니다.

## 1. 기술 스택

| 레이어 | 기획서 기준 | 확정 버전 | 비고 |
|--------|-------------|-----------|------|
| Frontend | Angular 18+, Angular Material, RxJS, TipTap | Angular 21.2, Angular Material 21.2, RxJS 7.8, TypeScript 5.9 | Standalone + signals, zoneless. Angular 22는 Node 24.15+가 필요해 보류. TipTap 3.32 (본문은 Markdown으로 저장) |
| Console | 독립 Angular 운영 사이트 | Angular 21.2, RxJS 7.8, TypeScript 5.9 | Standalone + signals. 사용자 웹과 별도 package-lock·테스트·빌드 |
| Node.js (개발) | — | 24.14 | |
| Main Backend | Spring Boot 4.x, Spring Security, WebClient, Flyway | Spring Boot 4.1.2 (Spring Framework 7, Jackson 3, Hibernate 7), springdoc-openapi 3.2.0 | 빌드 도구: Maven Wrapper (Maven 3.10.0) |
| Java | — | 소스/타깃 21, 개발 JDK 26 | 설치된 JDK가 26뿐이라 26으로 실행 |
| AI Service | FastAPI, Pydantic v2, httpx / OpenAI SDK | FastAPI 0.158.1, Pydantic 2, anthropic SDK 2.0.0, Jinja2 3 | 패키지 관리: venv + `requirements.txt` |
| Python | — | 3.10 | |
| Python 정적 검사 | — | Pyright 1.1.414 | 루트 `pyrightconfig.json`, AI venv의 Python 3.10 기준. AI·테스트·검증 스크립트 포함 |
| LLM | OpenAI, Anthropic 등 | 개발 기본 `mock`, 일반 출력 Ollama `gemma4:e4b`, 선택 Anthropic `claude-*` | 서버 측 refusal fallback(`fallbacks: "default"`) 사용 |
| Database | PostgreSQL 16+ | PostgreSQL 16 (Docker 이미지 `postgres:16`) | pgvector는 Phase 3 |
| 인프라 | Docker Compose (개발), 향후 Kubernetes | Docker Compose (DB만) | 웹·콘솔·백엔드·AI는 로컬 프로세스로 실행 |
| 안정성 | Circuit Breaker, Rate Limiting | Resilience4j 2.4.0, 자체 필터 | [stability.md](stability.md) |
| 내보내기 | Markdown, DOCX, PDF | commonmark 0.32, Apache POI 6.0, OpenPDF 3.0, 나눔고딕 내장. TXT, HTML, EPUB, ODT, RTF는 라이브러리 없이 생성 | [should-features.md](should-features.md), [community.md](community.md) |
| 관측성 | Spring Actuator, Prometheus, 구조화 로깅 | Actuator `health`, `info`만 노출 | Prometheus 미도입 |

## 2. 포트 (로컬 개발)

| 서비스 | 개발 포트 | 외부 노출 | 비고 |
|--------|-----------|-----------|------|
| storyforge-frontend | 4200 | 예 | `ng serve`. `/api`는 `proxy.conf.json`으로 8080에 전달 |
| storyforge-console | 4201 | 운영자 전용 | 독립 Angular 21.2 앱. `/api`는 같은 백엔드로 전달 |
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
| `CONSOLE_ORIGIN` | backend | `http://localhost:4201` | 운영 콘솔 CORS 허용 출처 |
| `JWT_SECRET` | backend | (없음) | 32바이트 이상. 비우면 시작할 때 무작위 생성(재시작하면 로그인 풀림). **운영에서는 필수** |
| `JWT_TTL` | backend | `12h` | 액세스 토큰 유효 기간 |
| `JWT_REFRESH_TTL` | backend | `14d` | 갱신 토큰 유효 기간. 비밀번호를 바꾸면 이전 토큰은 거부 |
| `METRICS_TOKEN` | backend | (없음) | 있으면 `X-Metrics-Token`으로 `/actuator/prometheus`. 없으면 404 |
| `MAIL_ENABLED` | backend | `false` | SMTP 계정 인증·비밀번호 재설정 |
| `MAIL_FROM` | backend | (없음) | 메일 활성화 시 필수 발신 주소 |
| `SMTP_HOST` / `SMTP_PORT` | backend | `localhost` / `1025` | 로컬 수신기 기준 |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | backend | (없음) | SMTP 자격 증명 |
| `SMTP_AUTH` / `SMTP_STARTTLS` | backend | `false` / `false` | 외부 공급자는 인증·TLS 설정 필요. STARTTLS true이면 필수 협상 |
| `RATE_LIMIT_AUTH_PER_MINUTE` | backend | `20` | IP당 로그인·가입 |
| `RATE_LIMIT_GENERATE_PER_MINUTE` | backend | `30` | 사용자당 AI 생성, 가이드챗 |
| `RATE_LIMIT_COMMUNITY_PER_MINUTE` | backend | `60` | 사용자당 글·댓글·공유·Clone, IP당 공유 방문 |
| `ADMIN_EMAILS` | backend | (없음) | 관리자 이메일. 쉼표로 구분. [moderation.md](moderation.md) |
| `TERMS_VERSION` | backend | `2026-11-04` | 이용약관 버전. 올리면 기존 사용자는 다시 동의해야 게시 가능 |
| `TRUSTED_PROXIES` | backend | (없음) | 이 주소에서 온 요청만 `X-Forwarded-For`를 신뢰. 비우면 접속 주소만 사용 |
| `CONTENT_BLOCKLIST` | backend | (없음) | 쉼표로 구분한 금지 표현. 비우면 검사하지 않음 |
| `COMMUNITY_ZONE` | backend | `Asia/Seoul` | 작업 그래프와 활동 잔디의 날짜 기준 |
| `CODICOMPERCHAT_URL` | backend | (없음) | 가이드챗 콜라보 이동 주소. 비우면 이동 버튼을 표시하지 않음 |
| `LLM_PROVIDER` | ai, backend | `mock` | 테스트만 `mock`. 일반 실행은 `ollama`(Gemma 4 E4B). 강력 출력은 프로젝트 모델이 `claude-*`일 때 Anthropic. 백엔드는 외부 전송 안내에 같은 값을 씀 |
| `LLM_MODEL` | ai | `claude-opus-5-5` | 강력 출력 기본 모델 |
| `OLLAMA_BASE_URL` | ai | `http://127.0.0.1:11434` | 일반 출력 |
| `OLLAMA_MODEL` | ai | `gemma4:e4b` | 일반 출력 모델 |
| `OLLAMA_KEEP_ALIVE` | ai | `15m` | 모델 상주 시간 |
| `OLLAMA_NUM_CTX` | ai | `32768` | 요청·예열에 명시하는 컨텍스트. 4096~131072. VRAM과 모델 한도 확인 |
| `OLLAMA_THINK` | ai | `false` | 선택적 모델 추론 출력. 활성화하면 첫 토큰 지연 증가 가능 |
| `OLLAMA_WARMUP` | ai | `false` | Ollama 모드에서 시작 시 모델 예열. 실패해도 앱 시작 |
| `OLLAMA_MAX_CONCURRENT` | ai | `1` | 프로세스별 동시 추론 수, 1~32 |
| `OLLAMA_QUEUE_TIMEOUT` | ai | `5` | 추론 슬롯 대기 제한(초) |
| `OLLAMA_TIMEOUT` | ai | `180` | Ollama HTTP 읽기 제한(초), 연결·풀 대기는 5초 |
| `OLLAMA_STRUCTURED_MAX_TOKENS` | ai | `8000` | JSON 생성 출력 상한. 잘리면 실패 |
| `OLLAMA_STREAM_MAX_TOKENS` | ai | `24000` | 챕터 스트림 출력 상한. 잘리면 실패 |
| `LLM_EFFORT` | ai | `high` | 소설 생성·이어쓰기 추론. Claude 선택 모델은 Haiku high, Sonnet·Opus medium |
| `ANTHROPIC_API_KEY` | ai | (없음) | 비우면 SDK 표준 자격 증명 경로 사용 |

## 4. 서비스 간 통신

| 경로 | 프로토콜 | 기획서 근거 |
|------|----------|-------------|
| Angular → Spring Boot | HTTP REST + SSE (`fetch` 스트림) + WebSocket `/api/v1/ws` | §7.1. 챕터 본문은 SSE. 가이드와 장면 개척만 웹소켓 |
| Spring Boot → FastAPI | 내부 HTTP (WebClient), 챕터는 SSE, 실시간 요청은 `/internal/ws` | §7.1, §7.3 |
| FastAPI → LLM Providers | Ollama HTTP 연결 풀 / Anthropic SDK 스트리밍 | §7.1 |
| Spring Boot → PostgreSQL | JDBC (JPA, Flyway) | §7.1 |

- 외부 API 버전 prefix: `/api/v1/` (§11). 상세는 [api-v1.md](api-v1.md)
- 내부 API: `POST /internal/generate/{bible|outline|chapter|review}` (§7.2), `/internal/package/continuity`, `/internal/guide/chat`
- 타임아웃: 백엔드 → AI 180초, 연결 5초. MVC 비동기 요청 180초 (§4 P95 < 90초 기준 여유)

## 운영 콘솔과 추론 정책 (2026-11-10)

운영 콘솔은 사용자 웹과 동일한 Spring Boot를 사용합니다. Ollama는 프로세스별 추론 슬롯, 대기 시간 제한, 모델 상주, JSON 스키마 검증을 적용합니다. 필수 컨텍스트와 Continuity 검증은 유지합니다. [구조](../Architecture/operations-console.md), [조정 가이드](../guidance/ollama-performance.md).

동일 작성자의 여러 대상을 일괄 정지·해제할 때 계정 변경·작성자 알림을 중복 실행하지 않습니다. 공백뿐인 스트림은 정상 생성으로 저장하지 않습니다. AI 실행 데이터(`storyforge-ai/data/used_characters.json`, `storyforge-ai/data/chroma/`)는 추적하지 않으며 이름 풀 `names.json`은 버전 관리합니다. [재현과 해결](../find_debug/2026-11-10.md).

이름 풀 225개는 `python -m scripts.embed_characters`로 Chroma 컬렉션 `characters`에 넣습니다. 벡터는 256차원 글자·음절 겹침(`storyforge-overlap`)이며 신경망 모델을 받지 않습니다. 새 인물 설명은 같은 컬렉션에 추가되고, 거리 0.35 미만이면 다음 Bible 프롬프트에서 피하도록 넘깁니다. 저장 위치는 `storyforge-ai/data/chroma/`입니다.

장면 연결 어색함은 `storyforge-ai/app/services/plausibility_weights.json`의 로지스틱 회귀로 계산합니다. 학습 데이터는 합성 문장이고 생성 모델을 받지 않습니다. 결과는 Continuity의 `plot_thread`에만 더합니다.

`prod`에서는 `FRONTEND_ORIGIN`과 `CONSOLE_ORIGIN`을 각각 경로·와일드카드·후행 슬래시 없는 HTTPS 출처 하나로 명시합니다. 두 출처를 정확하게 허용하며, 개발 DB 자격 증명·빈 JWT 키 거부와 API 문서 비활성 정책은 유지합니다.

## 변경 이력

Beat 3개 이상인 실모델 챕터는 두 구간으로 순차 생성합니다. 뒤 구간 목표 분량은 전체 목표 5,000자와 앞 구간 실제 길이로 계산합니다. 첫 토큰을 바로 전달하지만 전체 완료 시간은 생성 분량과 구간 수에 따라 증가할 수 있습니다. [실측 기록](../find_debug/2026-11-11.md).

2026-11-11: `SMTP_SSL=false`를 추가했습니다. Gmail 설정은 STARTTLS 587·인증 활성·SSL 비활성이며 로컬 비밀 설정은 루트 `.env`에 둡니다. Windows 로컬 실행은 `storyforge-backend/scripts/start-local.ps1`을 사용합니다. V14는 프로젝트 창작 요구를 저장합니다. AI 컨텍스트는 `form`, `creativeBrief`, `storyOutline`, 이전 장의 `endingExcerpt`를 포함하며 기존 입력은 새 필드 없이도 처리합니다. 실모델 후처리를 요약·Continuity 공동 구조화 호출로 통합했습니다.

AI 서버의 정식 실행은 `storyforge-ai` 폴더에서 `python -m uvicorn main:app`입니다. 루트 `main.py` → `app/orchestration.py` → API·모델 수명주기 순서로 조립하며 패키지별 `orchestration.py`에 등록·작업 순서·공통 컨텍스트를 모읍니다. `app.main:app`은 동일 서버의 호환 경로입니다. `python -m scripts <도구> <인자>` 통합 명령과 기존 개별 도구 명령을 함께 지원합니다. [책임 경계](../Architecture/system-overview.md#ai-서버-내부-실행-조정).

실모델 파이프라인 검증은 `storyforge-ai` 폴더에서 `.venv/Scripts/python -m scripts.validate_pipeline --chapters 3`으로 실행합니다. 단계별 제한은 기본 180초이며 원문 대신 길이·시간·Continuity 이슈를 출력합니다. 인물 사용 기록은 임시 경로로 격리합니다. Outline 응답의 챕터 수 불일치는 생성 오류로 처리합니다. 수동 도구는 각 서비스의 `scripts/`, 자동 회귀 테스트는 표준 테스트 경로에 둡니다.

SMTP 활성화 시 `ADMIN_EMAILS`에 지정된 계정도 이메일 인증을 완료해야 관리자 권한을 얻습니다. 비밀번호 변경·재설정은 모든 기존 JWT를 폐기합니다. V12가 이메일 인증·자격 증명 버전·계정 낙관적 잠금·해시 토큰을 저장합니다. 이전 브라우저 검증 전 병합 보류는 후속 사용자의 커밋·병합·푸시 직접 명령으로 변경됐으며, 실제 화면 검증은 미완료입니다.

챕터의 기존 프롬프트 목표는 공백 포함 4,000~6,000자입니다. 규칙 기반 Continuity는 4,000자 미만을 낮은 심각도 검토 이슈로 표시하며, 전체 이름 미검출은 다른 호칭 가능성을 함께 안내합니다. 목표 미달이 자동으로 거부되거나 승인되지는 않습니다.

챕터 스트림 수신을 중단하면 FastAPI SSE → 챕터 생성기 → 모델 스트림을 명시적으로 닫아 연결과 추론 슬롯을 즉시 반환합니다. 기존 비동기 생성기의 가비지 컬렉션 시점에 의존하던 정리를 수정했습니다.

백엔드는 `done` 없는 AI 스트림 종료를 오류로 처리해 부분 원고를 저장하지 않고 생성 전 상태로 복귀합니다. V13은 `chapter.version`을 추가하며 재검증 시작 버전과 결과 적용 버전이 다르면 409로 거부합니다. AI 처리 중 DB 트랜잭션을 유지하지 않습니다.

CI의 AI 작업은 `.venv` 생성 → 개발 의존성 설치 → 루트 Pyright 검사 → pytest 순서입니다. 로컬도 같은 venv를 사용합니다. 종료 가능한 스트림 타입과 명시적인 수집 결과를 사용하며 SQL COUNT는 필수 결과로 확인합니다. 예외 클래스는 명시적 직렬화 버전을 둡니다.

| 일자 | 변경 | 커밋 기록 |
|------|------|-----------|
| 2026-11-10 | 독립 콘솔·명시적 HTTPS 출처, Ollama 연결·동시성 설정, 동일 작성자 일괄 조치·공백 스트림 수정 | [디버깅 일지](../find_debug/2026-11-10.md) |
| 2026-10-22 | 초안 작성 (기획서 기준값만 기록, 확정값 미정) | — |
| 2026-10-22 | 첫 구현에 맞춰 버전·포트·환경 변수 확정 | `feature/web-foundation` 커밋 기록 참고 |
| 2026-11-02 | JWT 인증 환경 변수 추가 | [auth.md](auth.md) |
| 2026-11-02 | Spring Boot 4.1.2 전환, AI 기본 포트 8001 | [roadmap-progress.md](roadmap-progress.md) |
| 2026-11-03 | 커뮤니티 환경 변수, 내보내기 형식 추가 | [community.md](community.md) |
| 2026-11-04 | `ADMIN_EMAILS`, `TERMS_VERSION`, 내부 API 2개 추가 | [moderation.md](moderation.md) |
| 2026-11-05 | 운영 프로필 잠금, `TRUSTED_PROXIES`, `CONTENT_BLOCKLIST`, 외부 모델 안내 | 원격 develop과 병합 |
