# storyforge-frontend/src/app/features/

기능별 화면과 그 기능 전용 서비스를 둡니다. 기능 하나에 폴더 하나입니다. 이 README가 모든 기능 폴더를 설명합니다.

| 폴더 | 파일 | 화면 (경로) | 기획서 |
|------|------|-------------|--------|
| `auth/` | `login.page.ts` | 로그인·가입 (`/login`) | §7.1 |
| `dashboard/` | `dashboard.page.ts` | 프로젝트 카드 그리드, 진행률, 새 프로젝트 (`/`) | §9.6-1 |
| `projects/` | `project-shell.page.ts` | 프로젝트 상단(제목, 파이프라인 단계, 내보내기)과 Bible/Outline/챕터/설정 탭 (`/projects/:id`) | §9.1 |
| | `project-context.ts` | 프로젝트·상태 신호를 하위 화면과 공유 | |
| | `project-settings.page.ts` | 제목, 장르, 모델, 기본 톤, 내보내기 형식, 삭제 (`.../settings`) | §9.6-5 |
| | `project-create.dialog.ts` | 새 프로젝트 입력 | |
| `story-bible/` | `story-bible.page.ts` | AI 생성, Overview/Characters/World/Themes 편집, 이력 비교, 확정 (`.../bible`) | §6.1, §9.6-2 |
| `outline/` | `outline.page.ts` | AI 생성, 드래그 앤 드롭 재배치, 챕터별 편집, 확정 (`.../outline`) | §6.2, §9.6-3 |
| `chapters/` | `chapter-workspace.page.ts` | 3단 워크스페이스: 스트리밍 생성, TipTap 편집, Continuity 리포트, 이력 비교, 승인·재생성 (`.../chapters`) | §6.3, §9.6-4 |

## 규칙

- LLM 호출, Continuity 판단, 상태 전이 로직을 넣지 않습니다. 화면은 백엔드가 알려 준 상태를 보여 주고 요청만 보냅니다.
- `CHAPTER_REVIEW` 단계를 건너뛰는 자동 승인을 만들지 않습니다. 심각도 high 이슈가 있으면 승인 전에 한 번 더 확인합니다.
- 확정된 Bible·Outline·챕터는 읽기 전용으로 보여 줍니다.
- 한 기능에서만 쓰는 컴포넌트는 그 기능 폴더에, 두 곳 이상에서 쓰면 `shared/`로 옮깁니다.

## 관련 문서

- [기획서 §8.1, §9.6](../../../../StoryForge_기획서_v1.2.md)
- [docs/plan/should-features.md](../../../../docs/plan/should-features.md)
