# StoryForge 규칙 (Grok 어댑터)

> 이 파일은 **어댑터**입니다. 규칙 정본은 `.agents/skills/storyforge/SKILL.md`이며, 내용이 어긋나면 정본이 이깁니다.
> 이 파일만 고치지 말고, 정본 → 루트 `AGENTS.md` → 어댑터 순서로 동기화합니다.

작업을 시작하기 전에 다음 문서를 읽고 따릅니다.

1. `StoryForge_기획서_v1.2.md` — 기획 원본 (최우선)
2. `.agents/skills/storyforge/SKILL.md` — 규칙 정본
3. `AGENTS.md` — 정본 요약과 체크리스트

빠르게 확인할 핵심 원칙 (상세는 정본):

- 기획 원본의 설계 의도·워딩·파이프라인·상태 머신을 임의로 바꾸지 않습니다. 충돌하면 먼저 사용자에게 묻습니다.
- 작업과 커밋은 `feature/*`, `bugfix/*`, `experimental/*`에서만 합니다. 병합은 `--no-ff`로, 사용자가 요청할 때만 합니다.
- 커밋마다 `docs/branchs/commits/`에 기록 파일을 만들고 `docs/branchs/README.md` 목록에 추가합니다.
- Angular는 FastAPI를 직접 호출하지 않습니다. 모든 요청은 Spring Boot를 거칩니다.
- 모든 폴더에 `README.md`를 둡니다.
