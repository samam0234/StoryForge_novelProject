# chore(docs): 프로젝트 뼈대와 문서 규칙 구성

## 제목 / 커밋번호

- **제목**: `chore(docs): 프로젝트 뼈대와 문서 규칙 구성`
- **커밋번호**: `1bd8e86`
- **브랜치**: `feature/project-scaffold`
- **일시**: 2026-10-22 03:14

## 주 내용

기획서 v1.2 §8 폴더 구조와 정본 docs 규칙에 따라 저장소 뼈대, 문서 체계, 에이전트 어댑터를 만들었습니다.

## 상세

- 규칙 정본을 `.agents/skills/storyforge/SKILL.md`로 옮기고 "폴더 README 규칙"을 추가했습니다 (v1.0 → v1.1, 사용자 요청)
- 루트 `README.md`, `AGENTS.md`, `.gitignore`를 추가했습니다
- `.claude/`, `.cursor/`, `.codex/`, `.grok/`, `.antigravity/` 어댑터를 추가했습니다
- `docs/` 10개 폴더 README, `plan/CURRENT_STACK.md`, `Architecture/` 문서 2개, `branchs/TEMPLATE.md`를 추가했습니다
- `storyforge-frontend/`, `storyforge-backend/`, `storyforge-ai/` 폴더 구조와 폴더별 README를 추가했습니다 (기획서 §8.1~8.3)
- 기획서와 정본 요약이 다른 Flyway 경로는 기획서(`src/main/resources/db/migration/`)를 따랐습니다

## 결과

- 모든 폴더에 README.md가 있는지, 문서 사이 상대 링크가 깨지지 않았는지 스크립트로 확인했습니다 (누락 0, 깨진 링크 0)
- 기획서에 없는 항목(포트, 버전, `service/outline/`, 챕터 반복 전이 등)은 `미정`·확인 필요로 남겼습니다
