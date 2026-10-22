# storyforge-frontend/src/app/features/story-bible/

**Story Bible 에디터** 화면입니다. 세계관, 캐릭터, 테마, 톤을 AI로 생성하고 사용자가 편집·확정합니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `story-bible.page.ts` | AI 생성, 4개 탭 편집, 저장·확정 |

## 들어갈 것

- 상단 탭: Overview, Characters, World, Themes (§9.6 2번)
- 중앙 에디터 + AI 생성 패널, 저장 상태 표시
- 생성 요청 `POST /api/v1/projects/{id}/bible/generate`, 저장 `PUT /api/v1/projects/{id}/bible` (§11)

## 들어가지 않을 것

- Bible 생성 프롬프트, LLM 호출

## 관련 문서

- [기획서 §6.1, §9.5, §9.6](../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
