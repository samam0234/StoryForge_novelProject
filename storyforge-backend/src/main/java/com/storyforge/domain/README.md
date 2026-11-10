# storyforge-backend/src/main/java/com/storyforge/domain/

도메인별 **Entity, DTO, Repository**를 둡니다. 기획서 §10 데이터 모델 초안이 기준이고, 스키마는 `src/main/resources/db/migration/`이 정합니다.

## 들어가지 않을 것

- 비즈니스 로직 → `service/`

## 패키지

| 패키지 | 파일 | 내용 |
|--------|------|------|
| `user/` | `AppUser`, `AppUserRepository`, `AuthDtos` | 사용자(이메일, 비밀번호 해시). 기획서 §8.2에는 없고 인증을 위해 추가 ([auth.md](../../../../../../../docs/plan/auth.md)) |
| `project/` | `Project`, `ProjectStatus`, `ProjectRepository`, `ProjectDtos`, `ProjectCollaborator`, `WorkCommit`, `CollabDtos` | 프로젝트, 파이프라인 상태, 설정, 낙관적 락, Clone 출처, 공동 창작자와 작업 커밋 |
| `bible/` | `StoryBible`, `Character`, `StoryBibleVersion`, 각 Repository, `BibleDtos` | Bible과 캐릭터(`currentState` JSONB), 버전 이력 스냅샷 |
| `outline/` | `Outline`, `OutlineChapter`, `OutlineRepository`, `OutlineDtos` | Outline(`chapters` JSONB) |
| `chapter/` | `Chapter`, `ChapterStatus`, `ChapterVersion`, `ContinuityReport`, `ContinuityIssue`, 각 Repository, `ChapterDtos` | 챕터(DRAFT/CONFIRMED), 생성·수정 이력, Continuity 리포트 |
| `community/` | `Work`, `WorkChapter`, `WorkRevision`, `WorkClone`, `WorkShare`, `PackageItem`, `PackageLink`, `PackageReport`, `Post`, `Comment`, `IssueEvent`, 각 Repository, `CommunityDtos` | 게시 작품(프로젝트와 독립된 스냅샷), Clone, 공유, 패키지포지, 라운지, 달성 ([community.md](../../../../../../../docs/plan/community.md)) |
| `moderation/` | `ContentReport`, `UserBlock`, 각 Repository, `ModerationDtos`, `ConsoleDtos` | 신고와 처리 결과, 사용자 차단, 운영 계약 |
| `notification/` | `Notification`, `NotificationRepository`, `NotificationDtos` | 알림 |
| `activity/` | `ActivityEvent`, `ActivityType`, `ActivityEventRepository`, `ActivityDtos` | 작업 그래프와 활동 잔디의 원본. 수동·에이전트 구분 |

## 규칙

- `ProjectStatus`의 이름과 순서는 기획서 §7.2 그대로입니다. 바꾸지 않습니다.
- 관리자가 숨긴 작품·글은 삭제하지 않고 `hidden_at`만 씁니다. 목록 쿼리는 이 값을 거릅니다.
- 공개 응답 DTO에 이메일을 넣지 않습니다 (`UserSummary`). 작업 기록 요약에 본문을 넣지 않습니다.
- `Work`, `Post`의 카운터는 리포지토리의 증감 쿼리로만 바꿉니다 (`@DynamicUpdate`로 다른 필드 저장이 덮어쓰지 않음).
- JSON 컬럼(`currentState`, `chapters`, `issues`, `generationContext`)은 `@JdbcTypeCode(SqlTypes.JSON)`을 씁니다.

## 관련 문서

- [기획서 §8.2, §10](../../../../../../../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
- [should-features.md](../../../../../../../docs/plan/should-features.md) — 이력과 설정
