# 브랜치와 병합 규칙

> 정본 [SKILL.md](SKILL.md) "브랜치와 병합" 절의 상세 문서입니다. 어긋나면 정본이 이깁니다.

## 브랜치 종류

| 브랜치 | 용도 | 직접 커밋 |
|--------|------|-----------|
| `main` | 배포 기준 통합 브랜치 | 금지 |
| `develop` | 개발 통합 브랜치 | 금지 (일상 기능) |
| `feature/*` | 기능 개발 | 허용 |
| `bugfix/*` | 버그 수정 | 허용 |
| `experimental/*` | 실험, PoC | 허용 |

**브랜치를 임의로 만들지 않습니다.** 형식에 맞더라도 사용자가 정해 주지 않은 브랜치는 새로 만들지 않습니다.

| 현재 지정 브랜치 | 용도 |
|------------------|------|
| `main` | 기본 브랜치 |
| `develop` | 통합 |
| `feature/auth` | 작업 브랜치 (1개). 인증 이후 작업도 여기서 합니다 |

## 흐름

```
feature/* ──(--no-ff)──▶ develop ──(--no-ff)──▶ main
```

1. 최신 `develop`에서 작업 브랜치를 만듭니다.
2. 작업 브랜치에서 커밋합니다. 커밋 기록은 필요할 때만 남깁니다.
3. **사용자가 요청하면** `develop`으로 병합합니다.
   ```
   git checkout develop
   git merge --no-ff feature/xxx -m "merge: feature/xxx into develop (no-ff)"
   ```
4. `main` 병합은 **사용자의 직접 명령이 있을 때만**, `develop`을 거친 뒤에 합니다.

## 금지 사항

- fast-forward 병합
- `develop` 없이 `main`에 직행
- 일상 기능을 `develop`에 직접 커밋
- feature끼리 장기 병합
- force-push (명시적 요청 제외)
- `.env` 시크릿 커밋
