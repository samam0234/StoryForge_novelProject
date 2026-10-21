# storyforge-frontend/src/app/features/

기능별 화면과 그 기능 전용 서비스·컴포넌트를 둡니다. 기능 하나에 폴더 하나입니다.

## 들어갈 것

- 화면 컴포넌트, 기능 전용 서비스, 기능 라우트

## 들어가지 않을 것

- LLM 호출, Continuity 판단 로직 (FastAPI 담당)
- 다른 기능 폴더를 직접 import하는 강한 결합

## 하위 폴더

| 폴더 | 내용 |
|------|------|
| [chapters/](chapters/) | 챕터 워크스페이스 |
| [dashboard/](dashboard/) | 프로젝트 대시보드 |
| [outline/](outline/) | Outline 관리 |
| [projects/](projects/) | 프로젝트 목록/생성 |
| [story-bible/](story-bible/) | Story Bible 에디터 |

## 관련 문서

- [기획서 §8.1, §9.6](../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
