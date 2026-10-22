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
| `main` | 배포 기준 통합 (GitHub 기본 브랜치) | — | 2026-10-22 | 진행 중 |
| `develop` | 개발 통합 | `main` | 2026-10-22 | main 병합 |
| `feature/project-scaffold` | 프로젝트 뼈대, 문서 규칙, 에이전트 어댑터 | `develop` | 2026-10-22 | develop 병합 |
| `feature/web-foundation` | 프론트·백엔드·AI 서비스 기초 구현 (뼈대 브랜치 위에 쌓음) | `feature/project-scaffold` | 2026-10-22 | develop 병합 |
| `feature/merge-records` | 병합 커밋 기록 | `develop` | 2026-10-22 | develop 병합 |
| `feature/merge-records-main` | main 병합 기록 | `develop` | 2026-10-22 | develop 병합 |
| `feature/diagnostics-readme` | VS Code 경고 정리, README 실행 가이드 개편 | `develop` | 2026-10-23 | 진행 중 |

상태: `예정` / `진행 중` / `develop 병합` / `main 병합` / `폐기`

## 커밋 기록 목록

최신 기록을 위에 추가합니다.

| 일시 | 커밋 ID | 커밋 이름 | 브랜치 | 기록 |
|------|---------|-----------|--------|------|
| 2026-10-23 00:11 | `d4f2e85` | docs(docs): 서비스 README를 실행 방법 중심으로 개편 | `feature/diagnostics-readme` | [기록](commits/261023_0011_d4f2e85_readme-run-guides_feature-diagnostics-readme.md) |
| 2026-10-23 00:11 | `ff57fb9` | fix(api): VS Code null 안전성 경고와 Spring 힌트 정리 | `feature/diagnostics-readme` | [기록](commits/261023_0011_ff57fb9_fix-null-safety-warnings_feature-diagnostics-readme.md) |
| 2026-10-22 03:51 | `ba40a40` | merge: develop into main (no-ff) | `main` | [기록](commits/261022_0351_ba40a40_merge-develop-into-main_main.md) |
| 2026-10-22 03:50 | `0af0fe0` | merge: feature/web-foundation into develop (no-ff) | `develop` | [기록](commits/261022_0350_0af0fe0_merge-web-foundation_develop.md) |
| 2026-10-22 03:50 | `7ee5019` | merge: feature/project-scaffold into develop (no-ff) | `develop` | [기록](commits/261022_0350_7ee5019_merge-project-scaffold_develop.md) |
| 2026-10-22 03:44 | `7059f16` | docs(docs): 첫 구현에 맞춰 스택·API·아키텍처 문서 갱신 | `feature/web-foundation` | [기록](commits/261022_0344_7059f16_web-foundation-docs_feature-web-foundation.md) |
| 2026-10-22 03:44 | `fc3cbb9` | feat(ai): FastAPI AI 파이프라인 기초 구현 | `feature/web-foundation` | [기록](commits/261022_0344_fc3cbb9_web-foundation-ai_feature-web-foundation.md) |
| 2026-10-22 03:44 | `5349f0a` | feat(api): Spring Boot 기초 API와 파이프라인 상태 머신 구현 | `feature/web-foundation` | [기록](commits/261022_0344_5349f0a_web-foundation-backend_feature-web-foundation.md) |
| 2026-10-22 03:43 | `c21d882` | feat(web): Angular 기초 화면 구현 | `feature/web-foundation` | [기록](commits/261022_0343_c21d882_web-foundation-frontend_feature-web-foundation.md) |
| 2026-10-22 03:14 | `1bd8e86` | chore(docs): 프로젝트 뼈대와 문서 규칙 구성 | `feature/project-scaffold` | [기록](commits/261022_0314_1bd8e86_project-scaffold_feature-project-scaffold.md) |
| 2026-10-22 03:13 | `63740e1` | chore(repo): 저장소 초기화 | `main` | [기록](commits/261022_0313_63740e1_repo-init_main.md) |
