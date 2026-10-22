# StoryForge

사용자 프롬프트로부터 **일관된 장편 소설**을 만드는 AI 웹 플랫폼입니다.

```
Story Bible 생성/관리 → Outline 생성 → 순차 챕터 생성 + Continuity Tracking
```

단계마다 사용자 검토와 AI 자동 검증을 거칩니다. 자세한 배경은 [기획서](StoryForge_기획서_v1.2.md)를 보세요.

---

## 빠른 실행 (전체 스택)

### 1. 준비물

| 도구 | 버전 | 확인 명령 |
|------|------|-----------|
| Docker Desktop | 실행 중일 것 | `docker info` |
| Node.js | 24.14 이상 | `node -v` |
| JDK | 21 이상 | `java -version` |
| Python | 3.10 이상 | `python --version` |

Maven과 Angular CLI는 따로 설치하지 않아도 됩니다 (Maven Wrapper, `npx` 사용).

### 2. 실행 순서

터미널 4개를 열고 저장소 루트에서 순서대로 실행합니다. 각 서비스의 자세한 옵션은 아래 "서비스별 안내"의 README에 있습니다.

```bash
# ① DB — PostgreSQL 16 (호스트 포트 5433)
docker compose up -d

# ② AI 서비스 — http://127.0.0.1:8000 (처음 한 번만 venv 생성·설치)
cd storyforge-ai
python -m venv .venv
.venv/Scripts/python -m pip install -r requirements.txt
.venv/Scripts/python -m uvicorn app.main:app --host 127.0.0.1 --port 8000

# ③ 백엔드 — http://localhost:8080 (Flyway가 스키마를 자동 생성)
cd storyforge-backend
./mvnw spring-boot:run

# ④ 프론트엔드 — http://localhost:4200 (처음 한 번만 npm install)
cd storyforge-frontend
npm install
npx ng serve
```

> macOS/Linux에서는 `.venv/Scripts/python` 대신 `.venv/bin/python`을 씁니다.
> Windows PowerShell·cmd에서는 `./mvnw` 대신 `.\mvnw.cmd`를 씁니다.

### 3. 동작 확인

| 확인 | 명령 / 주소 | 기대 결과 |
|------|-------------|-----------|
| AI 서비스 | `curl http://127.0.0.1:8000/health` | `{"status":"ok","provider":"mock",...}` |
| 백엔드 | `curl http://localhost:8080/actuator/health` | `{"status":"UP"}` |
| API 문서 | http://localhost:8080/swagger-ui.html | Swagger UI |
| 웹 화면 | http://localhost:4200 | 다크 테마 대시보드 |

`health`가 JSON이 아니거나 포트 사용 중 오류가 나면 [local-port-conflicts.md](docs/repeater/local-port-conflicts.md)를 보세요.

화면에서 **새 프로젝트 → Story Bible 생성·확정 → Outline 생성·확정 → 챕터 생성**까지 진행하면, "AI writing..." 표시와 함께 본문이 흘러나오고 우측에 Continuity 점수가 나옵니다.

### 4. 실제 Claude로 생성하기

기본값은 API 키 없이 동작하는 **mock 생성기**입니다. 생성물에는 "※ 개발용 mock 생성기 출력입니다"라는 표시가 붙습니다. Claude를 쓰려면 ② 단계를 이렇게 실행합니다.

```bash
LLM_PROVIDER=anthropic ANTHROPIC_API_KEY=sk-ant-... \
  .venv/Scripts/python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

PowerShell에서는 `$env:LLM_PROVIDER="anthropic"; $env:ANTHROPIC_API_KEY="sk-ant-..."`를 먼저 실행합니다.

### 5. 종료와 초기화

| 하려는 일 | 명령 |
|-----------|------|
| 서비스 종료 | 각 터미널에서 `Ctrl+C` |
| DB 컨테이너 종료 (데이터 유지) | `docker compose down` |
| DB 데이터까지 삭제 | `docker compose down -v` |

---

## 서비스별 안내

| 서비스 | 기술 | 포트 | 실행 안내 |
|--------|------|------|-----------|
| 프론트엔드 | Angular 21 + Angular Material | 4200 | [storyforge-frontend/README.md](storyforge-frontend/README.md) |
| 백엔드 (Orchestrator) | Spring Boot 3.5 + PostgreSQL | 8080 | [storyforge-backend/README.md](storyforge-backend/README.md) |
| AI 서비스 | FastAPI + anthropic SDK | 8000 (내부 전용) | [storyforge-ai/README.md](storyforge-ai/README.md) |
| DB | PostgreSQL 16 (Docker) | 5433 | [docker-compose.yml](docker-compose.yml) |

```
브라우저 ──▶ Angular(4200) ──/api 프록시──▶ Spring Boot(8080) ──내부 HTTP──▶ FastAPI(8000) ──▶ LLM
                                              │
                                              ▼
                                       PostgreSQL(5433)
