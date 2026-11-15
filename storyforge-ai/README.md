# storyforge-ai

StoryForge의 **AI 서비스**입니다. 프롬프트를 조립하고, 언어 모델로 Story Bible·Outline·챕터를 생성하고, 생성한 본문을 요약하고 앞선 설정과 어긋나는지 검증합니다.

| | |
|---|---|
| **기술** | FastAPI, Pydantic 2, Jinja2, anthropic SDK, httpx, ChromaDB. Python 3.10 이상 |
| **주소** | `http://127.0.0.1:8001` — **내부 전용** |
| **호출하는 곳** | Spring Boot([storyforge-backend](../storyforge-backend/README.md))만. 브라우저는 접속하지 않습니다 |
| **언어 모델** | `mock`(기본, 키 불필요) · Ollama `gemma4:e4b` · Anthropic `claude-*` |
| **테스트** | pytest 77건 + Pyright. 실제 모델을 호출하지 않습니다 |

이 서비스는 **아무것도 확정하지 않습니다.** 인증도, DB도, 프로젝트 상태도 없습니다. 받은 컨텍스트로 결과를 만들어 돌려줄 뿐이고, 저장과 승인은 Spring Boot와 사용자가 합니다. 인증이 없으므로 **절대 외부에 열지 않습니다.**

## 목차

