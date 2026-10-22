# storyforge-backend/src/main/java/com/storyforge/domain/outline/

**Outline** 엔티티와 관련 DTO·Repository입니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `Outline.java` | 엔티티 (`chapters` JSONB) |
| `OutlineChapter.java` | 챕터 한 칸 record |
| `OutlineRepository.java` | 리포지토리 |
| `OutlineDtos.java` | 요청·응답 record |

## 들어갈 것

- Outline: projectId, chapters[] (order, title, beats, characters, goals, continuityPoints) (§10)

## 관련 문서

- [기획서 §6.2, §10](../../../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
