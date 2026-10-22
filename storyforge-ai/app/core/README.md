# storyforge-ai/app/core/

**설정 로딩과 LLM 클라이언트 추상화**를 둡니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `config.py` | 환경 변수 설정 (`LLM_PROVIDER`, `LLM_MODEL`, `LLM_EFFORT` 등) |
| `llm.py` | LLM 추상화: `MockLLM`, `AnthropicLLM` (refusal fallback, 거절·잘림 감지) |

## 들어갈 것

- `config.py` — 환경 변수 기반 설정
- `llm.py` — LLM 클라이언트 추상화, 공급자(OpenAI, Anthropic 등) 교체 가능
- AI 비용 추적 훅 (§12 관측성)

## 들어가지 않을 것

- API Key 값 (환경 변수로 주입, 커밋 금지)

## 관련 문서

- [기획서 §8.3, §12, §13](../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
