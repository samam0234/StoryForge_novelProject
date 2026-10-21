# storyforge-ai/app/

FastAPI **애플리케이션 패키지**입니다. 기획서 §8.3 구조를 따릅니다.

## 들어갈 것

- `main.py` — FastAPI 앱 생성, 라우터 등록 (예정)

## 하위 폴더

| 폴더 | 내용 |
|------|------|
| [api/](api/) | API 라우터 |
| [core/](core/) | 설정과 LLM 추상화 |
| [models/](models/) | Pydantic 스키마 |
| [prompts/](prompts/) | 프롬프트 템플릿 |
| [services/](services/) | AI 파이프라인 서비스 |

## 의존 방향

```
api/v1 → services → core/llm
   └──────→ models (Pydantic)
services → prompts (템플릿)
```

## 관련 문서

- [기획서 §8.3](../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