- [실행](#실행)
- [언어 모델 공급자](#언어-모델-공급자)
- [환경 변수](#환경-변수)
- [내부 API](#내부-api)
- [챕터가 만들어지는 과정](#챕터가-만들어지는-과정)
- [Continuity 검사](#continuity-검사)
- [그 밖의 기능](#그-밖의-기능)
- [구조](#구조)
- [테스트](#테스트)
- [보조 도구](#보조-도구)
- [배포 이미지](#배포-이미지)
- [문제 해결](#문제-해결)
- [규칙](#규칙)
- [검증된 것과 한계](#검증된-것과-한계)

---

## 실행

### 준비물

| 항목 | 버전 / 조건 | 비고 |
|------|-------------|------|
| Python | 3.10 이상 | `python --version` |
| Ollama | 선택 | `LLM_PROVIDER=ollama`일 때. `gemma4:e4b` 모델 |
| Anthropic API 키 | 선택 | `LLM_PROVIDER=anthropic`이거나 프로젝트가 `claude-*`를 고를 때 |

### 처음 한 번

```bash
cd storyforge-ai
python -m venv .venv
.venv/Scripts/python -m pip install -r requirements.txt       # macOS/Linux: .venv/bin/python
```

### 서버

```bash
.venv/Scripts/python -m uvicorn main:app --host 127.0.0.1 --port 8001
```

- 기본은 **mock 모드**입니다. API 키 없이 파이프라인 흐름(Bible → Outline → 챕터 스트리밍 → Continuity)을 확인할 수 있습니다. 생성물에는 "※ 개발용 mock 생성기 출력입니다" 표시가 붙습니다.
- 코드 변경을 바로 반영하려면 `--reload`를 붙입니다.
- **호스트는 반드시 `127.0.0.1`로 묶습니다.** `0.0.0.0`으로 열면 내부 전용 원칙이 깨집니다.
- 반드시 `storyforge-ai` 폴더에서 실행합니다. 정식 진입점은 `main:app`이고, 이전 명령 `app.main:app`도 같은 서버를 가리킵니다.
- 저장소 루트나 이 폴더의 `.env`를 자동으로 읽습니다 (`.env`는 커밋하지 않습니다).

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
| 실행 | `.venv/Scripts/python -m uvicorn main:app --host 127.0.0.1 --port 8001` |
| 테스트 의존성 설치 | `.venv/Scripts/python -m pip install -r requirements-dev.txt` |
| 테스트 | `.venv/Scripts/python -m pytest -q` |
| 타입 검사 | `.venv/Scripts/pyright --project ..` (루트 `pyrightconfig.json` 사용) |

---

## 언어 모델 공급자

`LLM_PROVIDER`로 고릅니다.

| 값 | 동작 | 원고의 행방 |
|----|------|-------------|
| `mock` (기본) | 결정적 가짜 생성기. 테스트와 개발용 | PC 안 |
| `ollama` | 일반 출력은 로컬 `gemma4:e4b`. 프로젝트가 `claude-*` 모델을 지정하면 그 요청만 Claude로 보냅니다 | 기본은 PC 안 |
| `anthropic` | 모델을 지정하지 않은 요청도 Claude(기본 `claude-opus-5-5`)로 보냅니다 | **외부 제공자에게 전송** |

```bash
# Ollama
LLM_PROVIDER=ollama .venv/Scripts/python -m uvicorn main:app --host 127.0.0.1 --port 8001

# Claude
LLM_PROVIDER=anthropic ANTHROPIC_API_KEY=sk-ant-... \
  .venv/Scripts/python -m uvicorn main:app --host 127.0.0.1 --port 8001
```

```powershell
# PowerShell
$env:LLM_PROVIDER = "anthropic"
$env:ANTHROPIC_API_KEY = "sk-ant-..."
.venv\Scripts\python -m uvicorn main:app --host 127.0.0.1 --port 8001
```

**Ollama 모드**는 HTTP 연결 풀을 재사용하고, 모델을 메모리에 상주시키며, 동시 추론 수를 제한합니다. 추론 슬롯을 제한 시간(기본 5초) 안에 얻지 못하면 무한정 기다리지 않고 503과 `Retry-After`를 돌려줍니다. 조정 방법과 실측은 [ollama-performance.md](../docs/guidance/ollama-performance.md)에 있습니다.

**Claude 모드**는 서버 측 refusal fallback(`fallbacks: "default"`)을 켜 둡니다. 모델이 거절하거나 출력이 잘리면 정상 결과로 넘기지 않고 `error`를 보냅니다.

## 환경 변수

기준값과 설명 전체는 [CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md)가 원본입니다.

### 공통

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `LLM_PROVIDER` | `mock` | `mock` / `ollama` / `anthropic` |
| `LLM_MODEL` | `claude-opus-5-5` | 강력 출력 기본 모델 |
| `LLM_EFFORT` | `high` | `low` ~ `max`. 생성의 깊이와 비용 |
| `ANTHROPIC_API_KEY` | (없음) | 비우면 SDK 표준 자격 증명 경로에서 찾습니다 |
| `MOCK_STREAM_DELAY` | `0.02` | mock 스트리밍 조각 사이 지연(초) |

### Ollama

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `OLLAMA_BASE_URL` | `http://127.0.0.1:11434` | Ollama 주소 |
| `OLLAMA_MODEL` | `gemma4:e4b` | 일반 출력 모델 |
| `OLLAMA_KEEP_ALIVE` | `15m` | 모델 상주 시간 |
| `OLLAMA_NUM_CTX` | `32768` | 컨텍스트 크기 (4096~131072). 요청과 예열이 같은 값을 씁니다 |
| `OLLAMA_THINK` | `false` | 모델 추론 출력. 켜면 첫 토큰이 늦어질 수 있습니다 |
| `OLLAMA_WARMUP` | `false` | 시작할 때 모델 예열. 실패해도 서버는 뜹니다 |
| `OLLAMA_MAX_CONCURRENT` | `1` | 프로세스당 동시 추론 수 (1~32) |
| `OLLAMA_QUEUE_TIMEOUT` | `5` | 추론 슬롯 대기 제한(초) |
| `OLLAMA_TIMEOUT` | `180` | HTTP 읽기 제한(초) |
| `OLLAMA_STRUCTURED_MAX_TOKENS` | `8000` | JSON 생성 출력 상한. 잘리면 실패 |
| `OLLAMA_STREAM_MAX_TOKENS` | `24000` | 챕터 스트림 출력 상한. 잘리면 실패 |

---

## 내부 API

| 요청 | 응답 |
|------|------|
| `POST /internal/generate/bible` | `{worldSetting, theme, tone, characters[]}` |
| `POST /internal/generate/outline` | `{chapters[]}`. 요청한 챕터 수와 다르면 오류 |
| `POST /internal/generate/chapter` | SSE: `token`(본문 조각) → `done`(`{title, content, summary, report}`) 또는 `error` |
| `POST /internal/generate/review` | `{summary, report}`. 사람이 고친 본문의 재요약·재검증 |
| `POST /internal/package/continuity` | `{issues[], score}`. 패키지에 묶인 작품 사이의 충돌 |
| `POST /internal/guide/chat` | `{reply, actions[]}`. AI 가이드챗 |
| `WS /internal/ws` | 연결이 열린 동안 `ping`, `guide`, `scout`를 여러 번 받음 |
| `GET /health` | `{status, provider, model}` |

- JSON 필드는 camelCase입니다. 백엔드의 [AiDtos.java](../storyforge-backend/src/main/java/com/storyforge/client/AiDtos.java)와 [schemas.py](app/models/schemas.py)가 짝이므로, 한쪽을 바꾸면 **같은 커밋에서** 다른 쪽도 바꿉니다.
- 모델 오류는 502, 추론 슬롯 대기 초과는 503입니다.
- 상세 형식은 [api-v1.md](../docs/plan/api-v1.md)의 "내부 API"에 있습니다.

## 챕터가 만들어지는 과정

```
컨텍스트 수신
  Story Bible · 해당 장의 Outline · 승인된 이전 챕터 요약 · 캐릭터 상태
  + 창작 요구 · 작품 형식 · 전체 Outline · 직전 장의 마지막 장면
        │
        ▼
프롬프트 조립 (Jinja2 템플릿)
        │
        ▼
본문 스트리밍 ── token 이벤트를 받는 즉시 전달
  · 사건(Beat)이 3개 이상이면 두 구간으로 나눠 씀
  · 뒤 구간은 앞 구간의 실제 본문 전체를 이어받음
  · 같은 문장이 되풀이되면 스트리밍 중에 감지해 오류로 종료
        │
        ▼
요약 + Continuity 분석 (구조화 호출 한 번) + 규칙 검사 → 점수
        │
        ▼
done 이벤트 {title, content, summary, report}
```

- **모든 단계가 성공해야 `done`을 보냅니다.** 본문만 나오고 요약이나 검증이 실패하면 `error`입니다.
- 수신 쪽이 연결을 끊으면 SSE → 챕터 생성기 → 모델 스트림을 차례로 닫아 추론 슬롯을 즉시 돌려줍니다.
- 공백뿐인 스트림은 정상 생성으로 취급하지 않습니다.
- 목표 분량은 공백 포함 4,000~6,000자입니다. 미달이어도 강제로 이어 쓰거나 자동 거부하지 않고, 검토 화면에 낮은 심각도 이슈로 알립니다.

사람이 본문을 고치면 승인할 때 `review`가 같은 요약과 검증을 그 본문으로 다시 합니다.

## Continuity 검사

두 가지를 합칩니다.

| 검사 | 방식 | 언제 |
|------|------|------|
| **규칙 검사** | 코드로 확인. Outline에 나오기로 한 인물이 본문에 있는지, 분량이 목표에 닿는지, 직전 장면과 첫머리가 이어지는지 | 항상 (mock에서도) |
| **모델 검증** | 언어 모델이 Bible·Outline·이전 요약과 본문을 대조 | 실제 모델일 때 |

- 이슈 유형은 `setting_conflict`, `character_consistency`, `timeline`, `plot_thread` 네 가지입니다.
- 점수는 100에서 시작해 `high` 30, `medium` 12, `low` 4씩 깎습니다.
- 장면 연결은 로지스틱 회귀(`plausibility_weights.json`)로 계산해 `plot_thread`에만 더합니다. 학습 데이터는 합성 문장입니다.
- 인물의 전체 이름이 본문에 없으면 "없다"고 단정하지 않고 다른 호칭을 확인하라고 알립니다.

**어떤 결과도 챕터를 자동으로 승인하거나 거부하지 않습니다.** 리포트는 사람이 판단하도록 보여 주는 자료입니다. 기준은 [continuity-rules.md](../docs/novel/continuity-rules.md)에 있습니다.

## 그 밖의 기능

| 기능 | 내용 | 구현 |
|------|------|------|
| **장면 개척** | 직전 문장에서 남길 사실(`carry`)과 이어 쓸 첫 문단(`opening`)을 제안. 저장·승인 없음 | `services/story_scout.py` |
| **AI 가이드챗** | 명령어는 모델 없이 답하고, 자유 질문만 모델에 보냅니다. **이동 버튼은 서버의 표에서만** 만들어 모델 응답이 임의의 링크를 만들지 못합니다 | `services/guide_assistant.py` |
| **패키지 검사** | 묶인 작품 사이의 인물·세계 규칙·사건 충돌. 다른 작가가 공개하지 않은 설정은 받지 않습니다 | `services/package_continuity.py` |
| **인물 이름 관리** | 이름 풀 225개. 쓴 인물을 기록하고, ChromaDB의 글자 겹침 벡터로 비슷한 인물을 피하게 합니다. 벡터가 없어도 이름 중복 방지는 동작합니다 | `services/character_bank.py` |

## 구조

```
storyforge-ai/
├── main.py                 # 정식 ASGI 진입점 (uvicorn main:app)
├── requirements.txt        # 실행 의존성
├── requirements-dev.txt    # 개발 의존성 (pytest, pyright)
├── Dockerfile              # 배포 이미지
├── app/
│   ├── orchestration.py    # 앱 팩토리, /health, API·모델 수명주기 조립
│   ├── main.py             # 이전 실행 경로 호환
│   ├── api/v1/             # 라우터 (/internal/generate, /package, /guide, /ws)
│   ├── core/               # 설정, LLM 인터페이스, Ollama 클라이언트, 공급자 라우팅
│   ├── services/           # 생성기, 검증, 장면 개척, 가이드챗, mock
│   ├── prompts/            # Jinja2 템플릿
│   └── models/             # Pydantic 스키마, 컨텍스트
├── scripts/                # 수동 성능 측정·검증 도구
├── tests/                  # 자동 회귀 테스트
└── data/                   # 이름 풀(names.json). 실행 데이터는 Git 제외
```

호출은 한 방향으로만 흐릅니다.

```
main.py → app/orchestration.create_app → api/orchestration → api/v1/*
api/v1 → services/orchestration (AiTaskOrchestrator) → 개별 서비스 → core/llm
```

각 패키지의 `orchestration.py`가 그 계층의 등록과 실행 순서를 모읍니다. 모든 AI 작업은 `AiTaskOrchestrator` 하나를 거치고, 개별 서비스는 상위 계층을 거꾸로 참조하지 않습니다.

| 폴더 | 안내 |
|------|------|
| `app/` | [README](app/README.md) — 계층별 파일과 의존 방향 |
| `app/services/` | [README](app/services/README.md) — 서비스별 역할 |
| `app/prompts/` | [README](app/prompts/README.md) — 템플릿 목록 |
| `scripts/` | [README](scripts/README.md) — 도구 실행법 |
| `tests/` | [README](tests/README.md) — 테스트 범위 |

`data/used_characters.json`과 `data/chroma/`는 실행 중에 쌓이는 데이터라 Git에서 제외합니다. 이름 풀 `data/names.json`만 버전 관리합니다.

## 테스트

```bash
.venv/Scripts/python -m pip install -r requirements-dev.txt
.venv/Scripts/python -m pytest -q          # 77건
.venv/Scripts/pyright --project ..         # 오류 0, 경고 0
```

- 모두 mock과 가짜 LLM으로 실행합니다. **실제 모델을 호출하지 않으므로** 키와 비용이 필요 없습니다.
- 다루는 것: 내부 API 계약, SSE 이벤트 순서, 필수 컨텍스트 주입, 두 구간 생성, 반복 감지, 스트림 중단 시 자원 해제, Ollama 연결 재사용과 동시성 제한, Continuity 규칙과 점수, 가이드챗, 패키지 검사.
- 인물 저장소는 테스트마다 임시 경로로 격리합니다.
- 실제 Claude를 호출하는 테스트는 비용이 들므로 넣지 않습니다. 추가하려면 별도 표시와 사용자 승인이 필요합니다.

## 보조 도구

수동 측정·검증 도구는 [scripts/](scripts/README.md)에 있고 이 폴더에서 모듈로 실행합니다. `--help`는 모델을 호출하지 않습니다.

| 용도 | 명령 |
|------|------|
| 짧은 프롬프트 지연 측정 | `python -m scripts.benchmark_ollama --runs 3 --warmup` |
| 합성 작품의 다중 챕터 생성·요약·검증 | `python -m scripts.validate_pipeline --chapters 3` |
| 슬롯 포화, 중단, 긴 컨텍스트 회복 | `python -m scripts.validate_load` |
| 장시간 반복 생성과 중단 후 재사용 | `python -m scripts.validate_soak --duration 2400 --timeout 180` |
| 이름 풀을 ChromaDB에 넣기 | `python -m scripts.embed_characters` |
| 장면 연결 가중치 다시 학습 | `python -m scripts.train_plausibility` |

이 도구들은 원문이나 사용자 데이터를 출력하지 않고 시간·길이·검증 이슈만 냅니다. 운영 DB를 쓰지 않으며, **API·DB 저장·사용자 승인 검증을 대신하지 않습니다.**

## 배포 이미지

[Dockerfile](Dockerfile)은 Python 3.12 이미지에서 서버를 띄웁니다. 컨테이너 안에서는 `0.0.0.0:8001`로 열지만 **호스트에는 포트를 게시하지 않습니다.** 같은 Compose 네트워크의 백엔드만 접근합니다. 전체 구성은 루트의 [docker-compose.deploy.yml](../docker-compose.deploy.yml)에 있습니다.

## 문제 해결

| 증상 | 원인 / 해결 |
|------|-------------|
| `ModuleNotFoundError: app` | `storyforge-ai` 폴더에서 실행해야 합니다 |
| `Address already in use` (8001), 또는 `/health`가 HTML을 돌려줌 | 다른 서버가 8001을 씁니다. 다른 포트로 띄우고 백엔드 `AI_SERVICE_URL`도 맞춥니다 — [local-port-conflicts.md](../docs/repeater/local-port-conflicts.md) |
| 한글 출력 시 `UnicodeEncodeError: 'cp949'` | Windows 콘솔 인코딩 문제입니다. `PYTHONIOENCODING=utf-8`을 설정합니다 |
| 503과 `Retry-After` | 추론 슬롯이 꽉 찼습니다. 잠시 뒤 다시 요청하거나 `OLLAMA_MAX_CONCURRENT`를 조정합니다 |
| 첫 요청이 유난히 느림 | 모델을 메모리에 올리는 시간입니다. `OLLAMA_WARMUP=true`와 `OLLAMA_KEEP_ALIVE`를 봅니다 |
| Claude 모드에서 `error` 이벤트 | 키와 네트워크를 확인합니다. 메시지가 "거절"이나 "출력 한도"면 프롬프트·분량 문제입니다 |
| 생성이 반복 오류로 끝남 | 같은 문장이 되풀이돼 중단한 것입니다. 다시 생성합니다 |

## 규칙

- 인증, 프로젝트 상태 관리, DB 저장을 하지 않습니다 (Spring Boot 담당).
- Continuity 실패를 임의로 통과시키지 않습니다.
- 성능을 맞추려고 필수 컨텍스트(Bible, Outline, 이전 요약)를 잘라 내지 않습니다.
- 긴 프롬프트를 코드에 넣지 않고 `prompts/`의 템플릿으로 둡니다.
- API 키를 코드에 넣지 않습니다.
- 외부에 노출하지 않습니다.

## 검증된 것과 한계

| 항목 | 상태 |
|------|------|
| mock 파이프라인 전체 | 자동 테스트로 확인 |
| Ollama 합성 3장 생성·요약·검증 | 실제 모델로 확인 |
| 이전 요약 40개를 넣은 긴 입력 | 실제 모델로 확인 |
| 슬롯 포화 시 거부, 중단 후 복구 | 실제 모델로 확인 |
| 40분 반복 생성 | 95회 완료. **목표 분량을 채운 것은 27회** |
| 실제 Claude 호출 | **검증하지 못함** |
| 장편 원고의 품질 | 보장하지 않음 |
| 배포 서버에서의 성능 | 측정한 적 없음 (모두 단일 로컬 PC) |

실제 Gemma의 일부 Continuity 지적은 문장이 불완전하거나 평가와 설명이 맞지 않았습니다. 그래서 검증 결과는 사람이 검토하도록 보여 주기만 합니다. 측정 조건과 수치는 [ollama-performance.md](../docs/guidance/ollama-performance.md)에 있습니다.

## 관련 문서

- [기획서 §6, §7, §8.3](../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
- [docs/novel/](../docs/novel/README.md) — Continuity 규칙
- [api-v1.md](../docs/plan/api-v1.md), [CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md)
- [system-overview.md](../docs/Architecture/system-overview.md) — 서버 내부 실행 조정
- 저장소 안내 [../README.md](../README.md) · 작업 규칙 [../AGENTS.md](../AGENTS.md) · 보안 [../SECURITY.md](../SECURITY.md) · 라이선스 [../LICENSE](../LICENSE)
