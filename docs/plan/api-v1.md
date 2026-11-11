# API v1 스펙

> 기준: 기획서 v1.2 §7.2, §10, §11
> 실행 중인 백엔드의 OpenAPI 문서: `http://localhost:8080/swagger-ui.html`
> **가입, 로그인, 공개 메일 API, 커뮤니티 읽기(아래 🌐)를 뺀 모든 API는 `Authorization: Bearer <토큰>`이 필요합니다** ([auth.md](auth.md)). 없으면 401, 다른 사용자의 리소스는 404, 정지된 계정의 쓰기와 관리자 전용 API는 403입니다.
> 오류 응답은 모두 RFC 9457 ProblemDetail(`application/problem+json`)이며, 사용자 메시지는 `detail`에 있습니다.

## 외부 API (Spring Boot, `/api/v1`)

### 인증

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/auth/register` | 가입 → 토큰. `acceptTerms: true` 필수 |
| POST | `/auth/login` | 로그인 → 토큰 |
| GET | `/auth/me` | 내 정보 |
| GET | `/auth/mail/status` | 공개. SMTP 기능 활성 여부 |
| POST | `/auth/mail/reset-request` | 공개. `{email}`. 가입 여부와 무관한 동일 안내 |
| POST | `/auth/mail/verify-request` | 로그인 필수. 인증 메일 요청 |
| POST | `/auth/mail/verify` | 공개. `{token}`. 이메일 인증 |
| POST | `/auth/mail/reset` | 공개. `{token,password}`. 재설정 및 기존 JWT 폐기 |

메일 링크의 만료·단일 사용·오류 계약은 [auth.md](auth.md)에 있습니다. `UserResponse`에 `emailVerified`가 추가되며 SMTP 활성화 시 미인증 관리자 계정도 `admin=false`입니다.

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
| GET | `/projects` | 내 프로젝트 목록 (최근 수정순) | 대시보드 (§9.6) |
| GET / DELETE | `/projects/{id}` | 조회 / 삭제 | |
| GET | `/projects/{id}/bible` | Bible 조회 (없으면 404) | |
| PUT | `/projects/{id}/bible/confirm` | `BIBLE_DRAFT → BIBLE_CONFIRMED` | §7.2 상태 전이에 대응하는 요청이 §11에 없음 |
| GET / PUT | `/projects/{id}/outline` | Outline 조회 / 저장 (순서 재배치 포함) | §6.2 드래그 앤 드롭 |
| PUT | `/projects/{id}/outline/confirm` | `OUTLINE_DRAFT → OUTLINE_CONFIRMED` | 위와 같음 |
| GET | `/projects/{id}/chapters` | 챕터 목록 | |
| GET / PUT | `/chapters/{id}` | 챕터 조회 / 검토 중 수정 | §6.3 "사용자 승인/수정" |

### Should 기능 ([should-features.md](should-features.md))

| 메서드 | 경로 | 설명 |
|--------|------|------|
| PUT | `/projects/{id}` | 프로젝트 설정 저장 |
| GET | `/projects/{id}/export?format=md\|docx\|pdf` | 승인된 챕터 내보내기 |
| GET | `/projects/{id}/bible/versions` | Bible 버전 이력 (최신순) |
| GET | `/chapters/{id}/versions` | 챕터 생성·수정 이력 (최신순) |

### 커뮤니티 ([community.md](community.md))

🌐는 로그인 없이 읽을 수 있습니다. 다른 사용자의 리소스를 고치려 하면 404입니다.

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET 🌐 | `/works?kind=&genre=&q=&sort=&page=&size=` | 게시판 (`sort`: latest, popular, views, clones) |
| GET 🌐 | `/works/{id}`, `/works/{id}/chapters/{n}` | 작품 상세, 챕터 읽기 |
| POST | `/projects/{id}/publish` | 게시 |
| PUT / DELETE | `/works/{id}` | 작품 정보 수정, 삭제 |
| PUT | `/works/{id}/chapters/{n}` | 게시본 본문 수정 |
| POST | `/works/{id}/sync` | 프로젝트에서 업데이트 |
| GET | `/works/{id}/export?format=` | 내려받기 |
| PUT / DELETE | `/works/{id}/like` | 추천, 취소 |
| GET 🌐 / POST | `/works/{id}/comments` | 댓글 |
| PUT / DELETE | `/comments/{id}` | 댓글 수정, 삭제 |
| GET 🌐 | `/works/{id}/activity` | 작업 그래프 |
| GET | `/projects/{id}/activity` | 프로젝트 작업 기록 |
| POST | `/works/{id}/clone` | Clone |
| GET 🌐 | `/works/{id}/network` | Clone 네트워크 |
| GET | `/projects/{id}/upstream` | 원본 비교 |
| GET 🌐 / POST | `/works/{id}/shares` | 공유 그래프, 공유 |
| POST 🌐 | `/shares/{token}/visit` | 공유 링크 방문 |
| POST | `/packages` | 패키지 생성 |
| POST / PUT / DELETE | `/packages/{id}/items[/{itemId}]` | 작품 추가(다른 작가는 승인 대기), 수정, 빼기 |
| PUT | `/packages/{id}/order` | 독서 순서 |
| POST / DELETE | `/packages/{id}/links[/{linkId}]` | 연결 |
| GET / PUT | `/me/package-requests[/{itemId}]` | 편입 요청 조회, 승인·거절 |
| GET 🌐 / POST | `/posts` | 라운지 목록, 글쓰기 |
| GET 🌐 / PUT / DELETE | `/posts/{id}` | 글 읽기, 수정, 삭제 |
| PUT / DELETE | `/posts/{id}/like` | 추천, 취소 |
| GET 🌐 / POST | `/posts/{id}/comments` | 댓글 |
| GET 🌐 | `/issues` | 이슈 게시판 |
| GET 🌐 | `/users/{id}`, `/users/{id}/works`, `/users/{id}/contributions`, `/users/{id}/activity?date=`, `/users/{id}/achievements` | 프로필, 작품, 활동 잔디, 활동 내역, 업적 |
| GET | `/me/works` | 내 작품 (비공개 포함) |
| PUT | `/me/profile`, `/me/password`, `/me/terms` | 프로필, 비밀번호, 현재 약관 동의 |
| POST | `/me/delete` | 계정 삭제 (본문에 비밀번호) |
| POST | `/guide/chat` | AI 가이드챗 |
| POST | `/projects/{id}/upstream/chapters/{n}/pull` | 원본 챕터를 내 Draft로 가져오기 (전체 원고 Clone, 검토 단계) |
| PUT | `/projects/{id}/upstream/acknowledge` | 원본 현재 리비전까지 확인 표시 |
| POST / GET | `/packages/{id}/continuity` | 패키지 전체 검사 실행 / 최근 결과. 패키지 주인만 |

### 함께 창작 ([community.md](community.md))

| 메서드 | 경로 | 설명 | 누가 |
|--------|------|------|------|
| GET / POST | `/projects/{id}/collaborators` | 참여자 목록 / 이메일로 초대 | 참여자 / 주인 |
| DELETE | `/projects/{id}/collaborators/{userId}` | 초대 취소, 내보내기 | 주인 |
| GET / POST | `/projects/{id}/commits` | 작업 커밋 목록 / 남기기 | 참여자 |
| GET | `/projects/{id}/commits/{commitId}` | 작업 커밋 하나 | 참여자 |
| GET | `/me/invitations` | 받은 초대 | 본인 |
| POST / DELETE | `/me/invitations/{id}/accept`, `/me/invitations/{id}` | 수락 / 거절 | 본인 |

### 신고·관리·알림 ([moderation.md](moderation.md))

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET 🌐 | `/llm-notice` | 외부 언어 모델 사용 여부. `external`이면 원고가 밖으로 나감을 알림 |
| POST | `/reports` | 신고 |
| PUT / DELETE | `/users/{id}/block` | 차단, 해제 |
| GET | `/me/blocks` | 차단 목록 |
| GET | `/admin/reports?status=&page=&size=` | 신고 목록. 관리자만 |
| PUT | `/admin/reports/{id}` | 처리 `{action, note}`. `HIDE`, `RESTORE`, `DISMISS`, `SUSPEND`, `UNSUSPEND` |
| GET | `/me/notifications`, `/me/notifications/unread-count` | 알림 목록, 안 읽은 수 |
| PUT | `/me/notifications/{id}/read`, `/me/notifications/read-all` | 읽음 처리 |

### 독립 운영 콘솔

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/admin/overview` | 회원·프로젝트·작품·신고 집계 |
| GET | `/admin/users?q=&suspended=&page=&size=` | 회원 검색·필터·페이지 |
| PUT | `/admin/users/{id}/suspension` | `{suspended,note}` 직접 정지·해제 |
| POST | `/admin/reports/batch` | `{reportIds,action,note}` 최대 50건 원자적 조치 |
| GET | `/admin/audit?actorId=&page=&size=` | 감사 기록 |

