# storyforge-ai/

StoryForge의 **FastAPI AI Service**입니다. 프롬프트 조립, LLM 스트리밍 생성, 요약, Continuity 검증을 맡습니다.
**내부망 전용**이며 Spring Boot만 호출합니다. 브라우저에서 직접 접근하지 않습니다.

---

## 실행 방법

### 준비물

| 항목 | 버전 / 조건 | 비고 |
|------|-------------|------|
| Python | 3.10 이상 | `python --version` |
| Anthropic API 키 | 선택 | `LLM_PROVIDER=anthropic` 또는 `claude-*` 선택 시 필요합니다 |

### 처음 한 번 — 가상환경과 의존성

```bash
cd storyforge-ai
python -m venv .venv
.venv/Scripts/python -m pip install -r requirements.txt       # macOS/Linux: .venv/bin/python
```

`.venv/`는 `.gitignore`에 들어 있습니다.

### 실행 — mock 모드 (기본, API 키 불필요)

```bash
.venv/Scripts/python -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

개발 중 코드 변경을 바로 반영하려면 `--reload`를 붙입니다.

mock 모드는 파이프라인 흐름(Bible → Outline → 챕터 스트리밍 → Continuity)을 확인하기 위한 것입니다. 생성물에는 "※ 개발용 mock 생성기 출력입니다" 표시가 붙습니다.

### 실행 — Ollama 일반 출력

`LLM_PROVIDER=ollama`, `OLLAMA_MODEL=gemma4:e4b`로 실행합니다. Claude 자격 증명은 Ollama 단독 사용에 필요하지 않습니다. 모델 상주·시작 예열·동시 추론 제한·스키마 출력 설정은 [성능 가이드](../docs/guidance/ollama-performance.md)를 보세요.

```sh
.venv/Scripts/python benchmark_ollama.py --runs 3 --warmup
```

### 실행 — Claude 사용

```bash
# bash
LLM_PROVIDER=anthropic ANTHROPIC_API_KEY=sk-ant-... \
  .venv/Scripts/python -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

```powershell
# PowerShell
$env:LLM_PROVIDER = "anthropic"
$env:ANTHROPIC_API_KEY = "sk-ant-..."
.venv\Scripts\python -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

저장소 루트나 이 폴더의 `.env`에 값을 적어 두어도 자동으로 읽습니다 (`.env`는 커밋하지 않습니다).

### 확인

| 확인 | 명령 / 주소 | 기대 결과 |
|------|-------------|-----------|
| 상태 | `curl http://127.0.0.1:8001/health` | `{"status":"ok","provider":"mock","model":"-"}` |
| API 문서 | http://127.0.0.1:8001/docs | FastAPI Swagger UI (내부 확인용) |

직접 호출해 보기 (보통은 백엔드가 호출합니다):

```bash
curl -X POST http://127.0.0.1:8001/internal/generate/bible \
  -H "Content-Type: application/json" \
  -d '{"project":{"title":"Test","genre":"fantasy"},"prompt":"a shop that trades memories"}'
```

### 명령어

| 하려는 일 | 명령 |
|-----------|------|
| 실행 | `.venv/Scripts/python -m uvicorn app.main:app --host 127.0.0.1 --port 8001` |
| 자동 재시작 실행 | 위 명령에 `--reload` |
| 테스트 의존성 설치 | `.venv/Scripts/python -m pip install -r requirements-dev.txt` |
| 테스트 | `.venv/Scripts/python -m pytest -q` (mock 모드, API 키 불필요) |
| 타입 검사 | `npx pyright --pythonpath .venv/Scripts/python.exe app tests` |

### 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `LLM_PROVIDER` | `mock` | `mock` / `ollama` / `anthropic` |
| `LLM_MODEL` | `claude-opus-5-5` | Claude 모델 ID |
| `LLM_EFFORT` | `high` | `low` ~ `max`. 깊이와 비용을 조절합니다 |
| `ANTHROPIC_API_KEY` | (없음) | 비우면 SDK 표준 경로(`ant auth login` 프로필 등)에서 찾습니다 |
| `MOCK_STREAM_DELAY` | `0.02` | mock 스트리밍 조각 사이 지연(초) |

호스트는 반드시 `127.0.0.1`로 묶습니다. `0.0.0.0`으로 열면 내부 전용 원칙이 깨집니다.

---

## 내부 API

| 요청 | 응답 |
|------|------|
| `POST /internal/generate/bible` | `{worldSetting, theme, tone, characters[]}` |
| `POST /internal/generate/outline` | `{chapters[]}` |
| `POST /internal/generate/chapter` | SSE: `token`(본문 조각) → `done`(`{title, content, summary, report}`) 또는 `error` |
| `POST /internal/generate/review` | `{summary, report}`. 사람이 고친 본문의 재요약·재검증 |
| `POST /internal/package/continuity` | `{issues[], score}`. 패키지 작품 사이의 충돌 검사 |
| `POST /internal/guide/chat` | `{reply, actions[]}`. 명령어는 LLM 없이 답함. 이동 버튼은 서버의 표에서만 만듦 |
| `GET /health` | `{status, provider, model}` |

