# storyforge-frontend/src/app/core/models/

API 응답과 화면이 공유하는 **TypeScript 타입**을 둡니다. 기획서 §10 엔티티를 기준으로 합니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `models.ts` | 엔티티 타입, `ProjectStatus`, `PIPELINE_STAGES` |

## 들어갈 것

- `Project`, `StoryBible`, `Character`, `Outline`, `Chapter`, `ContinuityReport` (§10)
- 프로젝트 상태 타입: `BIBLE_DRAFT` … `COMPLETED` (상태 이름은 기획서 그대로)
- Chapter 상태: `DRAFT` / `CONFIRMED`

## 들어가지 않을 것

- 비즈니스 로직
- 상태 이름 임의 변경

## 관련 문서

- [기획서 §10](../../../../../StoryForge_기획서_v1.2.md)
- [docs/Architecture/pipeline-state-machine.md](../../../../../docs/Architecture/pipeline-state-machine.md)
- 상위 폴더: [../README.md](../README.md)
