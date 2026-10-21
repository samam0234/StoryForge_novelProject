# .agents/

에이전트(사람 기여자 포함)가 따르는 **작업 규칙의 정본**이 있는 폴더입니다.

## 구성

| 경로 | 내용 |
|------|------|
| [skills/storyforge/SKILL.md](skills/storyforge/SKILL.md) | 규칙 정본. 모든 어댑터의 기준 |
| [skills/storyforge/commit-message.md](skills/storyforge/commit-message.md) | 커밋 메시지·커밋 기록 상세 |
| [skills/storyforge/branch-merge.md](skills/storyforge/branch-merge.md) | 브랜치 전략·병합 절차 상세 |

## 어댑터와의 관계

도구마다 규칙을 읽는 위치가 다르므로, 각 도구 폴더에 정본을 가리키는 어댑터를 둡니다.

| 도구 | 어댑터 파일 |
|------|-------------|
| Claude Code | `.claude/CLAUDE.md`, `.claude/skills/storyforge/SKILL.md` |
| Cursor | `.cursor/rules/storyforge.mdc` |
| Codex | 루트 `AGENTS.md`(자동 인식), `.codex/instructions.md` |
| Grok | `.grok/rules.md` |
| Antigravity | `.antigravity/rules.md` |

어댑터 폴더마다 `README.md`가 있어 그 폴더에 무엇이 있는지 설명합니다.

## 수정 순서

1. 정본 `SKILL.md`를 고칩니다. (필요하면 문서 버전을 올립니다.)
2. 루트 `AGENTS.md` 요약을 맞춥니다.
3. 어댑터를 동기화합니다. 어댑터만 고치고 정본을 그대로 두지 않습니다.

## 들어가지 않을 것

- 기획 원본 (루트에 그대로 둡니다)
- 도구 고유 설정 파일 (`settings.json` 등은 각 도구 폴더에)
