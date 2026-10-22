# storyforge-frontend/src/app/features/chapters/

**챕터 생성과 편집** 화면입니다. 스트리밍으로 생성되는 챕터를 보고, Continuity 리포트를 확인한 뒤 승인하거나 재생성합니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `chapter-workspace.page.ts` | 3단 워크스페이스, 스트리밍 생성, 수정·승인·재생성 |

## 들어갈 것

- 3단 레이아웃: 좌측 Outline + 관련 Bible 항목, 중앙 에디터(스트리밍 중 "AI writing..."), 우측 Continuity 리포트·캐릭터 상태 (§9.5, §9.6 4번)
- 하단 승인 / 재생성 버튼 — 승인 `PUT /api/v1/chapters/{id}/confirm`
- TipTap 리치 텍스트 에디터 (§9.2)

## 들어가지 않을 것

- LLM 호출, Continuity 판단 로직
- 검토(CHAPTER_REVIEW)를 건너뛰는 자동 승인

## 관련 문서

- [기획서 §6.3, §7.2, §9.5, §9.6](../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
