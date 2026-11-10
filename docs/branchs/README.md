# docs/branchs/

브랜치 목록과 커밋 기록을 두는 폴더입니다. 커밋 기록은 **평소에는 작성하지 않고 필요할 때만** 작성합니다.

## 구성

| 경로 | 내용 |
|------|------|
| [TEMPLATE.md](TEMPLATE.md) | 커밋 기록 파일 템플릿 |
| [commits/](commits/) | 커밋 기록 파일 |
| `README.md` (이 문서) | 브랜치 목록, 커밋 기록 목록 |

## 기록 절차 (필요할 때만)

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
| `feature/diagnostics-readme` | VS Code 경고 정리, README 실행 가이드 개편 | `develop` | 2026-10-23 | develop 병합 |
| `feature/auth` | **지정 작업 브랜치 (1개).** 인증, SMTP 계정 메일, 창작·커뮤니티·운영 콘솔, Ollama·복구·코드 진단과 문서 | `develop` | 2026-11-02 | develop 병합 |

상태: `예정` / `진행 중` / `develop 병합` / `main 병합` / `폐기`

**브랜치를 임의로 만들지 않습니다.** 기록 전용으로 만들었던 `feature/merge-records*` 3개는 2026-11-02에 삭제했습니다.

2026-11-10 최신 사용자 명령은 진단·문서 수정 후 커밋 → `feature/auth → develop → main`의 `--no-ff` 병합 → 푸시입니다. 이전 브라우저 검증 전 보류를 변경하며 실제 화면 검증은 미완료로 남깁니다. 최신 통합 커밋은 `git log --first-parent develop`과 `git log --first-parent main`에서 확인하고, [진단·검증 범위](../vaildates/diagnostics-integration.md)를 함께 봅니다.

## 커밋 기록 목록

최신 기록을 위에 추가합니다.

| 일시 | 커밋 ID | 커밋 이름 | 브랜치 | 기록 |
|------|---------|-----------|--------|------|
| 2026-11-04 20:55 | `377d6fe` | fix(api): VS Code Java 경고 정리 | `feature/auth` | [기록](commits/261104_2055_377d6fe_java-warnings_feature-auth.md) |
| 2026-11-04 20:52 | `0770835` | docs(docs): 재검증, 신고·관리, 패키지 검사를 반영해 문서 전체 갱신 | `feature/auth` | [기록](commits/261104_2052_0770835_docs-overhaul_feature-auth.md) |
| 2026-11-04 20:44 | `dad7fd2` | feat(web): 알림, 신고, 관리자, 이용약관, 차단, 패키지 검사, 원본 가져오기 화면 추가 | `feature/auth` | [기록](commits/261104_2044_dad7fd2_moderation-web_feature-auth.md) |
| 2026-11-04 20:40 | `edbc036` | fix(web): 창작하기 화면의 남는 닫는 태그로 빌드가 깨지던 문제 수정 | `feature/auth` | [기록](commits/261104_2040_edbc036_fix-create-page-build_feature-auth.md) |
| 2026-11-04 20:34 | `79f61af` | feat(api): 패키지 전체 Continuity 검사와 원본 변경 가져오기 추가 | `feature/auth` | [기록](commits/261104_2034_79f61af_package-continuity_feature-auth.md) |
| 2026-11-04 20:29 | `1c540d8` | feat(api): 신고, 관리자 조치, 차단, 계정 정지, 이용약관, 알림 추가 | `feature/auth` | [기록](commits/261104_2029_1c540d8_moderation-api_feature-auth.md) |
| 2026-11-04 20:21 | `7ba9a61` | test(ai): 콜라보 안내 테스트를 버튼 구성 변경에 맞춤 | `feature/auth` | [기록](commits/261104_2021_7ba9a61_collab-guide-test_feature-auth.md) |
| 2026-11-04 20:21 | `3020429` | feat(api): 고친 원고를 승인할 때 다시 요약·검증 | `feature/auth` | [기록](commits/261104_2021_3020429_chapter-review_feature-auth.md) |
| 2026-11-03 10:46 | `c0f74ab` | merge: feature/auth into develop (no-ff) | `develop` | [기록](commits/261103_1046_c0f74ab_merge-collaboration_develop.md) |
| 2026-11-03 07:13 | `f6dd5b8` | feat(api): 함께 창작 초대와 작업 커밋을 추가 | `feature/auth` | [기록](commits/261103_0713_f6dd5b8_collaboration-api_feature-auth.md) |
| 2026-11-03 07:13 | `538cb59` | feat(web): 작업 기록표와 함께 창작 화면을 추가 | `feature/auth` | [기록](commits/261103_0713_538cb59_collaboration-web_feature-auth.md) |
| 2026-11-03 07:13 | `07b704c` | docs(docs): 함께 창작 스펙과 에이전트 어댑터를 맞춤 | `feature/auth` | [기록](commits/261103_0713_07b704c_collaboration-docs_feature-auth.md) |
| 2026-11-03 06:40 | `c373e57` | docs(docs): 커뮤니티 스펙과 변경 사유 문서화 | `feature/auth` | [기록](commits/261103_0640_c373e57_community-docs_feature-auth.md) |
| 2026-11-03 06:40 | `bf640bf` | feat(web): 게시판, 작업·공유 그래프, 패키지포지, 프로필, 가이드챗 화면 추가 | `feature/auth` | [기록](commits/261103_0640_bf640bf_community-web_feature-auth.md) |
| 2026-11-03 06:40 | `4c209c7` | feat(ai): AI 가이드챗 추가 | `feature/auth` | [기록](commits/261103_0640_4c209c7_guide-chat_feature-auth.md) |
| 2026-11-03 06:40 | `4580148` | merge: feature/auth into develop (no-ff) | `develop` | [기록](commits/261103_0640_4580148_merge-community_develop.md) |
| 2026-11-03 06:40 | `40d1406` | feat(api): 게시판, Clone, 공유, 패키지포지, 작업 그래프, 프로필 추가 | `feature/auth` | [기록](commits/261103_0640_40d1406_community-api_feature-auth.md) |
| 2026-10-23 00:22 | `285f83e` | merge: feature/diagnostics-readme into develop (no-ff) | `develop` | [기록](commits/261023_0022_285f83e_merge-diagnostics-readme_develop.md) |
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
