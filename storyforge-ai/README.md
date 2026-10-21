# storyforge-ai/

StoryForge의 **FastAPI AI Service**입니다. 프롬프트 조립, LLM 스트리밍 호출, Continuity 검증을 맡습니다.
**내부망 전용**이며 Spring Boot만 호출합니다. 브라우저에서 직접 접근할 수 없어야 합니다.

## 들어갈 것

- FastAPI, Pydantic v2, httpx / OpenAI SDK, LangChain 선택적 도입 (기획서 §12)
- AI 파이프라인 실행: Bible 생성, Outline 생성, 챕터 생성, Continuity 검증 (§7.3)
- LLM 스트리밍 호출 (OpenAI, Anthropic 등, §7.1)

## 들어가지 않을 것

- 인증, 프로젝트 상태 관리, DB 영속화 (Spring Boot 담당)
- 외부(브라우저) 노출 엔드포인트
- 포트 하드코딩 (값은 `docs/plan/CURRENT_STACK.md`)

## 하위 폴더

| 폴더 | 내용 |
|------|------|
| [app/](app/) | 애플리케이션 패키지 |
| [tests/](tests/) | 테스트 (mock 공급자) |

## 현재 상태

FastAPI + anthropic SDK로 기초 AI 파이프라인을 구현했습니다.

- `LLM_PROVIDER=mock`(기본): API 키 없이 동작하는 개발용 생성기. 생성물에 mock 표시가 붙습니다
- `LLM_PROVIDER=anthropic`: Claude `claude-opus-5-5`, 서버 측 refusal fallback, effort 명시
- 챕터: 토큰 스트리밍 → 요약 → Continuity 검증(규칙 기반 + LLM) → `done`
- 실행 `.venv/Scripts/python -m uvicorn app.main:app --host 127.0.0.1 --port 8000`, 테스트 `pytest` ([tests/](tests/))

의존성은 `requirements.txt`(실행), `requirements-dev.txt`(테스트)에 있습니다. 실행 방법은 [docs/guidance/local-dev-setup.md](../docs/guidance/local-dev-setup.md)를 보세요.

## 관련 문서

- [기획서 §7, §8.3](../StoryForge_기획서_v1.2.md)
- [docs/Architecture/system-overview.md](../docs/Architecture/system-overview.md)
- [docs/novel/](../docs/novel)
- 저장소 안내: [../README.md](../README.md), 작업 규칙: [../AGENTS.md](../AGENTS.md)
