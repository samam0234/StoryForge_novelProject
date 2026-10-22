# storyforge-backend/src/main/java/com/storyforge/service/project/

**프로젝트** 생성·조회·수정·삭제와 진행률 계산을 맡습니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `ProjectService.java` | 프로젝트 CRUD, 진행률·다음 챕터 번호 계산 |
| `OutlineService.java` | Outline 저장·조회 (order 재번호) |

## 들어갈 것

- 프로젝트 CRUD
- 대시보드용 진행률, 최근 활동 (§6.4)

## 들어가지 않을 것

- 상태 전이 → `orchestration/`

## 관련 문서

- [기획서 §6.4, §10](../../../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
