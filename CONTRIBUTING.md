# 기여 안내

StoryForge는 [독점 라이선스](LICENSE)로 공개된 저장소입니다. **기여를 보내기 전에 저장소 소유자의 허락이 필요하며**, 허락을 받은 기여는 이 저장소의 라이선스에 따라 StoryForge Project Team에 귀속됩니다.

작업 규칙의 정본은 [.agents/skills/storyforge/SKILL.md](.agents/skills/storyforge/SKILL.md)이고, 아래는 그 요약입니다. 규칙이 어긋나면 정본이 이깁니다.

## 시작하기

1. [README.md](README.md)의 "빠른 실행"으로 전체 스택을 띄웁니다. 막히면 [docs/guidance/local-dev-setup.md](docs/guidance/local-dev-setup.md)를 보세요.
2. **기획 원본**([StoryForge_기획서_v1.2.md](docs/plan/Proposal/StoryForge_기획서_v1.2.md))을 먼저 읽습니다. 설계 의도, 파이프라인 단계, 상태 머신은 임의로 바꾸지 않습니다. 충돌하는 결정이 필요하면 먼저 묻습니다.

## 브랜치와 병합

| 규칙 | 내용 |
|------|------|
| 작업 브랜치 | 사용자가 정해 준 브랜치만 씁니다 (현재 `feature/auth` 1개). **임의로 새로 만들지 않습니다** |
| 직접 커밋 | `develop`, `main`에는 하지 않습니다 |
| 병합 | `git merge --no-ff`만 씁니다. 흐름은 `feature → develop → main` |
| `main` | **저장소 소유자의 직접 명령이 있을 때만** 반영합니다 |
| 금지 | fast-forward 병합, `develop` 없이 `main` 직행, force-push(명시적 요청 제외), `.env` 커밋 |

## 커밋 메시지

```
type(scope): 한국어 요약
```

- `type`과 `scope`는 영어(`feat`, `fix`, `docs`, `test`, `chore` / `web`, `api`, `ai`, `docs` 등), 요약·본문은 한국어입니다.
- 제목 끝에 마침표를 찍지 않고, `type(scope):` 뒤에 한 칸을 둡니다.
- 서비스가 다른 변경은 커밋을 나눕니다. 병합 메시지는 `merge: feature/xxx into develop (no-ff)`입니다.
- 커밋 기록(`docs/branchs/commits/`)은 필요할 때만 남깁니다.

## 코드 규칙

- 호출 방향은 **Angular → Spring Boot → FastAPI**뿐입니다. Angular가 FastAPI를 직접 호출하지 않습니다.
- LLM 호출과 Continuity 검사는 FastAPI에서만 합니다. 상태 전이는 Spring Boot(`PipelineStateMachine`)에서만 합니다.
- 챕터 생성에는 Story Bible + Outline + 이전 챕터 요약을 반드시 넣고, Continuity 결과를 임의로 통과시키지 않으며, 사용자 검토(`CHAPTER_REVIEW`)를 건너뛰지 않습니다.
- 포트, 버전, 환경 변수의 기준값은 [docs/plan/CURRENT_STACK.md](docs/plan/CURRENT_STACK.md)입니다. 코드에 하드코딩하지 않습니다.
- 새 엔드포인트는 소유자 검사(경로 변수 `{projectId}`, `{chapterId}`)를 따르고, 다른 사용자 404 테스트를 함께 추가합니다. 공개 작품·게시글을 고치는 API는 서비스에서 작성자를 확인합니다.
- 작품·글을 목록에 내보내는 쿼리는 숨김(`hidden_at`)과 차단을 함께 거릅니다. 다른 사람에게 보이는 응답에 이메일을 넣지 않습니다.
- 비밀 값(API 키, JWT 키, 비밀번호)을 커밋하지 않습니다. [SECURITY.md](SECURITY.md)를 보세요.

## 테스트

변경한 서비스의 테스트를 돌리고, 전부 통과한 상태에서 올립니다.

| 서비스 | 명령 |
|--------|------|
| 백엔드 | `cd storyforge-backend && ./mvnw test` (Docker 필요. 건너뜀이 없는지 확인) |
| AI | `cd storyforge-ai && .venv/Scripts/python -m pytest -q` |
| 운영 콘솔 | `cd storyforge-console && npm test && npm run build` |
| 프론트 | `cd storyforge-frontend && npx ng test --watch=false && npx ng build` |

**프론트는 `ng build`까지 확인합니다.** 단위 테스트는 spec이 가져오는 파일만 컴파일하므로 화면 템플릿 오류를 놓칠 수 있습니다. 화면을 바꿨다면 브라우저에서 직접 눌러 확인합니다. 실제 Claude API를 호출하는 테스트는 기본 테스트에 넣지 않습니다.

## 문서

- 구현을 바꾸면 [docs/Architecture/](docs/Architecture/)와 [CURRENT_STACK.md](docs/plan/CURRENT_STACK.md)를 함께 갱신합니다.
- 기능이나 진행 방식을 바꾸면 **사유와 바뀐 방식**을 문서에 남깁니다 ([roadmap-progress.md](docs/plan/roadmap-progress.md)의 변경 이력, 디버그는 [find_debug/](docs/find_debug/)).
- README는 필요한 곳(서비스 루트, 하위 기능을 묶는 폴더, 핵심 폴더)에만 둡니다. 파일이 한두 개뿐인 폴더에는 만들지 않고 상위 README의 표에 적습니다.
- 사용자 인터페이스와 문서는 한국어입니다.

## PR을 만들 때

[PR 템플릿](.github/PULL_REQUEST_TEMPLATE.md)의 항목을 채웁니다. 변경 이유, 확인한 방법, 갱신한 문서를 적어 주세요.
