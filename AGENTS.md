# AGENTS.md — StoryForge 작업 규칙 요약

> 이 문서는 정본 [.agents/skills/storyforge/SKILL.md](.agents/skills/storyforge/SKILL.md)의 요약입니다.
> 내용이 어긋나면 **정본이 이깁니다**. 규칙을 고칠 때는 정본과 이 문서를 먼저 고치고, 어댑터는 그 뒤에 맞춥니다.

## 우선순위

1. 기획 원본 [StoryForge_기획서_v1.2.md](StoryForge_기획서_v1.2.md) (또는 최신 버전)
2. 정본 `SKILL.md`
3. 이 문서 `AGENTS.md`
4. [commit-message.md](.agents/skills/storyforge/commit-message.md)
5. [branch-merge.md](.agents/skills/storyforge/branch-merge.md)
6. [TEMPLATE.md](docs/branchs/TEMPLATE.md)
7. 도구별 어댑터 (`.claude/`, `.cursor/`, `.codex/`, `.grok/`, `.antigravity/`)

## 작업 전 체크리스트

- [ ] 기획 원본의 설계 의도, 워딩, 파이프라인 단계, 상태 머신을 바꾸지 않는가? (정본 §0)
- [ ] 원본과 충돌하는 결정이면 **먼저 사용자에게 물었는가?**
- [ ] `feature/*`, `bugfix/*`, `experimental/*` 브랜치에서 작업하는가? (`develop`, `main`에 직접 커밋 금지)
- [ ] MVP(Phase 1) 범위를 벗어나는 기능을 임의로 넣지 않는가?

## 커밋할 때

- 형식: `type(scope): 한국어 요약` — type/scope는 영어, 요약·본문·바닥글은 한국어, 끝에 마침표 없음
- 커밋마다 `docs/branchs/commits/YYMMDD_HHMM_[커밋ID]_[커밋이름]_[브랜치].md`를 만들고
  [docs/branchs/README.md](docs/branchs/README.md) 목록 표에 링크를 추가합니다. 둘 다 같은 브랜치에 커밋합니다.
- merge나 작은 수정도 예외가 아닙니다.
- `.env` 시크릿은 커밋하지 않습니다.

## 병합할 때

- `git merge --no-ff`만 사용합니다. 메시지: `merge: feature/xxx into develop (no-ff)`
- `develop`을 거치지 않고 `main`에 병합하지 않습니다.
- **병합은 사용자가 요청할 때만** 합니다.
- force-push는 명시적 요청이 있을 때만 합니다.

## 코드 구조 규칙

- Angular → Spring Boot → FastAPI 순서로만 호출합니다. **Angular는 FastAPI를 직접 호출하지 않습니다.**
- FastAPI는 내부망 전용입니다.
- 파이프라인 상태 머신과 오케스트레이션은 Spring Boot가 맡습니다.
- LLM 호출과 Continuity Check는 FastAPI에서만 합니다. Angular 컴포넌트에 LLM 로직을 넣지 않습니다.
- 상태 머신은 임의로 바꾸지 않습니다.
  `BIBLE_DRAFT → BIBLE_CONFIRMED → OUTLINE_DRAFT → OUTLINE_CONFIRMED → CHAPTER_GENERATING → CHAPTER_REVIEW → COMPLETED`
- 포트와 인프라 값은 코드에서 정하지 않고 [docs/plan/CURRENT_STACK.md](docs/plan/CURRENT_STACK.md)에서 관리합니다.
- 폴더 구조는 기획서 §8을 따릅니다. 정본의 "권장 폴더 구조"와 다르면 기획서가 우선합니다.

## 문서 규칙

- 진입점은 [docs/README.md](docs/README.md)입니다. 폴더별 용도는 그 문서와 정본의 표를 따릅니다.
- 구현을 바꾸면 `docs/Architecture/`와 `CURRENT_STACK.md`를 함께 갱신합니다.
- 기획 원본을 `docs/` 안으로 옮기거나 이름을 바꾸지 않습니다.
- **모든 폴더에 `README.md`를 둡니다.** 목적, 들어갈 것, 들어가지 않을 것, 관련 문서를 적습니다.
- `docs/vaildates/`의 스펠링은 의도적이므로 교정하지 않습니다.

## Continuity & 품질

- 챕터 생성 시 Story Bible + Outline + 이전 챕터 요약을 반드시 컨텍스트로 주입합니다.
- Continuity Checker 결과를 무시하고 통과시키지 않습니다.
- 사용자 검토(`CHAPTER_REVIEW`) 단계를 건너뛰지 않습니다.
