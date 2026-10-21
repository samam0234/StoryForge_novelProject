# StoryForge

사용자 프롬프트로부터 **일관된 장편 소설**을 생성하는 AI 웹 플랫폼입니다.
핵심은 멀티 스테이지 파이프라인입니다.

```
Story Bible 생성/관리 → Outline 생성 → 순차 챕터 생성 + Continuity Tracking
```

각 단계는 사용자 검토와 AI 자동 검증을 함께 거칩니다. 자세한 내용은 기획 원본을 보세요.

## 먼저 읽을 문서

| 순서 | 문서 | 내용 |
|------|------|------|
| 1 | [StoryForge_기획서_v1.2.md](StoryForge_기획서_v1.2.md) | 기획 원본. 모든 결정의 최우선 기준 (`.docx`는 같은 내용의 배포본) |
| 2 | [.agents/skills/storyforge/SKILL.md](.agents/skills/storyforge/SKILL.md) | 에이전트·기여자 작업 규칙 **정본** |
| 3 | [AGENTS.md](AGENTS.md) | 정본 요약과 빠른 체크리스트 |
| 4 | [docs/README.md](docs/README.md) | 프로젝트 문서 진입점 |

## 저장소 구조

```
StoryForge/
├── StoryForge_기획서_v1.2.md   # 기획 원본 (이동·이름 변경 금지)
├── StoryForge_기획서_v1.2.docx
├── AGENTS.md                   # 작업 규칙 요약 (정본을 가리킴)
├── .agents/                    # 규칙 정본 (skills/storyforge/)
├── .claude/ .cursor/ .codex/ .grok/ .antigravity/   # 도구별 어댑터
├── docs/                       # 스펙, 아키텍처, 커밋 기록, 가이드 등
├── storyforge-frontend/        # Angular SPA
├── storyforge-backend/         # Spring Boot Orchestrator
└── storyforge-ai/              # FastAPI AI Service (내부망 전용)
```

모든 폴더에는 그 폴더를 소개하는 `README.md`가 있습니다.

## 서비스 구성

| 영역 | 기술 | 폴더 | 역할 |
|------|------|------|------|
| Frontend | Angular 18+ | [storyforge-frontend/](storyforge-frontend/) | SPA, Material UI, TipTap, RxJS |
| Orchestrator | Spring Boot 3.x | [storyforge-backend/](storyforge-backend/) | 인증, CRUD, 상태 머신, WebClient, Flyway |
| AI Service | FastAPI | [storyforge-ai/](storyforge-ai/) | 프롬프트 조립, LLM 스트리밍, Continuity 검증 |
| Database | PostgreSQL 16+ | — | 영속화 (pgvector 확장 검토) |

```
Angular ──REST/SSE──▶ Spring Boot ──내부 HTTP──▶ FastAPI ──▶ LLM Providers
                          │
                          ▼
                      PostgreSQL
```

Angular는 FastAPI를 직접 호출하지 않습니다. 모든 요청은 Spring Boot를 거칩니다.

## 현재 상태

- **뼈대 단계**: 기획서 §8의 폴더 구조와 폴더별 README만 있습니다.
- 각 서비스의 프레임워크 초기화(Angular CLI, Spring Initializr, Python 패키지 설정)는 아직 하지 않았습니다.
- 버전·포트·인프라 확정값은 [docs/plan/CURRENT_STACK.md](docs/plan/CURRENT_STACK.md)에서 관리합니다.

## 작업 흐름 요약

1. `develop`에서 `feature/*`, `bugfix/*`, `experimental/*` 브랜치를 만들어 작업합니다.
2. 커밋 메시지는 `type(scope): 한국어 요약` 형식입니다.
3. 커밋마다 [docs/branchs/commits/](docs/branchs/commits/)에 기록 파일을 남깁니다.
4. 병합은 `git merge --no-ff`로만 합니다. 흐름은 `feature → develop → main`입니다.

세부 규칙은 [AGENTS.md](AGENTS.md)를 보세요.
