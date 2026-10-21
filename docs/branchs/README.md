# docs/branchs/

브랜치와 커밋 기록을 남기는 폴더입니다. **커밋마다 기록합니다. merge나 작은 수정도 예외가 아닙니다.**

## 구성

| 경로 | 내용 |
|------|------|
| [TEMPLATE.md](TEMPLATE.md) | 커밋 기록 파일 템플릿 |
| [commits/](commits/) | 커밋 기록 파일 |
| `README.md` (이 문서) | 브랜치 목록, 커밋 기록 목록 |

## 기록 절차

1. 작업 브랜치에서 커밋하고 커밋 ID(짧은 해시)를 확인합니다.
2. `commits/`에 `YYMMDD_HHMM_[커밋ID]_[커밋이름]_[브랜치].md` 파일을 [TEMPLATE.md](TEMPLATE.md) 형식으로 만듭니다.
   - 예: `261022_0230_a1b2c3d_chapter-continuity_feature-ai.md`
   - `HHMM`은 한 덩어리로, 브랜치의 `/`는 `-`로 바꿉니다.
3. 아래 "커밋 기록 목록" 표에 한 줄을 추가합니다.
4. 기록 파일과 이 문서 변경을 **같은 브랜치에** 커밋합니다.

커밋 메시지 형식은 [commit-message.md](../../.agents/skills/storyforge/commit-message.md), 브랜치 전략은 [branch-merge.md](../../.agents/skills/storyforge/branch-merge.md)를 따릅니다.

## 브랜치 목록

| 브랜치 | 목적 | 분기 원본 | 생성일 | 상태 |
|--------|------|-----------|--------|------|
| `main` | 배포 기준 통합 | — | 2026-10-22 | 진행 중 |
| `develop` | 개발 통합 | `main` | 2026-10-22 | 진행 중 |
| `feature/project-scaffold` | 프로젝트 뼈대, 문서 규칙, 에이전트 어댑터 | `develop` | 2026-10-22 | 진행 중 |
| `feature/web-foundation` | 프론트·백엔드·AI 서비스 기초 구현 (뼈대 브랜치 위에 쌓음) | `feature/project-scaffold` | 2026-10-22 | 진행 중 |

상태: `예정` / `진행 중` / `develop 병합` / `main 병합` / `폐기`

## 커밋 기록 목록

최신 기록을 위에 추가합니다.

| 일시 | 커밋 ID | 커밋 이름 | 브랜치 | 기록 |
|------|---------|-----------|--------|------|
| 2026-10-22 03:14 | `1bd8e86` | chore(docs): 프로젝트 뼈대와 문서 규칙 구성 | `feature/project-scaffold` | [기록](commits/261022_0314_1bd8e86_project-scaffold_feature-project-scaffold.md) |
| 2026-10-22 03:13 | `63740e1` | chore(repo): 저장소 초기화 | `main` | [기록](commits/261022_0313_63740e1_repo-init_main.md) |