```

- **Angular는 FastAPI를 직접 호출하지 않습니다.** 모든 요청은 Spring Boot를 거칩니다.
- 버전·포트·환경 변수의 기준값은 [docs/plan/CURRENT_STACK.md](docs/plan/CURRENT_STACK.md)에 있습니다. 바꿀 때는 이 문서를 먼저 고칩니다.
- 환경 변수 목록은 [.env.example](.env.example)에 있습니다. 실제 값은 `.env`(커밋 금지)에 둡니다.

## 테스트

| 서비스 | 명령 (각 서비스 폴더에서) |
|--------|---------------------------|
| 백엔드 | `./mvnw test` |
| AI 서비스 | `.venv/Scripts/python -m pip install -r requirements-dev.txt` 후 `.venv/Scripts/python -m pytest -q` |
| 프론트엔드 | `npx ng build` (빌드·타입 검사) |

## 현재 구현 범위

| 상태 | 항목 |
|------|------|
| ✅ 동작 | 프로젝트 CRUD, Story Bible 생성·편집·확정, Outline 생성·재배치·확정, 챕터 순차 생성(스트리밍), Continuity 리포트, 사용자 승인, 다크/라이트 테마 |
| ⏳ 미구현 | 인증(JWT — 현재 API 개방), TipTap 에디터(현재 textarea), 내보내기, 버전 히스토리·Diff |
| ❓ 확인 대기 | 상태 머신 임시 전이 2개 — [pipeline-state-machine.md](docs/Architecture/pipeline-state-machine.md) "구현 임시안" |

## 저장소 구조

```
StoryForge/
├── StoryForge_기획서_v1.2.md   # 기획 원본 (이동·이름 변경 금지, .docx는 같은 내용의 배포본)
├── AGENTS.md                   # 작업 규칙 요약
├── .agents/                    # 규칙 정본 (skills/storyforge/SKILL.md)
├── .claude/ .cursor/ .codex/ .grok/ .antigravity/   # 도구별 규칙 어댑터
├── docs/                       # 스펙, 아키텍처, 실행 가이드, 커밋 기록
├── storyforge-frontend/        # Angular SPA
├── storyforge-backend/         # Spring Boot Orchestrator
├── storyforge-ai/              # FastAPI AI Service
├── docker-compose.yml          # 개발용 PostgreSQL
└── .env.example                # 환경 변수 키 목록
```

모든 폴더에는 그 폴더를 소개하는 `README.md`가 있습니다.

## 문서

| 문서 | 내용 |
|------|------|
| [StoryForge_기획서_v1.2.md](StoryForge_기획서_v1.2.md) | 기획 원본. 모든 결정의 최우선 기준 |
| [docs/README.md](docs/README.md) | 문서 진입점 (폴더별 용도) |
| [docs/guidance/local-dev-setup.md](docs/guidance/local-dev-setup.md) | 로컬 실행 가이드 (문제 해결 포함) |
| [docs/plan/api-v1.md](docs/plan/api-v1.md) | API, SSE 이벤트, Continuity 리포트 형식 |
| [docs/Architecture/](docs/Architecture/) | 시스템 구조, 상태 머신 |
| [AGENTS.md](AGENTS.md) / [.agents/skills/storyforge/SKILL.md](.agents/skills/storyforge/SKILL.md) | 작업 규칙 요약 / 정본 |

## 기여 규칙 요약

1. `develop`에서 `feature/*`, `bugfix/*`, `experimental/*` 브랜치를 만들어 작업합니다.
2. 커밋 메시지는 `type(scope): 한국어 요약` 형식입니다.
3. 커밋마다 [docs/branchs/commits/](docs/branchs/commits/)에 기록 파일을 남깁니다.
4. 병합은 `git merge --no-ff`로만 하고, 흐름은 `feature → develop → main`입니다. `main`이 기본 브랜치입니다.

세부 규칙은 [AGENTS.md](AGENTS.md)를 보세요.
