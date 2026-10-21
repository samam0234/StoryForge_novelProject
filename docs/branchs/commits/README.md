# docs/branchs/commits/

커밋 기록 파일을 두는 폴더입니다. 커밋 하나에 파일 하나입니다.

## 파일명

```
YYMMDD_HHMM_[커밋ID]_[커밋이름]_[브랜치].md
```

| 부분 | 규칙 | 예 |
|------|------|----|
| `YYMMDD_HHMM` | 커밋 일시. `HHMM`은 한 덩어리 | `261022_0230` |
| `커밋ID` | 짧은 해시 | `a1b2c3d` |
| `커밋이름` | 커밋 내용을 나타내는 kebab-case | `chapter-continuity` |
| `브랜치` | 브랜치 이름의 `/`를 `-`로 | `feature-ai` |

예: `261022_0230_a1b2c3d_chapter-continuity_feature-ai.md`

## 규칙

- 본문은 [../TEMPLATE.md](../TEMPLATE.md)를 따릅니다.
- 파일을 만들면 [../README.md](../README.md)의 "커밋 기록 목록"에도 추가합니다.
- 기록 파일과 목록 갱신은 같은 브랜치에 커밋합니다.
- `docs/commits/`에는 기록하지 않습니다. 이 폴더만 사용합니다.
