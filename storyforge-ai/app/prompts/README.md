# storyforge-ai/app/prompts/

**프롬프트 템플릿** 파일(`.txt` 또는 Jinja)을 둡니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `chapter_analysis.j2` | Continuity 컨텍스트와 본문을 공유하는 요약·검증 공동 분석 |
| `chapter_scenes.j2` | 사건이 3개 이상인 챕터의 두 구간 생성, 실제 앞 구간과 남은 목표 분량 전달 |
| `orchestration.py` | `render(name, **context)` — Jinja2 로더 (`StrictUndefined`). 모든 서비스가 사용하는 템플릿 진입점 |
| `system.j2` | 공통 시스템 프롬프트 |
| `_bible_context.j2` | Story Bible 블록 (include용) |
| `bible.j2` | Bible 생성 |
| `outline.j2` | Outline 생성 |
| `chapter.j2` | 챕터 본문. 이전 장을 다시 쓰지 않고 장면마다 다른 선택으로 진행 |
| `complete.j2` | 게시본 이어쓰기. 앞 문장을 반복하지 않는 짧은 문단 |
| `summary.j2` | 다음 챕터용 요약 |
| `continuity.j2` | Continuity 검증 |
| `package_continuity.j2` | 패키지 작품 사이의 검증 |
| `guide.j2`, `guide_question.j2` | AI 가이드챗 시스템 프롬프트와 질문 |

## 들어갈 것

- 단계별 템플릿 (Bible, Outline, Chapter, Continuity)
- 컨텍스트 주입 자리표시자

## 들어가지 않을 것

- 코드 안에 하드코딩한 긴 프롬프트
- 실유저 데이터, 생성 소설 원문 예시

## 관련 문서

- [기획서 §8.3](../../../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
- [docs/novel/](../../../docs/novel) (프롬프트 작성 규칙)
