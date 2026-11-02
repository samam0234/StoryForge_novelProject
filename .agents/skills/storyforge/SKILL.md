---
name: storyforge
description: StoryForge 프로젝트의 에이전트 작업 규칙 정본. 기획 원본 보존, 커밋/브랜치 규칙, 커밋 기록, 서비스 호출 규칙, 상태 머신, docs 폴더 규칙을 다룬다. StoryForge 저장소에서 코드·문서·커밋 작업을 할 때 항상 따른다.
---

# StoryForge 에이전트 규칙 (정본)

> **정본**: 이 문서가 에이전트 규칙의 유일한 정본입니다.  
> 위치: `.agents/skills/storyforge/SKILL.md`  
> 어댑터(`.claude/`, `.cursor/`, `.codex/`, `.grok/`, `.antigravity/` 등)는 이 문서를 가리키는 복제본입니다.  
> 내용이 어긋나면 **정본이 이깁니다**.

규칙을 수정할 때는 이 정본과 루트 `AGENTS.md`를 먼저 고치고, 어댑터를 그 뒤에 맞춥니다.

자동 생성 파일은 작업 규칙이 아니므로 손으로 고치지 않습니다.

---

## 충돌 시 우선순위

1. 루트 기획 원본 (`StoryForge_기획서_v1.2.md` 또는 최신 버전)
2. 이 정본 `SKILL.md`
3. 루트 `AGENTS.md`
4. `commit-message.md`
5. `branch-merge.md`
6. `TEMPLATE.md`
7. 도구별 어댑터

---

## §0 원본 보존 원칙 (최상위, 예외 없음)

- 기획 원본의 **설계 의도, 문단 구조, 워딩, 세계관 설정 원칙, 파이프라인 단계, 상태 머신**을 임의로 수정·삭제·요약하지 않습니다.
- 오타 교정도 **사용자 확인 후**에 합니다.
- 원본과 충돌하는 결정이 필요하면 **먼저 사용자에게 묻습니다**.
  - 승인되면 문서 버전을 올리고 변경 이력을 남기거나, 해당 절 아래에 내용을 추가하는 방식으로만 반영합니다.
- Story Bible, Outline, Continuity 관련 파생 산출물에도 같은 원칙을 적용합니다. 요청받은 범위만 고칩니다.
- 이 원칙은 브랜치, 커밋, docs 규칙과 충돌해도 **항상 우선**합니다.
- **설정 붕괴(할루시네이션, 캐릭터 OOC, 타임라인 오류)**를 유발하는 변경은 특별히 금지합니다.

---

## 커밋 메시지

- `type`과 `scope`는 **영어**입니다.  
  예: `feat`, `fix`, `docs`, `web`, `api`, `ai`, `bible`, `chapter` 등
- 제목 요약, 본문, 바닥글은 **한국어**입니다.
- 제목 끝에 마침표를 찍지 않습니다.
- `type(scope):` 뒤에 **한 칸**을 둡니다.
- 병합 메시지는 다음 형식을 따릅니다.  
  `merge: feature/xxx into develop (no-ff)`

### 커밋 기록 (필요할 때만)

- **평소에는 작성하지 않습니다.** 사용자가 요청하거나 따로 남길 필요가 있을 때만 작성합니다. (v1.2 변경)
- 작성할 때는 `docs/branchs/commits/`에 기록 파일을 만들고,  
  `docs/branchs/README.md` 목록 표에도 링크를 추가합니다.  
  **둘 다 같은 브랜치에 커밋**합니다.
- 파일명 형식:  
  `YYMMDD_HHMM_[커밋ID]_[커밋이름]_[브랜치].md`  
  예: `261022_0230_a1b2c3d_chapter-continuity_feature-ai.md`
- `HHMM`은 한 덩어리로 쓰고, 브랜치의 `/`는 `-`로 바꿉니다.
- 본문은 `TEMPLATE.md`를 따릅니다.  
  섹션: **제목/커밋번호**, **주 내용**, **상세**, **결과**

---

## 브랜치와 병합

- 작업과 커밋은 `feature/*`, `bugfix/*`, `experimental/*`에서만 합니다.
- `develop`과 `main`은 **통합용**입니다.
- 병합은 `git merge --no-ff`만 사용합니다.
- **브랜치를 임의로 만들지 않습니다.** 사용자가 정해 준 브랜치만 씁니다. `feature/*` 형식이라도 새로 만들려면 먼저 확인합니다. (v1.2 추가)
  - 현재 지정 브랜치: `main`, `develop`, 작업 브랜치 `feature/auth` **1개**
