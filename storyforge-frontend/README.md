# storyforge-frontend/

StoryForge의 **Angular 18+ SPA**입니다. 사용자 화면, 리치 텍스트 에디터, 파이프라인 진행 표시를 맡습니다.
모든 API 호출은 Spring Boot(`storyforge-backend`)로만 보냅니다.

## 들어갈 것

- Angular 18+ / Angular Material / RxJS / TipTap (기획서 §12)
- Standalone components 권장 (§12)
- 다크 모드 우선, Light/Dark 토글 (§9.1, §9.2)
- SSE로 챕터 생성 스트리밍 수신 (§7.3, §11)

## 들어가지 않을 것

- FastAPI 직접 호출, LLM 호출 로직 (정본 "호출 및 책임 규칙")
- 파이프라인 상태 전이 판단 (Spring Boot 담당)
- 포트·API 주소 하드코딩 (값은 `docs/plan/CURRENT_STACK.md`)

## 하위 폴더

| 폴더 | 내용 |
|------|------|
| [src/](src/) | 앱 소스 루트 |
| [public/](public/) | 그대로 복사되는 정적 파일 (favicon) |

## 현재 상태

Angular 21.2 + Angular Material로 기초 화면을 구현했습니다.

| 화면 | 경로 | 내용 |
|------|------|------|
| 대시보드 | `/` | 프로젝트 카드 그리드, 진행률, 새 프로젝트 |
| Story Bible | `/projects/:id/bible` | AI 생성, Overview/Characters/World/Themes 탭 편집, 확정 |
| Outline | `/projects/:id/outline` | AI 생성, 드래그 앤 드롭 재배치, Beats 편집, 확정 |
| 챕터 | `/projects/:id/chapters` | 3단 워크스페이스, 스트리밍 생성, Continuity 리포트, 승인·재생성 |

- 개발 서버: `npx ng serve` (http://localhost:4200, `/api`는 `proxy.conf.json`으로 백엔드에 전달)
- 아직 없음: 인증(JWT) 화면, TipTap 에디터(현재 textarea), 내보내기, 프로젝트 설정 화면

실행 방법은 [docs/guidance/local-dev-setup.md](../docs/guidance/local-dev-setup.md)를 보세요.

## 관련 문서

- [기획서 §8.1, §9](../StoryForge_기획서_v1.2.md)
- [docs/Architecture/system-overview.md](../docs/Architecture/system-overview.md)
- [docs/plan/CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md)
- 저장소 안내: [../README.md](../README.md), 작업 규칙: [../AGENTS.md](../AGENTS.md)
