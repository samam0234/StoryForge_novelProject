# storyforge-frontend/

StoryForge의 **Angular SPA**입니다. 대시보드, Story Bible 에디터, Outline 관리, 챕터 워크스페이스 화면을 제공합니다.
API는 Spring Boot(`storyforge-backend`)만 호출합니다. FastAPI를 직접 호출하지 않습니다.

---

## 실행 방법

### 준비물

| 항목 | 버전 / 조건 | 비고 |
|------|-------------|------|
| Node.js | 24.14 이상 | Angular 22는 Node 24.15+가 필요해서 21을 씁니다 |
| npm | 11 이상 | Node와 함께 설치됨 |
| 백엔드 | `http://localhost:8080`에서 실행 중 | 없어도 화면은 뜨지만 "서버에 연결할 수 없습니다"가 표시됩니다 |

Angular CLI는 전역 설치하지 않아도 됩니다. `npx ng ...`가 프로젝트에 설치된 CLI를 씁니다.

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
- `/api`로 시작하는 요청은 [proxy.conf.json](proxy.conf.json)이 `http://localhost:8080`으로 전달합니다. 그래서 개발 중에는 CORS 설정 없이 동작합니다.

### 명령어

| 하려는 일 | 명령 | 결과 |
|-----------|------|------|
| 개발 서버 | `npx ng serve` | http://localhost:4200 |
| 다른 포트로 실행 | `npx ng serve --port 4300` | 백엔드 `FRONTEND_ORIGIN`도 맞춰야 합니다 (CORS) |
| 운영 빌드 | `npx ng build` | `dist/storyforge-frontend/browser/` |
| 빌드 감시 | `npm run watch` | 변경 시 개발 빌드 |
| 단위 테스트 | `npx ng test` | Vitest (아직 테스트 파일 없음) |

### 백엔드 주소를 바꾸려면

| 바꿀 곳 | 내용 |
|---------|------|
| [proxy.conf.json](proxy.conf.json) | 개발 서버가 `/api`를 전달할 대상 (`target`) |
| [src/environments/environment.ts](src/environments/environment.ts) | API 기본 경로 `apiBaseUrl` (기본 `/api/v1`) |
| [docs/plan/CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md) | 포트 기준값 — **먼저 고칩니다** |

---

## 화면

| 화면 | 경로 | 주요 기능 | 기획서 |
|------|------|-----------|--------|
| 대시보드 | `/` | 프로젝트 카드 그리드, 진행률, 새 프로젝트 | §9.6-1 |
| Story Bible | `/projects/:id/bible` | AI 생성, Overview/Characters/World/Themes 탭 편집, 확정 | §6.1, §9.6-2 |
| Outline | `/projects/:id/outline` | AI 생성, 드래그 앤 드롭 재배치, Beats 편집, 확정 | §6.2, §9.6-3 |
| 챕터 | `/projects/:id/chapters` | 3단 워크스페이스, 스트리밍 생성, Continuity 리포트, 수정·승인·재생성 | §6.3, §9.6-4 |

화면 위쪽의 파이프라인 표시는 백엔드가 알려 준 상태를 그대로 보여줍니다. 버튼 활성화도 그 상태를 따릅니다. 실제 상태 판단은 백엔드가 합니다.

## 디자인

- 다크 모드 기본, 툴바의 버튼으로 라이트 모드 전환 (선택값은 브라우저에 저장)
- 기획서 §9.3 팔레트: Primary `#1F4E79`, Secondary `#2E75B6`, Accent `#E8A838`
- 글꼴: Pretendard (CDN), 아이콘: Material Icons
- 토큰과 테마는 [src/styles/](src/styles/)에서 관리합니다

## 폴더 구조

```
storyforge-frontend/
├── proxy.conf.json        # 개발 서버 /api → 백엔드
├── angular.json           # 빌드 설정 (proxyConfig, budgets)
├── public/                # 그대로 복사되는 파일 (favicon)
└── src/
    ├── environments/      # apiBaseUrl
    ├── styles/            # 디자인 토큰, Material 테마
    └── app/
        ├── core/          # api(REST·SSE 래퍼), models(타입), auth(예정)
        ├── shared/        # 공통 컴포넌트 (pipeline-stepper)
        ├── features/      # dashboard, projects, story-bible, outline, chapters
        └── layout/        # 셸, 테마 서비스
```

## 문제 해결

| 증상 | 원인 / 해결 |
|------|-------------|
| 화면에 "서버에 연결할 수 없습니다" | 백엔드(8080)가 꺼져 있습니다. `storyforge-backend`에서 `./mvnw spring-boot:run` |
| 챕터 생성 시 "챕터 생성에 실패했습니다" | AI 서비스(8000)가 꺼져 있습니다. 실패해도 프로젝트 상태는 생성 전으로 돌아갑니다 |
| `ng` 실행 시 Node 버전 오류 | Node 24.14 이상인지 확인합니다 (`node -v`) |
| 4200 포트 사용 중 | `npx ng serve --port 4300`으로 바꾸고 백엔드 `FRONTEND_ORIGIN`도 맞춥니다 |

## 아직 없는 것

- 인증(JWT) 화면 — 백엔드 인증 도입과 함께 `core/auth/`에 추가
- TipTap 에디터 (기획서 §9.2) — 현재 챕터 본문은 textarea
- 내보내기, 프로젝트 설정 화면 (§9.6-5)

## 규칙

- LLM 호출, Continuity 판단, 상태 전이 로직을 이 프로젝트에 넣지 않습니다.
- FastAPI 주소, API 키 등 시크릿을 `environments/`에 넣지 않습니다 (번들에 노출됨).

## 관련 문서

- [기획서 §8.1, §9](../StoryForge_기획서_v1.2.md)
- [docs/plan/api-v1.md](../docs/plan/api-v1.md) — 호출하는 API와 SSE 이벤트
- [docs/guidance/local-dev-setup.md](../docs/guidance/local-dev-setup.md) — 전체 스택 실행
- 저장소 안내: [../README.md](../README.md), 작업 규칙: [../AGENTS.md](../AGENTS.md)
