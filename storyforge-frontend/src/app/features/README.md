# storyforge-frontend/src/app/features/

기능별 화면과 그 기능 전용 서비스를 둡니다. 기능 하나에 폴더 하나입니다. 이 README가 모든 기능 폴더를 설명합니다.

| 폴더 | 파일 | 화면 (경로) | 기획서 |
|------|------|-------------|--------|
| `auth/` | `login.page.ts` | 로그인·가입 (`/login`) | §7.1 |
| `dashboard/` | `dashboard.page.ts` | 내 프로젝트: 다음 할 일, 상태 필터, 진행률, 수동·에이전트 비율, 게시·Clone 표시 (`/`) | §9.6-1 |
| `projects/` | `project-shell.page.ts` | 프로젝트 상단(제목, 파이프라인 단계, 내보내기)과 Bible/Outline/챕터/설정 탭 (`/projects/:id`) | §9.1 |
| | `project-context.ts` | 프로젝트·상태 신호를 하위 화면과 공유 | |
| | `project-settings.page.ts` | 제목, 장르, 모델, 기본 톤, 내보내기 형식, 삭제 (`.../settings`) | §9.6-5 |
| | `project-create.dialog.ts` | 새 프로젝트 입력 (장편·단편) | |
| | `project-activity.page.ts` | 작업 기록, 함께 쓰기, 작업 커밋 (`.../activity`) | |
| | `project-publish.page.ts` | 게시 (`.../publish`) | |
| | `project-upstream.page.ts` | Clone 원본 비교 (`.../upstream`) | |
| `story-bible/` | `story-bible.page.ts` | AI 생성, Overview/Characters/World/Themes 편집, 이력 비교, 확정 (`.../bible`) | §6.1, §9.6-2 |
| `outline/` | `outline.page.ts` | AI 생성, 드래그 앤 드롭 재배치, 챕터별 편집, 확정 (`.../outline`) | §6.2, §9.6-3 |
| `chapters/` | `chapter-workspace.page.ts` | 3단 워크스페이스: 스트리밍 생성, TipTap 편집, Continuity 리포트, 이력 비교, 승인·재생성 (`.../chapters`) | §6.3, §9.6-4 |
| `board/` | `board.page.ts` | 롱품, 숏품, 패키지포지 게시판 (`/board/:kind`) | |
| | `work-detail.page.ts` | 작품 읽기, 세계관, 작업 그래프, 공유 그래프, Clone 네트워크, 이력, 댓글, 패키지 구성 (`/works/:id`) | |
| | `clone.dialog.ts`, `share.dialog.ts`, `comment-thread.component.ts` | Clone 범위, 공유 링크, 댓글 | |
| `lounge/` | `create.page.ts`, `post-detail.page.ts`, `post-edit.dialog.ts` | 창작하기, 함께 창작 초대, 창작 라운지 (`/create`, `/posts/:id`) | |
| `issues/` | `issues.page.ts` | 달성 축하, 인기작, 새 작품 (`/issues`) | |
| `packages/` | `package-forge.page.ts` | 패키지 생성·편집 (`/package-forge[/:id]`) | |
| `profile/` | `profile.page.ts` | 프로필, 활동 잔디, 업적 (`/users/:id`) | |
| | `account-settings.page.ts`, `my-works.page.ts`, `work-edit.page.ts` | 계정 설정, 내 작품 관리, 게시 작품 수정 (`/me/...`) | |
| `guide/` | `guide-chat.component.ts` | AI 가이드챗 패널 (상단 버튼) | |

스펙은 [docs/plan/community.md](../../../../docs/plan/community.md)에 있습니다.

## 규칙

- LLM 호출, Continuity 판단, 상태 전이 로직을 넣지 않습니다. 화면은 백엔드가 알려 준 상태를 보여 주고 요청만 보냅니다.
- `CHAPTER_REVIEW` 단계를 건너뛰는 자동 승인을 만들지 않습니다. 심각도 high 이슈가 있으면 승인 전에 한 번 더 확인합니다.
- 확정된 Bible·Outline·챕터는 읽기 전용으로 보여 줍니다.
- 다른 사용자가 쓴 글은 `innerHTML`로 넣지 않습니다. 본문은 읽기 전용 TipTap, 댓글·게시글은 글자로 보여 줍니다.
- `effect` 안에서 데이터를 읽어 올 때는 일부러 추적할 신호만 읽습니다. 날짜·페이지 같은 신호를 함께 추적하면 화면 전체가 다시 그려집니다.
- 한 기능에서만 쓰는 컴포넌트는 그 기능 폴더에, 두 곳 이상에서 쓰면 `shared/`로 옮깁니다.

## 관련 문서

- [기획서 §8.1, §9.6](../../../../StoryForge_기획서_v1.2.md)
- [docs/plan/should-features.md](../../../../docs/plan/should-features.md)