모두 관리자만 가능합니다. 상세 필드·충돌·잠금 규칙은 [운영 콘솔 스펙](operations-console.md)을 따릅니다.

### 챕터 생성 규칙

- 생성할 수 있는 챕터는 **확정된 챕터 수 + 1**뿐입니다. 이전 챕터를 승인하지 않으면 다음 챕터를 만들 수 없습니다.
- 검토 중(Draft)인 챕터는 같은 번호로 다시 생성(재생성)할 수 있습니다.
- 마지막 챕터를 승인하면 `COMPLETED`가 됩니다.
- 생성 뒤 본문을 고쳤거나 Clone·원본 가져오기로 들어온 챕터는 `needsReview: true`입니다. 승인할 때 먼저 요약과 Continuity를 다시 만듭니다. 심각한 이슈가 나오면 결과를 저장하고 **409**로 멈추며, 다시 승인하면 진행합니다. AI가 실패하면 **502**이고 승인되지 않습니다.
- 재검증 도중 챕터가 수정되면 **409**입니다. 이전 본문의 요약·리포트를 적용하지 않으며 최신 본문을 다시 검증해야 합니다. 본문을 바꿨다가 원래대로 저장해도 내부 버전이 달라지면 거부합니다.

### 챕터 생성 SSE 이벤트

