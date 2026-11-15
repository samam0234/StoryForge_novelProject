# 문서 개편 전 상태 확인 — 2026-11-15

- 브랜치: `feature/auth`
- 범위: 문서만 변경. 코드, 설정, 마이그레이션은 바꾸지 않았습니다.
- 목적: README에 적는 수치와 설명을 현재 코드로 다시 확인

## 자동 검사 (로컬)

| 검사 | 명령 | 결과 |
|------|------|------|
| 백엔드 | `./mvnw test` | 108건 통과, 실패·오류·건너뜀 0 |
| AI 서비스 | `.venv/Scripts/python -m pytest -q` | 77건 통과 |
| AI 타입 검사 | `.venv/Scripts/pyright --project ..` | 오류 0, 경고 0 |
| 사용자 웹 | `npx ng test --watch=false` | 54건 통과 (8파일) |
| 사용자 웹 빌드 | `npx ng build` | 통과 |
| 운영 콘솔 | `npm test` | 8건 통과 (2파일) |
| 운영 콘솔 빌드 | `npm run build` | 통과 |
| 문서 링크 | 추적 중인 Markdown의 상대 링크와 문서 안 앵커 | 깨진 링크 0 |

## 확인하면서 알게 된 것

- **백엔드 첫 실행은 108건 중 92건이 건너뛰어졌습니다.** Docker Desktop이 꺼져 있었습니다. 종료 코드는 0이라 건너뜀을 보지 않으면 통과로 오해합니다. Docker를 켜고 다시 실행해 건너뜀 0을 확인했습니다.
- `.env.example`의 `LLM_EFFORT`는 `medium`이고 코드 기본값(`app/core/config.py`)과 CURRENT_STACK은 `high`입니다. 값은 바꾸지 않고 CURRENT_STACK에 차이를 적었습니다. 어느 쪽이 의도인지 결정이 필요합니다.
- 기획 원본은 `docs/plan/Proposal/`에 있습니다. 정본 SKILL.md에는 "기획 원본을 `docs/` 안으로 옮기지 않는다"는 문장이 남아 있어 현재 위치와 어긋납니다. 원본과 규칙은 이번에 건드리지 않았습니다. 사용자 결정이 필요합니다.

## 하지 않은 것

- 브라우저 확인. 화면 코드는 바꾸지 않았습니다. 2026-11-10 이후 추가한 화면의 미검증 상태는 그대로입니다.
- 실제 모델(Ollama, Claude) 실행.
- 배포 구성(`docker-compose.deploy.yml`) 실행.
- 원격 CI 결과 확인. 로컬 결과를 원격 CI 통과로 간주하지 않습니다.
