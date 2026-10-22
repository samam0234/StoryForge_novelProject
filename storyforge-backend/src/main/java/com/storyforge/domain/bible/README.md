# storyforge-backend/src/main/java/com/storyforge/domain/bible/

**StoryBible, Character** 엔티티와 관련 DTO·Repository입니다.

## 현재 파일

| 파일 | 내용 |
|------|------|
| `StoryBible.java` | 엔티티 (캐릭터 목록 포함) |
| `Character.java` | 엔티티 (`currentState` JSONB) |
| `StoryBibleRepository.java` | 리포지토리 |
| `BibleDtos.java` | 요청·응답 record |

## 들어갈 것

- StoryBible: projectId, worldSetting, theme, tone, version, updatedAt (§10)
- Character: bibleId, name, description, personality, relationships, currentState (JSON) (§10)

## 관련 문서

- [기획서 §6.1, §10](../../../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
