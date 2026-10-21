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

## 현재 상태

폴더 구조와 README만 있습니다. Angular CLI 초기화(`package.json`, `angular.json`, `src/main.ts`, `app.routes.ts` 등)는 아직 하지 않았습니다.
초기화할 때 버전을 `docs/plan/CURRENT_STACK.md`에 먼저 확정합니다.

## 관련 문서

- [기획서 §8.1, §9](../StoryForge_기획서_v1.2.md)
- [docs/Architecture/system-overview.md](../docs/Architecture/system-overview.md)
- [docs/plan/CURRENT_STACK.md](../docs/plan/CURRENT_STACK.md)
- 저장소 안내: [../README.md](../README.md), 작업 규칙: [../AGENTS.md](../AGENTS.md)
