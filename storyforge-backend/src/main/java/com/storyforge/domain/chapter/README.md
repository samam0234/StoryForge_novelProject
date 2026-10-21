# storyforge-backend/src/main/java/com/storyforge/domain/chapter/

**Chapter** 엔티티와 관련 DTO·Repository입니다.

## 들어갈 것

- Chapter: projectId, number, title, content, status (DRAFT/CONFIRMED), summary, generationContext, createdAt (§10)
- ContinuityReport: chapterId, issues[] (type, description, severity), score, createdAt (§10)

> ContinuityReport의 패키지 위치는 기획서에 따로 정해져 있지 않습니다. `chapterId`로 Chapter에 연결되므로 이 패키지를 후보로 둡니다.

## 관련 문서

- [기획서 §6.3, §10](../../../../../../../../StoryForge_기획서_v1.2.md)
- 상위 폴더: [../README.md](../README.md)