- **`main` 병합·반영은 사용자의 직접 명령이 있을 때만** 합니다. 임의 반영이나 일방적인 반영은 금지합니다. (v1.2 추가)

### 금지 사항

- fast-forward 병합
- `develop` 없이 `main`에 직행
- 일상 기능을 `develop`에 직접 커밋
- feature끼리 장기 병합
- force-push (명시적 요청 제외)
- `.env` 시크릿 커밋

---

## 스택과 범위 (기획서 v1.2 기준)

| 영역 | 기술 | 역할 |
|------|------|------|
| Frontend | Angular 18+ (`storyforge-frontend/`) | SPA, Material UI, TipTap, RxJS |
| Orchestrator | Spring Boot 4.x (`storyforge-backend/`) | 인증, CRUD, 상태 머신, WebClient, Flyway |
| AI Service | FastAPI (`storyforge-ai/`) | 프롬프트 조립, LLM 스트리밍, Continuity 검증 |
| Database | PostgreSQL 16+ | 영속화 (pgvector 확장 검토) |

### 호출 및 책임 규칙

- **Angular는 FastAPI를 직접 호출하지 않습니다.** 모든 요청은 Spring Boot를 통합니다.
- FastAPI는 **내부망 전용**입니다. (서비스 간 통신만)
- Spring Boot가 파이프라인 상태 머신과 오케스트레이션을 담당합니다.
- 무거운 추론(LLM 호출, Continuity Check)은 FastAPI에서만 수행합니다.
- Angular Route / Component에 LLM 호출 로직을 넣지 않습니다.

### 상태 머신 (프로젝트 단위, 임의 변경 금지)

```
BIBLE_DRAFT → BIBLE_CONFIRMED → OUTLINE_DRAFT → OUTLINE_CONFIRMED
→ CHAPTER_GENERATING → CHAPTER_REVIEW → COMPLETED
```

기획서 v1.3 추가 전이: `CHAPTER_REVIEW → CHAPTER_GENERATING`(다음 챕터·재생성), `CHAPTER_GENERATING → OUTLINE_CONFIRMED`(첫 챕터 실패·중단 복귀)

### v1 / MVP 범위 준수

- Must: Story Bible, Outline, 순차 챕터 생성, 기본 Continuity, 에디터, 인증
- Phase 1에서 임의로 추가하지 말 것: 고급 벡터 검색, 협업, 문체 분석, 이미지 생성 연동
- 기획서 Phase 1~3 로드맵을 존중합니다.

포트와 인프라 확정값은 코드에서 정하지 않고 `docs/plan/CURRENT_STACK.md`에서 관리합니다.

---

## 권장 폴더 구조 (기획서 준수)

### Angular (`storyforge-frontend/`)
```
src/app/
├── core/          # auth, api, models
├── shared/        # components, ui
├── features/      # projects, story-bible, outline, chapters, dashboard
├── layout/
└── app.routes.ts
```

### Spring Boot (`storyforge-backend/`)
```
src/main/java/com/storyforge/
├── controller/
├── service/orchestration/   # 상태 머신
├── domain/                  # project, bible, outline, chapter
├── client/                  # FastAPI WebClient
├── config/
└── db/migration/            # Flyway
```

### FastAPI (`storyforge-ai/`)
```
app/
├── api/v1/
├── core/llm.py
├── services/                # bible_generator, outline_generator, chapter_generator, continuity_checker
├── prompts/
├── models/
└── main.py
```

---

## docs 규칙

진입점: `docs/README.md`

| 폴더 | 용도 / 규칙 |
|------|-------------|
| `plan/` | 기획서에서 파생된 개발 스펙. 원본과 모순되면 원본이 우선하고 여기를 고칩니다. |
| `Architecture/` | 시스템 구조, 오케스트레이션 흐름, 상태 머신. 구현이 바뀌면 여기를 먼저 갱신. ASCII 또는 mermaid 권장. |
| `branchs/` | 브랜치·커밋 기록. `commits/`, `TEMPLATE.md`, `README.md` 목록이 필수. |
| `guidance/` | 실행·기능 가이드. 전제 → 명령 → 기대결과 순서. 포트와 ENV는 표로. |
| `find_debug/` | `YYYY-MM-DD.md` 디버그 일지. 증상, 원인, 해결, 재발방지. |
| `repeater/` | 재발 가능한 인프라 이슈. 환경, 증상, 원인, 해결, 검증 명령. |
| `vaildates/` | 배포·데모 전 수동 검증 체크리스트. **폴더명 스펠링은 의도적이므로 교정하지 않습니다.** |
| `web_management/` | 배포 전/중/후 문서와 포트 인벤토리. |
| `trainings/` | 설계 판단과 연구 노트. "왜 그렇게 정했는지"만 작성. 코드, 프롬프트 원문, 실유저 데이터, 생성 소설 원문은 넣지 않습니다. |
| `novel/` | 세계관 템플릿, 프롬프트 가이드, Continuity 규칙 등 창작 관련 문서. |

