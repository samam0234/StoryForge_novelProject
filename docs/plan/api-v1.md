# API v1 스펙

> 기준: 기획서 v1.2 §7.2, §10, §11
> 실행 중인 백엔드의 OpenAPI 문서: `http://localhost:8080/swagger-ui.html`
> 오류 응답은 모두 RFC 9457 ProblemDetail(`application/problem+json`)이며, 사용자 메시지는 `detail`에 있습니다.

## 외부 API (Spring Boot, `/api/v1`)

### 기획서 §11에 있는 엔드포인트

| 메서드 | 경로 | 설명 | 허용 상태 |
|--------|------|------|-----------|
| POST | `/projects` | 프로젝트 생성 → `BIBLE_DRAFT` | — |
| POST | `/projects/{id}/bible/generate` | Bible AI 생성. 본문 `{prompt}` | `BIBLE_DRAFT` |
| PUT | `/projects/{id}/bible` | Bible 저장 (전체 교체, 버전 +1) | `BIBLE_DRAFT` |
| POST | `/projects/{id}/outline/generate` | Outline AI 생성. 본문 `{chapterCount}` | `BIBLE_CONFIRMED`, `OUTLINE_DRAFT` |
| POST | `/projects/{id}/chapters/{num}/generate` | 챕터 생성 SSE | `OUTLINE_CONFIRMED`, `CHAPTER_REVIEW` |
| PUT | `/chapters/{id}/confirm` | 챕터 승인 | `CHAPTER_REVIEW` |
| GET | `/projects/{id}/status` | 상태, 챕터 수, 다음 생성 가능 챕터 | — |

### 구현하면서 추가한 보조 엔드포인트

기획서 §11은 "예시"이므로 화면에 필요한 조회·확정 엔드포인트를 추가했습니다. 상태 머신 자체는 바꾸지 않았습니다.

| 메서드 | 경로 | 설명 | 이유 |
|--------|------|------|------|
| GET | `/projects` | 목록 (최근 수정순) | 대시보드 (§9.6) |
| GET / DELETE | `/projects/{id}` | 조회 / 삭제 | |
| GET | `/projects/{id}/bible` | Bible 조회 (없으면 404) | |
| PUT | `/projects/{id}/bible/confirm` | `BIBLE_DRAFT → BIBLE_CONFIRMED` | §7.2 상태 전이에 대응하는 요청이 §11에 없음 |
| GET / PUT | `/projects/{id}/outline` | Outline 조회 / 저장 (순서 재배치 포함) | §6.2 드래그 앤 드롭 |
| PUT | `/projects/{id}/outline/confirm` | `OUTLINE_DRAFT → OUTLINE_CONFIRMED` | 위와 같음 |
| GET | `/projects/{id}/chapters` | 챕터 목록 | |
| GET / PUT | `/chapters/{id}` | 챕터 조회 / 검토 중 수정 | §6.3 "사용자 승인/수정" |

### 챕터 생성 규칙

- 생성할 수 있는 챕터는 **확정된 챕터 수 + 1**뿐입니다. 이전 챕터를 승인하지 않으면 다음 챕터를 만들 수 없습니다.
- 검토 중(Draft)인 챕터는 같은 번호로 다시 생성(재생성)할 수 있습니다.
- 마지막 챕터를 승인하면 `COMPLETED`가 됩니다.

### 챕터 생성 SSE 이벤트

요청 헤더: `Accept: text/event-stream, application/problem+json` (상태 오류는 SSE 대신 ProblemDetail로 옵니다)

| event | data | 설명 |
|-------|------|------|
| `token` | `{"text": "..."}` | 본문 조각 |
| `done` | ChapterResponse JSON (Continuity 리포트 포함) | 생성 완료, Draft 저장, 상태 `CHAPTER_REVIEW` |
| `error` | `{"message": "..."}` | 실패. 상태는 생성 전으로 되돌아감 |

## 내부 API (FastAPI, Spring Boot만 호출)

| 메서드 | 경로 | 요청 | 응답 |
|--------|------|------|------|
| POST | `/internal/generate/bible` | `{project, prompt}` | `{worldSetting, theme, tone, characters[]}` |
| POST | `/internal/generate/outline` | `{project, bible, chapterCount}` | `{chapters[]}` |
| POST | `/internal/generate/chapter` | ChapterContext `{project, bible, chapterNumber, outlineChapter, previousChapters[]}` | SSE: `token`(텍스트), `done`(`{title, content, summary, report{issues[], score}}`), `error`(메시지) |
| GET | `/health` | — | `{status, provider, model}` |

계약 정의 위치: 백엔드 `client/AiDtos.java` ↔ AI `app/models/schemas.py`. 한쪽을 바꾸면 다른 쪽도 같은 커밋에서 바꿉니다.

## Continuity 리포트

- `issues[].type`: `setting_conflict`(설정 충돌), `character_consistency`(캐릭터 일관성), `timeline`(타임라인), `plot_thread`(플롯 스레드) — §6.3의 네 항목
- `issues[].severity`: `low` / `medium` / `high`
- `score`: 100에서 심각도별 감점 (high 30, medium 12, low 4), 최저 0
- 규칙 기반 검사(Outline 등장 캐릭터 누락 등)는 항상 실행하고, `LLM_PROVIDER=anthropic`이면 LLM 검증을 더합니다.
- 화면은 high 이슈가 있으면 승인 전에 한 번 더 확인합니다. 결과를 자동으로 통과 처리하지 않습니다.
