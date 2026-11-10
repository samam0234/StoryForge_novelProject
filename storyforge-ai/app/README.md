# storyforge-ai/app/

FastAPI **애플리케이션 패키지**입니다. 기획서 §8.3 구조를 따릅니다. 이 README가 `api`, `core`, `models`를 함께 설명하고, `services`와 `prompts`는 자체 README가 있습니다.

## 의존 방향

```
../main.py → orchestration.create_app → api/orchestration → api/v1/orchestration
api/v1 → api/dependencies → services/orchestration → 개별 AI 서비스 → core/llm
                                                ├→ models/orchestration (컨텍스트·이벤트)
                                                └→ prompts/orchestration (템플릿 렌더링)
orchestration.lifespan → core/orchestration (공급자 선택·예열·연결 정리)
```

## 파일

| 위치 | 내용 |
|------|------|
| `../main.py` | 정식 ASGI 진입점. 앱 팩토리로 서버 한 개 생성 |
| `main.py` | 이전 `app.main:app` 명령 호환. 루트와 같은 인스턴스 참조 |
| `orchestration.py` | `create_app()`, `/health`, 모델 수명주기와 HTTP 계층 조립 |
| `api/orchestration.py` | API 등록, `LLMError` → 502, 추론 대기 초과 → 503 |
| `api/dependencies.py` | 요청에 모델 클라이언트와 `AiTaskOrchestrator` 주입 |
| `api/v1/orchestration.py` | 생성·가이드·패키지 라우터와 prefix 조립 |
| `api/v1/bible.py`, `outline.py` | `POST /internal/generate/bible`, `/outline` |
| `api/v1/chapter.py` | `POST /internal/generate/chapter` — SSE (`token` → `done` 또는 `error`), `POST /internal/generate/review` — 재요약·재검증 |
| `api/v1/package.py` | `POST /internal/package/continuity` — 패키지 작품 사이의 검사 |
| `api/v1/guide.py` | `POST /internal/guide/chat` — AI 가이드챗 |
| `core/ollama.py` | HTTP 연결 풀, 추론 슬롯, 예열·모델 상주, JSON 스키마·스트림 완료 검증 |
| `core/config.py` | 환경 변수 설정 (`LLM_PROVIDER`, `LLM_MODEL`, `LLM_EFFORT` 등) |
| `core/orchestration.py` | `RoutingLLM`, 프로세스 단위 `get_llm()`, 시작 예열·종료·캐시 해제 |
| `core/llm.py` | LLM 인터페이스, `MockLLM`, `AnthropicLLM`(refusal fallback, 거절·잘림 감지) |
| `models/schemas.py` | Pydantic 스키마. JSON은 camelCase이며 백엔드 `AiDtos.java`와 짝 |
| `models/orchestration.py` | 챕터 컨텍스트 조립, 내부 `Token`·`Done` 이벤트 |
| `services/orchestration.py` | 모든 AI 작업의 진입점. 챕터 본문 → 요약 → Continuity 순서·중단 전파 |
| `prompts/orchestration.py` | Jinja 환경과 템플릿 렌더링 |

## 규칙

- 라우터는 HTTP·SSE 변환을 맡고 `AiTaskOrchestrator`를 호출합니다. 개별 서비스에서 상위 조정기를 역참조하지 않습니다.
- 패키지 초기화 파일은 등록 부작용을 두지 않습니다. 조정 기능은 명시적인 `orchestration.py`에서 가져옵니다.
- 인증, 프로젝트 상태 관리, DB 저장은 하지 않습니다 (Spring Boot 담당). 인증이 없으므로 **외부에 노출하지 않습니다**.
- 내부 API 계약을 바꾸면 `schemas.py`와 `AiDtos.java`를 같은 커밋에서 바꿉니다.
- API 키를 코드에 넣지 않습니다.

## 관련 문서

- [기획서 §7, §8.3](../../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
- [docs/plan/api-v1.md](../../docs/plan/api-v1.md) — 내부 API 형식