### docs에서 하지 말 것

- `docs/commits/`에 기록하지 않습니다. `docs/branchs/commits/`만 사용합니다.
- 구현을 바꾸면서 `Architecture/`나 `CURRENT_STACK.md`를 갱신하지 않는 일이 없게 합니다.
- 기획 원본을 `docs/` 안으로 옮기거나 이름을 바꾸지 않습니다.
- 새 하위 폴더가 필요하면 **정본의 표에 용도를 추가한 뒤** 만듭니다.

### 변경 사유 문서화 (v1.2 추가)

- 기능이 바뀌거나, 문제가 생겨 진행 방식을 바꾸면 **사유와 바뀐 방식을 문서에 남깁니다.**
- 위치: 구조·흐름은 `Architecture/`, 스펙과 진행 현황은 `plan/`, 선택 이유는 `trainings/`, 문제 원인은 `find_debug/`.

### 폴더 README 규칙 (v1.3 변경: "모든 폴더"에서 "필요한 곳만"으로)

README는 **필요한 곳에만** 둡니다. 파일이 한두 개뿐이라 없어도 되는 폴더에는 두지 않습니다.

README를 두는 곳:

- 저장소 루트, 각 서비스 루트 (`storyforge-frontend/`, `storyforge-backend/`, `storyforge-ai/`)
- 서비스 안에서 **여러 하위 기능을 묶는 폴더**와 **규칙이 있는 핵심 폴더** (예: 상태 머신, 보안 설정, 마이그레이션, 프롬프트)
- `docs/`와 `docs/` 아래 최상위 폴더 (폴더 정의와 작성 템플릿이 들어 있어서)

README를 두지 않는 곳: 파일이 한두 개뿐인 폴더, 패키지 경로용 빈 중간 폴더(`src/main/java/com/` 등), 자동 생성 폴더(`node_modules/`, `build/`, `dist/`), `docs/branchs/commits/`의 개별 기록.

- README에는 **목적, 들어갈 것, 들어가지 않을 것, 관련 문서**를 적습니다. 상위 README가 하위 폴더의 파일 표를 함께 적을 수 있습니다.
- 폴더 구조나 용도를 바꾸면 해당 README를 같은 커밋에서 갱신합니다.
- README가 없는 폴더의 설명은 가장 가까운 상위 README에 적습니다.

---

## 이 세션 추가 규칙

### 메모리 (피드백)

- 작업별 브랜치에 커밋하고, **병합은 사용자가 말할 때만** 합니다.
- 개발 서버가 켜져 있을 때는 빌드 명령으로 실행 중인 프로세스를 죽이지 않도록 주의합니다.

### Continuity & 품질 원칙

- Story Bible + Outline + 이전 챕터 요약을 컨텍스트로 반드시 주입합니다.
- Continuity Checker 결과를 무시하고 임의로 통과시키지 않습니다.
- 사용자 검토(CHAPTER_REVIEW) 단계를 건너뛰지 않습니다.

### 커밋 트레일러

필요 시 도구 표기를 바닥글에 추가할 수 있습니다 (정본 한국어 규칙과 별개).

---

## 어댑터 규칙

`.claude/`, `.cursor/`, `.codex/`, `.grok/`, `.antigravity/` 등의 파일은 이 정본을 가리키는 복제본입니다.

- 정본을 수정한 후 어댑터를 동기화합니다.
- 어댑터만 수정하고 정본을 그대로 두지 않습니다.
- 충돌 시 항상 정본이 우선합니다.

---

**문서 버전**: 1.3 (1.2 → 1.3: 폴더 README를 모든 폴더에서 필요한 곳만으로 변경. 1.1 → 1.2: 커밋 기록은 필요할 때만, 브랜치·main 병합 제한, 변경 사유 문서화, 기획서 v1.3 반영)  
**최종 갱신**: 2026-11-03  
**상태**: 정본 (Canonical)  
**프로젝트**: StoryForge  
**기준 문서**: StoryForge_기획서_v1.2.md (문서 버전 1.3, 파일명은 유지)  
**스택**: Angular + Spring Boot + FastAPI + PostgreSQL
