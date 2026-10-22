# storyforge-backend/src/main/java/com/storyforge/domain/project/

**Project** 엔티티와 관련 DTO·Repository입니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `Project.java` | 엔티티 |
| `ProjectStatus.java` | 파이프라인 상태 enum |
| `ProjectRepository.java` | 리포지토리 |
| `ProjectDtos.java` | 요청·응답 record |

## 들어갈 것

- Project: id, title, genre, status, ownerId, createdAt, updatedAt (§10)
- `status`는 파이프라인 상태 머신 값

## 관련 문서

- [기획서 §10](../../../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
