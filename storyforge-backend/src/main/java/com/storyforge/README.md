# storyforge-backend/src/main/java/com/storyforge/

백엔드의 **루트 패키지**입니다. 기획서 §8.2의 계층 구조를 따릅니다. 이 README가 하위 패키지(`controller`, `service`, `client`, `exception`)의 파일을 함께 설명합니다. `config`, `domain`, `service/orchestration`은 자체 README가 있습니다.

## 의존 방향

```
controller → service(orchestration, project, bible, chapter, auth) → domain(repository)
                    └→ client (FastAPI)
```

계층 밖에 클래스를 흩어 두지 않고 아래 패키지 중 하나에 넣습니다. 진입점은 `StoryforgeApplication.java` 하나입니다.

## controller/ — REST (`/api/v1`)

비즈니스 로직과 상태 전이는 `service/`에 맡깁니다. 엔드포인트 목록은 [api-v1.md](../../../../../../docs/plan/api-v1.md)에 있습니다.

| 파일 | 내용 |
|------|------|
| `AuthController` | `/auth/register`, `/auth/login`, `/auth/me` |
| `ProjectController` | 프로젝트 CRUD, 설정, 상태, 내보내기 |
| `BibleController` | Bible 조회·저장·생성·확정, 버전 이력 |
| `OutlineController` | Outline 조회·저장·생성·확정 |
| `ChapterController` | 챕터 목록, 생성(SSE), 조회·수정·승인, 이력 |
| `ProjectCommunityController` | 프로젝트 게시, 작업 기록, 원본 비교 |
| `WorkController` | 게시판, 작품 읽기·수정·삭제, 내려받기, 작업 그래프, 추천, 댓글, Clone, 공유 |
| `PackageController` | 패키지포지 편집, 편입 요청 |
| `PostController` | 창작 라운지 |
| `UserController` | 프로필, 활동 잔디, 업적, 이슈, 계정 설정 |
| `GuideController` | AI 가이드챗 |
| `CollaborationController` | 함께 창작: 초대, 참여자, 작업 커밋 |
| `ConsoleController` | 운영 집계, 회원 검색·정지, 일괄 조치, 감사 기록 |
| `ModerationController` | 신고, 차단, 관리자 처리 |
| `NotificationController` | 내 알림 |

## service/ — 비즈니스 로직

| 패키지 | 파일 | 내용 |
|--------|------|------|
| `moderation/` | `ModerationService`, `ConsoleService`, `AdminAudit` | 신고, 관리자 조치, 차단 ([moderation.md](../../../../../../docs/plan/moderation.md)) |
| `notification/` | `NotificationService` | 알림. 호출한 쪽 트랜잭션에 묶임 |
| `community/` | `WorkService`, `CloneService`, `ShareService`, `PackageService`, `PackageContinuityService`, `EngagementService`, `PostService`, `IssueService`, `ActivityService`, `AchievementService`, `ProfileService`, `GuideService` | 커뮤니티 ([community.md](../../../../../../docs/plan/community.md)) |
| `orchestration/` | `PipelineStateMachine`, `PipelineOrchestrator`, `PipelineRecovery` | 상태 머신, 단계 실행, 시작 시 복구 ([README](service/orchestration/README.md)) |
| `project/` | `ProjectService`, `OutlineService`, `ExportService`, `CollaborationService` | 프로젝트·Outline, 내보내기, 함께 창작. `ManuscriptBlocks`가 Markdown을 블록으로 바꾸고 `ManuscriptDocxWriter`·`ManuscriptPdfWriter`·`ManuscriptWriters`(TXT, HTML, RTF, EPUB, ODT)가 파일을 만듦 |
| `bible/` | `BibleService` | Bible 전체 교체 저장, 버전 증가, 이력 |
| `chapter/` | `ChapterService` | 챕터 Draft·리포트 저장, 수정, 재검증 결과 반영, Clone·원본 가져오기, 이력 |
| `auth/` | `AuthService`, `TokenService`, `CurrentUser`, `AccountPolicy` | 가입·로그인, HS256 토큰 발급, 요청 스레드의 사용자, 관리자·약관 판별 |

기획서 §8.2에는 `service/outline`과 `service/auth`가 없어, Outline은 `project/`에 두고 `auth/`는 인증 구현을 위해 추가했습니다 ([auth.md](../../../../../../docs/plan/auth.md)).

## client/ — FastAPI 호출

백엔드에서 FastAPI로 가는 호출은 모두 여기를 거칩니다. LLM을 직접 호출하지 않습니다.

| 파일 | 내용 |
|------|------|
| `AiServiceClient` | Bible·Outline·재검증·패키지 검사·가이드챗 동기 호출(연결 실패 1회 재시도), 챕터 SSE, Circuit Breaker |
| `AiDtos` | 내부 계약 record. `storyforge-ai/app/models/schemas.py`와 짝이라 한쪽을 바꾸면 다른 쪽도 함께 바꿉니다 |
| `AiServiceProperties` | `storyforge.ai.*` 설정 |

## exception/ — 예외 → ProblemDetail

| 파일 | 응답 |
|------|------|
| `GlobalExceptionHandler` | 아래 예외를 ProblemDetail로 변환. 동시 요청 충돌은 409, 입력 검증은 400 |
| `NotFoundException` | 404 |
| `InvalidStateException` | 409 (현재 상태에서 불가) |
| `ForbiddenException` | 403 (관리자 전용, 정지된 계정) |
| `AuthenticationFailedException` | 401 |
| `AiServiceException` | 502 (내부 주소는 응답에 노출하지 않음) |

## 관련 문서

- [기획서 §8.2](../../../../../../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
- [docs/plan/api-v1.md](../../../../../../docs/plan/api-v1.md), [docs/plan/stability.md](../../../../../../docs/plan/stability.md)
