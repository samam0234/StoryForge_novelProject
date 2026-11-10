# AGENTS.md — StoryForge 작업 규칙 요약

> 이 문서는 정본 [.agents/skills/storyforge/SKILL.md](.agents/skills/storyforge/SKILL.md)의 요약입니다.
> 내용이 어긋나면 **정본이 이깁니다**. 규칙을 고칠 때는 정본과 이 문서를 먼저 고치고, 어댑터는 그 뒤에 맞춥니다.

## 우선순위

1. 기획 원본 [StoryForge_기획서_v1.2.md](docs/plan/Proposal/StoryForge_기획서_v1.2.md) (또는 최신 버전)
2. 정본 `SKILL.md`
3. 이 문서 `AGENTS.md`
4. [commit-message.md](.agents/skills/storyforge/commit-message.md)
5. [branch-merge.md](.agents/skills/storyforge/branch-merge.md)
6. [TEMPLATE.md](docs/branchs/TEMPLATE.md)
7. 도구별 어댑터 (`.claude/`, `.cursor/`, `.codex/`, `.grok/`, `.antigravity/`, `.Manus/`, `.Gemini/`, `.Kimi/`, `.Ollama/`, `.Qwen/`, `.Opencode/`, `.Windsurf/`)

## 작업 전 체크리스트

- [ ] 기획 원본의 설계 의도, 워딩, 파이프라인 단계, 상태 머신을 바꾸지 않는가? (정본 §0)
- [ ] 원본과 충돌하는 결정이면 **먼저 사용자에게 물었는가?**
- [ ] `feature/*`, `bugfix/*`, `experimental/*` 브랜치에서 작업하는가? (`develop`, `main`에 직접 커밋 금지)
- [ ] MVP(Phase 1) 범위를 벗어나는 기능을 임의로 넣지 않는가?

## 완료 전 체크리스트

- [ ] 변경한 서비스의 테스트를 돌렸는가?
- [ ] 프론트를 바꿨다면 `ng test`와 **`ng build`** 를 모두 확인했는가? (단위 테스트는 화면 템플릿 오류를 놓칠 수 있습니다)
- [ ] 화면을 바꿨다면 브라우저에서 확인했는가?

## 커밋할 때

- 형식: `type(scope): 한국어 요약` — type/scope는 영어, 요약·본문·바닥글은 한국어, 끝에 마침표 없음
- 커밋 기록(`docs/branchs/commits/`)은 **평소에는 작성하지 않고 필요할 때만** 작성합니다. 형식은 [TEMPLATE.md](docs/branchs/TEMPLATE.md)를 따릅니다.
- `.env` 시크릿은 커밋하지 않습니다.

## 병합할 때

- `git merge --no-ff`만 사용합니다. 메시지: `merge: feature/xxx into develop (no-ff)`
- `develop`을 거치지 않고 `main`에 병합하지 않습니다.
- **`main` 병합·반영은 사용자의 직접 명령이 있을 때만** 합니다.
- **브랜치를 임의로 만들지 않습니다.** 현재 지정 브랜치는 `main`, `develop`, 작업 브랜치 `feature/auth` 1개입니다.
- **병합은 사용자가 요청할 때만** 합니다.
- force-push는 명시적 요청이 있을 때만 합니다.

## 코드 구조 규칙

- Angular → Spring Boot → FastAPI 순서로만 호출합니다. **Angular는 FastAPI를 직접 호출하지 않습니다.**
- FastAPI는 내부망 전용입니다.
- 파이프라인 상태 머신과 오케스트레이션은 Spring Boot가 맡습니다.
- LLM 호출과 Continuity Check는 FastAPI에서만 합니다. Angular 컴포넌트에 LLM 로직을 넣지 않습니다.
- 상태 머신은 임의로 바꾸지 않습니다.
  `BIBLE_DRAFT → BIBLE_CONFIRMED → OUTLINE_DRAFT → OUTLINE_CONFIRMED → CHAPTER_GENERATING → CHAPTER_REVIEW → COMPLETED`
  (기획서 v1.3 추가 전이: `CHAPTER_REVIEW → CHAPTER_GENERATING`, `CHAPTER_GENERATING → OUTLINE_CONFIRMED`)
- 포트와 인프라 값은 코드에서 정하지 않고 [docs/plan/CURRENT_STACK.md](docs/plan/CURRENT_STACK.md)에서 관리합니다.
- 폴더 구조는 기획서 §8을 따릅니다. 정본의 "권장 폴더 구조"와 다르면 기획서가 우선합니다.

## 보조 실행 도구 배치

- 서비스 루트에는 필요한 설정·의존성 파일·문서·필수 진입점만 둡니다.
- 성능 측정·수동 검증·복구 도구는 서비스별 `scripts/` 아래에 둡니다. 자동 회귀 테스트는 표준 테스트 경로에 둡니다.
- Python 도구는 서비스 폴더에서 `python -m scripts.<도구>`로 실행하며, 이동 시 import·데이터 경로·테스트·정적 검사·문서를 함께 갱신합니다. 루트 실행 래퍼는 남기지 않습니다.

## 문서 규칙

- 진입점은 [docs/README.md](docs/README.md)입니다. 폴더별 용도는 그 문서와 정본의 표를 따릅니다.
- 구현을 바꾸면 `docs/Architecture/`와 `CURRENT_STACK.md`를 함께 갱신합니다.
- 기능이 바뀌거나 문제가 생겨 진행 방식을 바꾸면 **사유와 바뀐 방식을 문서에 남깁니다.**
- 기획 원본을 `docs/` 안으로 옮기거나 이름을 바꾸지 않습니다.
- **README는 필요한 곳에만 둡니다** (서비스 루트, 하위 기능을 묶는 폴더, 규칙이 있는 핵심 폴더, `docs/` 최상위 폴더). 파일이 한두 개뿐인 폴더에는 두지 않고 상위 README에 적습니다. 상세는 정본 "폴더 README 규칙".
- `docs/vaildates/`의 스펠링은 의도적이므로 교정하지 않습니다.

## Continuity & 품질

- 챕터 생성 시 Story Bible + Outline + 이전 챕터 요약을 반드시 컨텍스트로 주입합니다.
- Continuity Checker 결과를 무시하고 통과시키지 않습니다.
- 사용자 검토(`CHAPTER_REVIEW`) 단계를 건너뛰지 않습니다.
