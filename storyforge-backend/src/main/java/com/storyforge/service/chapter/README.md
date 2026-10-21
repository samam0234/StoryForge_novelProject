# storyforge-backend/src/main/java/com/storyforge/service/chapter/

**챕터**의 저장, 요약 보관, Continuity 리포트 보관을 맡습니다.

## 들어갈 것

- Chapter CRUD, 상태(DRAFT/CONFIRMED) 저장
- 챕터 요약, 생성 컨텍스트(`generationContext`) 보관
- ContinuityReport 저장·조회
- 생성 히스토리 (§6.4)

## 들어가지 않을 것

- 챕터 생성·Continuity 검증 실행 (FastAPI)
- 프로젝트 상태 전이 → `orchestration/`

## 관련 문서

- [기획서 §6.3, §10](../../../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