요청 헤더: `Accept: text/event-stream, application/problem+json` (상태 오류는 SSE 대신 ProblemDetail로 옵니다)

| event | data | 설명 |
|-------|------|------|
| `token` | `{"text": "..."}` | 본문 조각 |
| `done` | ChapterResponse JSON (Continuity 리포트 포함) | 생성 완료, Draft 저장, 상태 `CHAPTER_REVIEW` |
| `error` | `{"message": "..."}` | 실패. 상태는 생성 전으로 되돌아감 |

AI SSE가 `done` 없이 종료된 경우도 `error`이며 부분 본문을 저장하지 않습니다. 첫 장은 `OUTLINE_CONFIRMED`, 이후 장은 `CHAPTER_REVIEW`로 돌아갑니다.

## 내부 API (FastAPI, Spring Boot만 호출)

| 메서드 | 경로 | 요청 | 응답 |
|--------|------|------|------|
| POST | `/internal/generate/bible` | `{project, prompt}` | `{worldSetting, theme, tone, characters[]}` |
| POST | `/internal/generate/outline` | `{project, bible, chapterCount}` | `{chapters[]}` |
| POST | `/internal/generate/chapter` | ChapterContext `{project, bible, chapterNumber, outlineChapter, previousChapters[]}` | SSE: `token`(텍스트), `done`(`{title, content, summary, report{issues[], score}}`), `error`(메시지) |
| POST | `/internal/generate/review` | `{context, title, content}` | `{summary, report}` |
| POST | `/internal/package/continuity` | `{title, worldNote, works[], links[]}` | `{issues[], score}` |
| POST | `/internal/guide/chat` | `{message, context}` | `{reply, actions[{label, kind, target}]}` |
| GET | `/health` | — | `{status, provider, model}` |

계약 정의 위치: 백엔드 `client/AiDtos.java` ↔ AI `app/models/schemas.py`. 한쪽을 바꾸면 다른 쪽도 같은 커밋에서 바꿉니다.

2026-11-11 컨텍스트 확장: `project`는 `form`(LONG/SHORT), `creativeBrief`(최초 창작 요구)를 포함합니다. `ChapterContext.storyOutline`은 전체 아웃라인이며 `previousChapters[].endingExcerpt`는 직전 확정 장의 마지막 1,200자입니다. FastAPI는 추가 필드 없는 기존 입력에도 기본값을 적용합니다. 실모델은 본문 후 요약과 Continuity를 하나의 구조화 분석으로 생성하고, 반복 루프는 SSE error로 종료합니다.

## Continuity 리포트

- `issues[].type`: `setting_conflict`(설정 충돌), `character_consistency`(캐릭터 일관성), `timeline`(타임라인), `plot_thread`(플롯 스레드) — §6.3의 네 항목
- `issues[].severity`: `low` / `medium` / `high`
- `score`: 100에서 심각도별 감점 (high 30, medium 12, low 4), 최저 0
- 규칙 기반 검사(Outline 등장 캐릭터 누락 등)는 항상 실행하고, mock 외의 Ollama·Anthropic 경로에서는 LLM 검증을 더합니다.
- 화면은 high 이슈가 있으면 승인 전에 한 번 더 확인합니다. 결과를 자동으로 통과 처리하지 않습니다.
