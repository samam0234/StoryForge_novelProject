# storyforge-backend/src/main/java/com/storyforge/service/bible/

**Story Bible과 캐릭터**의 저장, 버전 관리, 조회를 맡습니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `BibleService.java` | Bible·캐릭터 전체 교체 저장, 버전 증가 |

## 들어갈 것

- Bible·Character CRUD
- 버전 관리 및 변경 이력 (§6.1)
- 챕터 생성 시 참조할 관련 항목 조회

## 들어가지 않을 것

- Bible AI 생성 로직 (FastAPI `bible_generator`)
- 확정 상태 전이 → `orchestration/`

## 관련 문서

- [기획서 §6.1, §10](../../../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
