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

## 현재 상태

폴더 구조와 README만 있습니다. 패키지 설정(`pyproject.toml` 등)과 `app/main.py`는 아직 만들지 않았습니다.
Python 버전과 패키지 관리 도구는 `docs/plan/CURRENT_STACK.md`에 먼저 확정합니다.

## 관련 문서

- [기획서 §7, §8.3](../StoryForge_기획서_v1.2.md)
- [docs/Architecture/system-overview.md](../docs/Architecture/system-overview.md)
- [docs/novel/](../docs/novel)
- 저장소 안내: [../README.md](../README.md), 작업 규칙: [../AGENTS.md](../AGENTS.md)
