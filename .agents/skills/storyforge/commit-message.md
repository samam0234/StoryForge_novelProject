# 커밋 메시지 규칙

> 정본 [SKILL.md](SKILL.md) "커밋 메시지" 절의 상세 문서입니다. 어긋나면 정본이 이깁니다.

## 형식

```
type(scope): 한국어 제목 요약

한국어 본문 (무엇을, 왜)

한국어 바닥글 (관련 문서, 이슈, 도구 트레일러 등)
```

- `type`과 `scope`는 **영어**, 제목·본문·바닥글은 **한국어**입니다.
- `type(scope):` 뒤에 **한 칸**을 둡니다.
- 제목 끝에 마침표를 찍지 않습니다.

## type 예시

| type | 쓰는 경우 |
|------|-----------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서만 변경 |
| `refactor` | 동작 변화 없는 구조 개선 |
| `test` | 테스트 추가·수정 |
| `chore` | 빌드, 설정, 의존성, 뼈대 |
| `merge` | 병합 커밋 |

## scope 예시

| scope | 대상 |
|-------|------|
| `web` | `storyforge-frontend/` |
| `api` | `storyforge-backend/` |
| `ai` | `storyforge-ai/` |
| `bible` | Story Bible 기능 전반 |
| `outline` | Outline 기능 전반 |
| `chapter` | 챕터 생성·Continuity |
| `docs` | `docs/` |
| `agents` | `.agents/`, 어댑터 |

## 예시

```
feat(ai): 챕터 생성 프롬프트에 이전 챕터 요약 주입
docs(docs): 상태 머신 다이어그램 추가
merge: feature/ai-chapter into develop (no-ff)
```

## 커밋 기록 (생략 금지)

1. 커밋 후 커밋 ID(짧은 해시)를 확인합니다.
2. `docs/branchs/commits/`에 기록 파일을 만듭니다.
   - 파일명: `YYMMDD_HHMM_[커밋ID]_[커밋이름]_[브랜치].md`
   - 예: `261022_0230_a1b2c3d_chapter-continuity_feature-ai.md`
   - `HHMM`은 한 덩어리로, 브랜치의 `/`는 `-`로 바꿉니다.
3. 본문은 [docs/branchs/TEMPLATE.md](../../../docs/branchs/TEMPLATE.md)를 따릅니다.
4. [docs/branchs/README.md](../../../docs/branchs/README.md) 목록 표에 링크를 추가합니다.
5. 기록 파일과 목록 갱신을 **같은 브랜치에** 커밋합니다.

merge나 작은 수정도 예외가 아닙니다.
