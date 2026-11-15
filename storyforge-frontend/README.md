# storyforge-frontend

StoryForge의 **사용자 웹**입니다. 소설을 쓰는 화면(내 프로젝트, Story Bible, Outline, 챕터)과 읽고 나누는 화면(게시판, 작품, 패키지포지, 프로필)을 제공하는 Angular SPA입니다.

| | |
|---|---|
| **기술** | Angular 21.2 (standalone 컴포넌트, signals, zoneless), Angular Material, TipTap 3, RxJS 7.8, TypeScript 5.9 |
| **주소** | 개발 http://localhost:4200 |
| **호출 대상** | Spring Boot(`/api/v1`)만. FastAPI는 호출하지 않습니다 |
| **테스트** | Vitest 56건(화면 스모크 포함) + 프로덕션 빌드 + 수동 브라우저 점검 |
| **운영자 화면** | 별도 앱 [storyforge-console](../storyforge-console/README.md) |

이 앱은 **보여 주고 요청만 합니다.** 프로젝트가 어느 단계인지, 다음에 무엇을 할 수 있는지는 백엔드가 알려 준 상태를 그대로 따릅니다. LLM 호출, Continuity 판단, 상태 전이는 여기에 없습니다.

## 목차

- [실행](#실행)
- [화면 지도](#화면-지도)
- [백엔드와 통신하는 방법](#백엔드와-통신하는-방법)
- [로그인과 세션](#로그인과-세션)
- [구조](#구조)
- [디자인](#디자인)
- [테스트](#테스트)
- [배포 빌드](#배포-빌드)
- [문제 해결](#문제-해결)
- [규칙](#규칙)
- [한계](#한계)

---

## 실행

### 준비물

| 항목 | 버전 / 조건 | 비고 |
|------|-------------|------|
| Node.js | 24.14 이상 | Angular 22는 Node 24.15+가 필요해 21을 씁니다 |
| npm | 11 이상 | Node와 함께 설치됩니다 |
| 백엔드 | `http://localhost:8080`에서 실행 중 | 없어도 화면은 뜨지만 "서버에 연결할 수 없습니다"가 표시됩니다 |

Angular CLI는 전역 설치하지 않습니다. `npx ng ...`가 프로젝트에 설치된 CLI를 씁니다.

### 처음 한 번

```bash
cd storyforge-frontend
npm install
```

### 개발 서버

```bash
npx ng serve          # 또는 npm start
```

- 주소: http://localhost:4200
- 코드를 저장하면 자동으로 다시 빌드됩니다.
- `/api`로 시작하는 요청은 [proxy.conf.json](proxy.conf.json)이 `http://localhost:8080`으로 넘깁니다. `"ws": true`가 있어 웹소켓도 같은 대상으로 갑니다. 그래서 개발 중에는 CORS 설정 없이 동작합니다.

### 명령어

| 하려는 일 | 명령 | 결과 |
|-----------|------|------|
| 개발 서버 | `npx ng serve` | http://localhost:4200 |
| 다른 포트로 실행 | `npx ng serve --port 4300` | 백엔드 `FRONTEND_ORIGIN`도 맞춰야 합니다 |
| 프로덕션 빌드 | `npx ng build` | `dist/storyforge-frontend/browser/` |
| 빌드 감시 | `npm run watch` | 변경 시 개발 빌드 |
| 단위 테스트 | `npx ng test --watch=false` | Vitest 56건 |
| 브라우저 점검 | `node scripts/browser-check.mjs` | [수동](#브라우저-점검-수동). 전체 스택 필요 |

### 백엔드 주소를 바꾸려면

| 바꿀 곳 | 내용 |
|---------|------|
| [docs/plan/CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md) | 포트 기준값 — **먼저 고칩니다** |
| [proxy.conf.json](proxy.conf.json) | 개발 서버가 `/api`를 넘길 대상 (`target`) |
| [src/environments/environment.ts](src/environments/environment.ts) | API 기본 경로 `apiBaseUrl` (기본 `/api/v1`) |

---

## 화면 지도

🌐는 로그인 없이 열리는 화면입니다.

### 창작

| 화면 | 경로 | 하는 일 | 기획서 |
|------|------|---------|--------|
| 로그인·가입 🌐 | `/login` | 이메일·비밀번호, 약관 동의. 성공하면 원래 가려던 화면으로 | §7.1 |
| 내 프로젝트 | `/` | 다음 할 일, 상태 필터, 진행률, 수동·에이전트 비율, 게시·Clone 표시 | §9.6-1 |
| Story Bible | `/projects/:id/bible` | AI 생성, Overview/Characters/World/Themes 편집, 이력 비교, 확정 | §6.1, §9.6-2 |
| Outline | `/projects/:id/outline` | AI 생성, 드래그 앤 드롭 재배치, Beats 편집, 확정 | §6.2, §9.6-3 |
| 챕터 | `/projects/:id/chapters` | 3단 워크스페이스. 스트리밍 생성, TipTap 편집, Continuity 리포트, 장면 개척, 승인·재생성 | §6.3, §9.6-4 |
| 설정 | `/projects/:id/settings` | 제목, 장르, 모델, 기본 톤, 내보내기 형식, 삭제 | §9.6-5 |
| 작업 기록 | `/projects/:id/activity` | 작업 그래프, 작업 기록표, 함께 창작 초대와 작업 커밋 | |
| 게시 | `/projects/:id/publish` | 승인된 챕터를 작품으로 게시 | |
| 원본 비교 | `/projects/:id/upstream` | Clone 원본과의 Diff, 원본으로 덮어쓰기, 확인 표시 | |

프로젝트 화면 위쪽에는 파이프라인 단계 표시와 **내보내기** 메뉴(MD·TXT·DOCX·PDF·HTML·EPUB·ODT·RTF)가 있습니다. Bible과 챕터 화면의 **이력** 버튼은 두 버전을 단어 단위로 비교합니다.

### 커뮤니티

| 화면 | 경로 | 하는 일 |
|------|------|---------|
| 게시판 🌐 | `/board/long`, `/board/short`, `/board/package` | 검색, 장르, 정렬 |
| 작품 🌐 | `/works/:id` | 읽기, 세계관, 작업 그래프, 공유 그래프, Clone 네트워크, 이력, 댓글, 추천, 신고 |
| 창작하기 🌐 | `/create` | 새 작품 시작, 함께 창작 초대, 창작 라운지 |
| 라운지 글 🌐 | `/posts/:id` | 글, 댓글, 추천, 신고 |
| 이슈 🌐 | `/issues` | 달성 축하, 최근 7일 인기작, 새 작품 |
| 프로필 🌐 | `/users/:id` | 활동 잔디, 활동 내역, 업적, 차단, 신고 |
| 패키지포지 | `/package-forge[/:id]` | 작품 묶기, 독서 순서, 연결 지도, 전체 Continuity 검사 |
| 내 작품 관리 | `/me/works`, `/me/works/:id/edit` | 게시 작품 수정·삭제, 프로젝트에서 업데이트, 패키지 편입 요청 |

### 계정과 관리

| 화면 | 경로 | 하는 일 |
|------|------|---------|
| 계정 설정 | `/me/settings` | 프로필, 비밀번호, 이메일 인증 요청, 차단 목록, 계정 삭제 |
| 계정 메일 🌐 | `/account-mail` | 메일 링크로 들어오는 이메일 인증, 비밀번호 재설정 |
| 이용약관 🌐 | `/terms` | 약관(법률 검토 전 초안), 동의 |
| 신고 관리 | `/admin` | 관리자 전용. 서버가 권한을 다시 확인합니다 |

화면 상단에는 알림 종과 AI 가이드챗 버튼이 있습니다. 화면별 파일은 [src/app/features/README.md](src/app/features/README.md)의 표에 있습니다.

## 백엔드와 통신하는 방법

용도에 따라 세 가지 방식을 씁니다.

| 방식 | 쓰는 곳 | 구현 | 알아 둘 점 |
|------|---------|------|-----------|
| **REST** | 대부분의 요청 | `core/api/storyforge-api.service.ts`, `community-api.service.ts`, `moderation-api.service.ts` | 화면은 `HttpClient`를 직접 쓰지 않습니다. 오류는 ProblemDetail의 `detail`을 사용자 메시지로 바꿉니다 |
| **SSE** | 챕터 본문 생성 | `core/api/chapter-stream.service.ts` | 생성 요청이 POST라 `EventSource` 대신 `fetch` 스트림으로 읽습니다. 이벤트는 `token` → `done` 또는 `error` |
| **WebSocket** | 가이드챗, 장면 개척 | `core/api/live-channel.service.ts` | `/api/v1/ws`. 연결이 없으면 가이드는 REST로 돌아갑니다. 챕터 본문은 이 소켓으로 오지 않습니다 |

챕터 SSE는 `fetch`를 쓰므로 HTTP 인터셉터를 거치지 않고 토큰을 직접 붙입니다. 인증 처리를 바꾸면 `chapter-stream.service.ts`도 함께 확인해야 합니다.

```
챕터 생성 클릭
  → POST /api/v1/projects/{id}/chapters/{num}/generate   (fetch 스트림)
  → token 이벤트마다 화면에 본문 추가
  → done 이벤트: 제목, 본문, 요약, Continuity 리포트
  → 사용자가 확인 후 PUT /api/v1/chapters/{id}/confirm
```

## 로그인과 세션

- 세션은 브라우저 `localStorage`에 보관합니다 (`core/auth/auth.service.ts`).
- 인터셉터가 `/api/v1` 요청에 Bearer 토큰을 붙입니다. 액세스 토큰이 만료되면 갱신 토큰으로 세션을 이어 가고, 갱신도 실패하면 로그인 화면으로 보냅니다.
- 비밀번호를 바꾸거나 재설정하면 서버가 이전 토큰을 모두 거부합니다.
- 가드는 세 가지입니다: `authGuard`(로그인 필요), `guestGuard`(로그인 화면용), `adminGuard`(관리자 화면). **가드는 화면 이동만 막습니다.** 실제 권한 검사는 항상 백엔드가 합니다.

## 구조

```
storyforge-frontend/
├── proxy.conf.json        # 개발 서버 /api, 웹소켓 → 백엔드
├── angular.json           # 빌드 설정 (proxyConfig, budgets)
├── Dockerfile, nginx.conf # 배포 이미지 (정적 파일 + /api 프록시)
├── public/                # 그대로 복사되는 파일
├── scripts/               # 수동 브라우저 점검 (browser-check.mjs)
└── src/
    ├── environments/      # apiBaseUrl
    ├── styles/            # 디자인 토큰, Material 테마
    └── app/
        ├── app.routes.ts  # 경로. 화면은 모두 지연 로딩
        ├── core/          # 싱글톤: API 래퍼, 인증, 공통 타입
        ├── shared/        # 두 곳 이상에서 쓰는 컴포넌트
        ├── features/      # 화면. 기능 하나에 폴더 하나
        └── layout/        # 셸, 테마 서비스, 알림 종
```

| 폴더 | 안내 | 두는 것 |
|------|------|---------|
| `core/` | [README](src/app/core/README.md) | 앱 전체에서 한 번만 만드는 서비스, 인터셉터, 가드, 타입 |
| `shared/` | [README](src/app/shared/README.md) | 에디터, 버전 이력, 그래프(작업·공유·Clone·패키지 지도·활동 잔디), 카드, 이미지 올리기 |
| `features/` | [README](src/app/features/README.md) | `auth`, `dashboard`, `projects`, `story-bible`, `outline`, `chapters`, `board`, `lounge`, `issues`, `packages`, `profile`, `guide`, `moderation`, `legal` |

**어디에 둘지** 판단하는 기준은 하나입니다. 한 기능에서만 쓰면 그 기능 폴더에, 두 곳 이상에서 쓰면 `shared/`에, 상태를 가진 싱글톤이면 `core/`에 둡니다.

## 디자인

- 다크 모드가 기본이고 툴바 버튼으로 라이트 모드로 바꿉니다. 선택은 브라우저에 저장합니다.
- 팔레트는 기획서 §9.3을 따릅니다: Primary `#1F4E79`, Secondary `#2E75B6`, Accent `#E8A838`.
- 글꼴은 Pretendard(CDN), 아이콘은 Material Icons입니다.
- 토큰과 테마는 [src/styles/](src/styles/)에서 관리합니다.
- 차트 색은 `--sf-series-*` 토큰만 쓰고, 색만으로 구분하지 않도록 범례와 표를 함께 둡니다.

## 테스트

```bash
npx ng test --watch=false     # Vitest 56건
npx ng build                  # 반드시 함께 확인
```

| 다루는 것 | 다루지 않는 것 |
|-----------|----------------|
| SSE 파서, 인증·세션·가드, 메일 복구 화면, 공통 컴포넌트, 그래프, 가이드챗, 알림. **모든 라우트의 화면이 템플릿 오류 없이 만들어지는지**(`routes.smoke.spec.ts`) | 화면의 동작, 실제 브라우저 |

**`ng build`를 빼먹지 않습니다.** 테스트 빌드는 spec이 가져오는 파일만 컴파일하므로, 어떤 spec도 가져오지 않는 화면은 템플릿이 깨져도 테스트가 통과했습니다. 실제로 그렇게 빌드가 깨진 채 `main`까지 올라간 적이 있어([2026-11-04 일지](../docs/find_debug/2026-11-04.md)) 두 가지를 더했습니다. ① `routes.smoke.spec.ts`가 라우트에 연결된 모든 화면을 실제로 만들어 보므로 같은 오류가 `ng test`에서도 실패합니다 (일부러 남는 `</div>`를 넣어 `NG5002`로 실패하는 것을 확인했습니다). ② `ng build`를 필수 확인으로 두고 CI에서도 돌립니다. 스모크 테스트는 화면이 **그려지는지**만 봅니다. 동작은 검증하지 않습니다.

## 브라우저 점검 (수동)

실제 Chrome으로 주요 화면을 확인하는 도구입니다. 자동 테스트가 아니고 **CI에 들어가지 않습니다.** 전체 스택이 떠 있어야 하고 새 계정과 작품을 만들기 때문입니다. 로컬 개발 DB에서만 실행하세요.

```bash
# 백엔드를 점검용 관리자 이메일로 실행해 둡니다 (콘솔 로그인 확인용)
ADMIN_EMAILS=browser-admin@example.com ./mvnw spring-boot:run     # storyforge-backend

cd storyforge-frontend
npm install --no-save playwright-core        # package.json에는 넣지 않습니다
node scripts/browser-check.mjs               # HEADED=1 이면 창을 띄웁니다
```

| 확인하는 것 | 방법 |
|-------------|------|
| 가입(약관 미동의 시 버튼 비활성, 동의 후 이동) | 화면 조작 |
| 프로젝트 목록, Bible·Outline·챕터·작업 기록·게시·설정 화면, 챕터 제목 | API로 준비한 뒤 화면 확인 |
| 로그아웃 상태: 게시판·작품 읽기, 응답에 이메일 없음, 보호된 화면은 로그인으로 | 별도 브라우저 |
| 좁은 화면(390px)에서 가로 넘침 없음 | 화면 폭 지정 |
| 운영 콘솔: 일반 계정 거부, 관리자 로그인, 현황·신고·회원·감사 화면 | 화면 조작 |
| 페이지 오류와 콘솔 오류 없음 | 전체 실행 동안 수집 |

확인하지 **않는** 것: 메일 인증·재설정 화면, 콘솔의 신고 일괄 조치, 화면에서 누르는 챕터 생성(스트리밍), Clone, 패키지포지 흐름, 내보낸 파일 열기. 환경 변수는 `WEB`, `CONSOLE`, `API`, `ADMIN_EMAIL`입니다. 결과는 [검증 기록](../docs/vaildates/browser-check-2026-11-15.md)에 있습니다.

## 배포 빌드

[Dockerfile](Dockerfile)은 Node 24에서 빌드한 정적 파일을 nginx에 올립니다. [nginx.conf](nginx.conf)가 하는 일은 세 가지입니다.

- `/api/` 요청을 `backend:8080`으로 넘깁니다 (읽기 제한 180초, 챕터 생성 시간에 맞춤).
- 웹소켓 업그레이드 헤더를 전달합니다.
- 그 밖의 경로는 `index.html`로 돌려 SPA 라우팅이 동작하게 합니다.

전체 구성은 루트의 [docker-compose.deploy.yml](../docker-compose.deploy.yml)에 있습니다. 실제 서버에 배포해 검증한 적은 없습니다.

## 문제 해결

| 증상 | 원인 / 해결 |
|------|-------------|
| "서버에 연결할 수 없습니다" | 백엔드(8080)가 꺼져 있습니다. `storyforge-backend`에서 `./mvnw spring-boot:run` |
| "챕터 생성에 실패했습니다" | AI 서비스(8001)가 꺼져 있습니다. 실패해도 프로젝트 상태는 생성 전으로 돌아갑니다 |
| 게시가 "이용약관에 동의해야" | 약관 도입 전 계정이거나 약관 버전이 올라갔습니다. `/terms`에서 동의합니다 |
| 승인이 "심각한 연속성 이슈"로 멈춤 | 고친 원고의 재검증 결과입니다. 리포트를 확인하고 다시 승인하면 진행됩니다 |
| 가이드챗이나 장면 개척이 응답하지 않음 | 프록시의 `"ws": true`와 백엔드 실행 여부를 확인합니다 |
| `ng` 실행 시 Node 버전 오류 | Node 24.14 이상인지 확인합니다 (`node -v`) |
| 4200 포트 사용 중 | `npx ng serve --port 4300`으로 바꾸고 백엔드 `FRONTEND_ORIGIN`도 맞춥니다 |

## 규칙

- LLM 호출, Continuity 판단, 상태 전이 로직을 넣지 않습니다.
- `CHAPTER_REVIEW`를 건너뛰는 자동 승인을 만들지 않습니다.
- FastAPI 주소, API 키 같은 시크릿을 `environments/`에 넣지 않습니다. 번들에 그대로 노출됩니다.
- 다른 사용자가 쓴 글을 `innerHTML`로 넣지 않습니다. 본문은 읽기 전용 TipTap, 댓글과 게시글은 글자로 보여 줍니다.
- `effect` 안에서는 일부러 추적할 신호만 읽습니다. 날짜나 페이지 신호까지 추적하면 화면 전체가 다시 그려집니다.

## 한계

- 메일 인증·재설정 화면은 실제 브라우저에서 확인하지 못했습니다. 주요 화면은 2026-11-15 수동 점검으로 확인했지만 위 "확인하지 않는 것"은 남아 있습니다.
- 알림은 실시간이 아니라 1분마다 확인합니다.
- 캐릭터 상태 자동 갱신은 Phase 2 항목이라 없습니다.
- 이용약관 문구는 법률 검토 전 초안입니다.

## 관련 문서

- [기획서 §8.1, §9](../docs/plan/Proposal/StoryForge_기획서_v1.2.md)
- [api-v1.md](../docs/plan/api-v1.md) — 호출하는 API와 SSE 이벤트
- [community.md](../docs/plan/community.md), [moderation.md](../docs/plan/moderation.md) — 화면이 따르는 스펙
- [local-dev-setup.md](../docs/guidance/local-dev-setup.md) — 전체 스택 실행
- 저장소 안내 [../README.md](../README.md) · 작업 규칙 [../AGENTS.md](../AGENTS.md) · 보안 [../SECURITY.md](../SECURITY.md) · 라이선스 [../LICENSE](../LICENSE)
