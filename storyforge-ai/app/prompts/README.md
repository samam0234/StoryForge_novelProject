# storyforge-ai/app/prompts/

**프롬프트 템플릿** 파일(`.txt` 또는 Jinja)을 둡니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `__init__.py` | `render(name, **context)` — Jinja2 로더 (`StrictUndefined`) |
| `system.j2` | 공통 시스템 프롬프트 |
| `_bible_context.j2` | Story Bible 블록 (include용) |
| `bible.j2` | Bible 생성 |
| `outline.j2` | Outline 생성 |
| `chapter.j2` | 챕터 본문 |
| `summary.j2` | 다음 챕터용 요약 |
| `continuity.j2` | Continuity 검증 |

## 들어갈 것

- 단계별 템플릿 (Bible, Outline, Chapter, Continuity)
- 컨텍스트 주입 자리표시자

## 들어가지 않을 것

- 코드 안에 하드코딩한 긴 프롬프트
- 실유저 데이터, 생성 소설 원문 예시

## 관련 문서

- [기획서 §8.3](../../../StoryForge_기획서_v1.2.md)
- [docs/novel/](../../../docs/novel) (프롬프트 작성 규칙)
- 상위 폴더: [../README.md](../README.md)
