# storyforge-backend/src/main/java/com/storyforge/service/orchestration/

**파이프라인 상태 머신과 오케스트레이션**을 맡는 핵심 패키지입니다. 프로젝트 상태 전이는 여기서만 일어납니다.

## 들어갈 것

- 상태 머신: `BIBLE_DRAFT → BIBLE_CONFIRMED → OUTLINE_DRAFT → OUTLINE_CONFIRMED → CHAPTER_GENERATING → CHAPTER_REVIEW → COMPLETED`
- 생성 요청 전 상태 확인 (예: 챕터 생성 시 BIBLE_CONFIRMED, OUTLINE_CONFIRMED 여부, §7.2)
- 컨텍스트 패키지 조립: Bible 항목 + Outline + 이전 챕터 요약 + 캐릭터 상태 → `client/`로 FastAPI 호출
- 생성 결과 Draft 저장, 사용자 승인 후 Confirmed 전환

## 들어가지 않을 것

- 상태 이름·순서 임의 변경 (정본 §0)
- CHAPTER_REVIEW를 건너뛰는 자동 전이
- Continuity 결과 무시

## 관련 문서

- [기획서 §7.2](../../../../../../../../StoryForge_기획서_v1.2.md)
- [docs/Architecture/pipeline-state-machine.md](../../../../../../../../docs/Architecture/pipeline-state-machine.md)
- 상위 폴더: [../README.md](../README.md)
