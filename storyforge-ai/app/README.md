# storyforge-ai/app/

FastAPI **애플리케이션 패키지**입니다. 기획서 §8.3 구조를 따릅니다. 이 README가 `api`, `core`, `models`를 함께 설명하고, `services`와 `prompts`는 자체 README가 있습니다.

## 의존 방향

```
api/v1 → services → core/llm
   └────→ models (Pydantic)
services → prompts (템플릿)
```

## 파일

| 위치 | 내용 |
|------|------|
| `main.py` | FastAPI 앱, 라우터 등록, `/health`, 수명주기 연결 정리, `LLMError` → 502, 추론 대기 초과 → 503 |
| `api/v1/__init__.py` | `/internal/generate` prefix로 라우터 묶음 |
| `api/v1/bible.py`, `outline.py` | `POST /internal/generate/bible`, `/outline` |
| `api/v1/chapter.py` | `POST /internal/generate/chapter` — SSE (`token` → `done` 또는 `error`), `POST /internal/generate/review` — 재요약·재검증 |
| `api/v1/package.py` | `POST /internal/package/continuity` — 패키지 작품 사이의 검사 |
| `api/v1/guide.py` | `POST /internal/guide/chat` — AI 가이드챗 |
| `core/ollama.py` | HTTP 연결 풀, 추론 슬롯, 예열·모델 상주, JSON 스키마·스트림 완료 검증 |
| `core/config.py` | 환경 변수 설정 (`LLM_PROVIDER`, `LLM_MODEL`, `LLM_EFFORT` 등) |
| `core/llm.py` | LLM 추상화. `MockLLM`, `RoutingLLM`, `AnthropicLLM`(refusal fallback, 프로젝트별 모델, 거절·잘림 감지) |
| `models/schemas.py` | Pydantic 스키마. JSON은 camelCase이며 백엔드 `AiDtos.java`와 짝 |

## 규칙

- 라우터에는 로직을 넣지 않고 `services/`를 호출합니다.
- 인증, 프로젝트 상태 관리, DB 저장은 하지 않습니다 (Spring Boot 담당). 인증이 없으므로 **외부에 노출하지 않습니다**.
- 내부 API 계약을 바꾸면 `schemas.py`와 `AiDtos.java`를 같은 커밋에서 바꿉니다.
- API 키를 코드에 넣지 않습니다.

## 관련 문서

- [기획서 §7, §8.3](../../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
- [docs/plan/api-v1.md](../../docs/plan/api-v1.md) — 내부 API 형식