JSON 필드는 camelCase이며, 백엔드 [AiDtos.java](../storyforge-backend/src/main/java/com/storyforge/client/AiDtos.java)와 [schemas.py](app/models/schemas.py)가 짝입니다. 한쪽을 바꾸면 다른 쪽도 같은 커밋에서 바꿉니다. 상세 형식은 [docs/plan/api-v1.md](../docs/plan/api-v1.md)에 있습니다.

## 챕터 생성 흐름

```
컨텍스트 수신 (Bible + Outline 해당 장 + 이전 챕터 요약 + 캐릭터 상태)
  → prompts/chapter.j2로 프롬프트 조립
  → LLM 스트리밍 (token 이벤트)
  → 요약 생성 (다음 챕터 컨텍스트용)
  → Continuity 검증: 규칙 기반 검사 + (Ollama·Claude 모드) LLM 검증 → 점수
  → done 이벤트
```

사람이 본문을 고치면 승인할 때 `review`가 같은 요약·검증을 그 본문으로 다시 합니다. mock에서는 요약이 본문 앞부분으로 만들어져, 내용이 바뀌면 요약도 바뀝니다.

Claude 모드에서는 서버 측 refusal fallback(`fallbacks: "default"`)을 켜 두었습니다. 모델이 거절하거나 출력이 잘리면 `error` 이벤트를 보내고, 백엔드는 프로젝트 상태를 되돌립니다.

## 폴더 구조

```
storyforge-ai/
├── requirements.txt        # 실행 의존성
├── requirements-dev.txt    # 테스트 의존성 (pytest, httpx)
├── app/
│   ├── main.py             # 앱, /health
│   ├── api/v1/             # /internal/generate/*, /internal/package/*, /internal/guide/*
│   ├── core/               # 설정, LLM 추상화
│   ├── services/           # 생성기, Continuity 검사, 패키지 검사, 가이드챗, mock
│   ├── prompts/            # Jinja2 템플릿
│   └── models/             # Pydantic 스키마
└── tests/                  # 52건. mock·가짜 LLM·Ollama HTTP 모의 전송, 인물 기록 격리
```

실행 중 쌓이는 `data/used_characters.json`과 `data/chroma/`는 Git에서 제외합니다. 이름 풀 `data/names.json`은 버전 관리합니다.

실제 모델 파이프라인은 `.venv/Scripts/python validate_pipeline.py --chapters 3`으로 확인합니다. 합성 작품의 Bible·Outline·챕터·요약·Continuity를 실행하며 인물 기록을 임시 폴더로 분리합니다. 원문이나 사용자 데이터를 출력하지 않고 시간·길이·검증 이슈만 출력합니다. 단계별 제한은 `--timeout`으로 정하며 기본 180초입니다. 이 도구는 API·DB·사용자 승인 검증을 대신하지 않습니다.

## 문제 해결

| 증상 | 원인 / 해결 |
|------|-------------|
| 한글 출력 시 `UnicodeEncodeError: 'cp949'` | Windows 콘솔 인코딩 문제입니다. `PYTHONIOENCODING=utf-8`을 설정합니다 |
| `Address already in use` (8001), 또는 `/health`가 HTML을 돌려줌 | 다른 서버가 8001을 씁니다. 다른 포트로 띄우고 백엔드 `AI_SERVICE_URL`도 맞춥니다 — [local-port-conflicts.md](../docs/repeater/local-port-conflicts.md) |
| Claude 모드에서 `error` 이벤트 | 키·네트워크를 확인합니다. 메시지가 "거절" 또는 "출력 한도"면 프롬프트·분량 문제입니다 |
| `ModuleNotFoundError: app` | `storyforge-ai` 폴더에서 실행해야 합니다 |

## 규칙

- 인증, 프로젝트 상태 관리, DB 저장은 하지 않습니다 (Spring Boot 담당).
- Continuity 실패를 임의로 통과시키지 않습니다.
- 실제 Claude를 호출하는 테스트는 비용이 들기 때문에 기본 테스트에 넣지 않습니다. 실제 Claude 호출은 미검증입니다. Ollama는 짧은 프롬프트 실측만 완료했으며 장편·배포 서버 성능은 추가 확인이 필요합니다.

## 관련 문서

- [기획서 §6, §7, §8.3](../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
- [docs/novel/](../docs/novel/) — 프롬프트 작성 규칙, Continuity 규칙
- [docs/plan/api-v1.md](../docs/plan/api-v1.md), [docs/plan/CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md)
- 저장소 안내: [../README.md](../README.md), 작업 규칙: [../AGENTS.md](../AGENTS.md)
- 보안: [../SECURITY.md](../SECURITY.md), 라이선스: [../LICENSE](../LICENSE)
